package org.folio.clients;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.service.logs.ErrorLogService;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.folio.util.ExternalPathResolver.AUTHORITY;
import static org.folio.util.ExternalPathResolver.INSTANCE;
import static org.folio.util.ExternalPathResolver.resourcesPathWithPrefix;

public abstract class BaseConcurrentClient {

  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
  private static final String QUERY_PATTERN_WITH_SOURCE = "id==%s and source==";
  public static final int CHUNK_SIZE = 15;
  public static final int THREADS_POOL_SIZE = 15;
  public static final String QUERY_LIMIT_PATTERN = "?query=(%s)&limit=";

  @Autowired
  private ErrorLogService errorLogService;

  public abstract String getEntitiesCollectionName();

  public Optional<JsonObject> getByIds(List<String> ids, String jobExecutionId, OkapiConnectionParams params) {
    return getByIds(ids, jobExecutionId, params, null);
  }

  public Optional<JsonObject> getByIds(List<String> ids, String jobExecutionId, OkapiConnectionParams params, String source) {
    var partitions = ListUtils.partition(ids, CHUNK_SIZE);
    var semaphore = new Semaphore(THREADS_POOL_SIZE);
    var lock = new ReentrantLock();
    var executor = Executors.newFixedThreadPool(THREADS_POOL_SIZE);
    var result = new JsonObject().put(getEntitiesCollectionName(), new JsonArray());

    try {

      for (List<String> partition : partitions) {

        executor.execute(() -> {
          try {
            semaphore.acquire();
            try {
              var entities = StringUtils.isNotEmpty(source) ?
                ClientUtil.getByIds(ids, params, resourcesPathWithPrefix(AUTHORITY) + QUERY_LIMIT_PATTERN + ids.size(),
                  "(" + QUERY_PATTERN_WITH_SOURCE + source + ")").getJsonArray(getEntitiesCollectionName())
                : ClientUtil.getByIds(partition, params, resourcesPathWithPrefix(INSTANCE) + QUERY_LIMIT_PATTERN + ids.size()).getJsonArray(getEntitiesCollectionName());
              lock.lock();
              result.getJsonArray(getEntitiesCollectionName()).addAll(entities);
            } catch (HttpClientException exception) {
              LOGGER.error(exception.getMessage(), exception.getCause());
              errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_GETTING_INSTANCES_BY_IDS.getCode(), Arrays.asList(exception.getMessage()), jobExecutionId, params.getTenantId());
            } finally {
              lock.unlock();
            }
            semaphore.release();
          } catch (InterruptedException exception) {
            LOGGER.error(exception.getMessage(), exception.getCause());
            errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_GETTING_INSTANCES_BY_IDS.getCode(), Arrays.asList(exception.getMessage()), jobExecutionId, params.getTenantId());
            Thread.currentThread().interrupt();
          }
        });
      }
      executor.shutdown();
      executor.awaitTermination(60, TimeUnit.HOURS);
    } catch (InterruptedException e) {
      LOGGER.error(e.getMessage(), e.getCause());
      errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_GETTING_INSTANCES_BY_IDS.getCode(), Arrays.asList(e.getMessage()), jobExecutionId, params.getTenantId());
      Thread.currentThread().interrupt();
    }

    result.put("totalRecords", result.getJsonArray(getEntitiesCollectionName()).size());
    return Optional.of(result);
  }
}
