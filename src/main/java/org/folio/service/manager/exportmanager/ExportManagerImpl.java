package org.folio.service.manager.exportmanager;

import com.google.common.collect.Lists;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.service.export.FileExportService;
import org.folio.service.loader.MarcLoadResult;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.mapping.MappingService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static io.vertx.core.Future.succeededFuture;

/**
 * The ExportManager is a central part of the data-export.
 * Runs the main export process calling other internal services along the way.
 */
@SuppressWarnings({"java:S1172", "java:S125"})
@Service
public class ExportManagerImpl implements ExportManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExportManagerImpl.class);
  private static final String IDENTIFIERS_REQUEST_KEY = "identifiers";
  private static final int POOL_SIZE = 1;
  private static final int SRS_LOAD_PARTITION_SIZE = 15;
  private static final int INVENTORY_LOAD_PARTITION_SIZE = 15;
  /* WorkerExecutor provides a worker pool for export process */
  private WorkerExecutor executor;

  @Autowired
  private RecordLoaderService recordLoaderService;
  @Autowired
  private FileExportService fileExportService;
  @Autowired
  private MappingService mappingService;

  public ExportManagerImpl() {
  }

  public ExportManagerImpl(Context context) {
    SpringContextUtil.autowireDependencies(this, context);
    this.executor = context.owner().createSharedWorkerExecutor("export-thread-worker", POOL_SIZE);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void exportData(JsonObject jsonRequest, JsonObject jsonRequestParams) {
    if (jsonRequest.containsKey(IDENTIFIERS_REQUEST_KEY)) {
      List<String> identifiers = jsonRequest.getJsonArray(IDENTIFIERS_REQUEST_KEY).getList();
      LOGGER.info("Starting export records for the given identifiers, size: " + identifiers.size());
      this.executor.executeBlocking(ar -> exportBlocking(identifiers), this::handleExportResult);
    } else {
      String errorMessage = "Can not find identifiers, request: " + jsonRequest;
      LOGGER.error(errorMessage);
      throw new IllegalArgumentException(errorMessage);
    }
  }

  /**
   * Runs the main export flow in blocking manner.
   *
   * @param identifiers instance identifiers
   */
  protected void exportBlocking(List<String> identifiers) {
    MarcLoadResult marcLoadResult = loadSrsMarcRecordsInPartitions(identifiers);
    fileExportService.export(marcLoadResult.getSrsMarcRecords());
    List<JsonObject> instances = loadInventoryInstancesInPartitions(identifiers);
    List<String> mappedMarcRecords = mappingService.map(instances);
    fileExportService.export(mappedMarcRecords);
  }

  /**
   * Loads marc records from SRS by the given instance identifiers
   *
   * @param identifiers instance identifiers
   * @return @see MarcLoadResult
   */
  private MarcLoadResult loadSrsMarcRecordsInPartitions(List<String> identifiers) {
    MarcLoadResult marcLoadResult = new MarcLoadResult();
    Lists.partition(identifiers, SRS_LOAD_PARTITION_SIZE).forEach(partition -> {
      MarcLoadResult partitionLoadResult = recordLoaderService.loadSrsMarcRecords(partition);
      marcLoadResult.getSrsMarcRecords().addAll(partitionLoadResult.getSrsMarcRecords());
      marcLoadResult.getInstanceIds().addAll(partitionLoadResult.getInstanceIds());
    });
    return marcLoadResult;
  }

  /**
   * Loads instances from Inventory by the given identifiers
   *
   * @param identifiers instance identifiers
   * @return list of instances
   */
  private List<JsonObject> loadInventoryInstancesInPartitions(List<String> identifiers) {
    List<JsonObject> instances = new ArrayList<>();
    Lists.partition(identifiers, INVENTORY_LOAD_PARTITION_SIZE).forEach(partition -> {
        List<JsonObject> partitionLoadResult = recordLoaderService.loadInventoryInstances(partition);
        instances.addAll(partitionLoadResult);
      }
    );
    return instances;
  }

  /**
   * Handles asynch result of export
   *
   * @param asyncResult result of export
   * @return return future
   */
  private Future<Void> handleExportResult(AsyncResult asyncResult) {
    if (asyncResult.failed()) {
      LOGGER.error("Export is failed, cause: " + asyncResult.cause());
    } else {
      LOGGER.info("Export has been successfully passed");
      // call importManager to request more ids
    }
    return succeededFuture();
  }
}
