package org.folio.service.manager.inputdatamanager;

import static io.vertx.core.Future.succeededFuture;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.List;

import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.manager.exportmanager.ExportManager;
import org.folio.service.manager.inputdatamanager.reader.SourceReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;

/**
 * Acts a source of a uuids to be exported.
 */
@SuppressWarnings({"java:S1172", "java:S125"})
class InputDataManagerImpl implements InputDataManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int POOL_SIZE = 1;
  private ExportManager exportManager;
  //TODO
  private SourceReader sourceReader;
  private Iterator<List<String>> sourceStream;
  private WorkerExecutor executor;

  public InputDataManagerImpl(Context context) {
    exportManager = context.get(ExportManager.class.getName());
    this.executor = context.owner().createSharedWorkerExecutor("input-data-manager-thread-worker", POOL_SIZE);
  }

  @Override
  public void init(JsonObject request, JsonObject params) {
    this.executor.executeBlocking(blockingFuture -> {
      ExportRequest exportRequest = request.mapTo(ExportRequest.class);
      FileDefinition fileDefinition = exportRequest.getFileDefinition();
      LOGGER.info("Initializing data export to read from {}", exportRequest.getFileDefinition().getFileName());
      sourceStream = sourceReader.getSourceStream(fileDefinition, exportRequest.getBatchSize());
      proceed(request, params);
    }, this::handleExportResult);
  }

  @Override
  public void proceed(JsonObject request, JsonObject params) {
    ExportRequest exportRequest = request.mapTo(ExportRequest.class);
    if (sourceStream != null && sourceStream.hasNext()) {
      LOGGER.info("Reading next chunk of uuids from {}", exportRequest.getFileDefinition().getFileName());
      List<String> identifiers = sourceStream.next();
      JsonObject payload = new JsonObject().put("identifiers", identifiers);
      //TODO
      //exportManager.export(payload, params);
    } else {
      LOGGER.info("All uuids from {} have been taken into processing", exportRequest.getFileDefinition().getFileName());
    }
  }

  /**
   * @param asyncResult
   * @return
   */
  private Future<Void> handleExportResult(AsyncResult asyncResult) {
    if (asyncResult.failed()) {
      LOGGER.error("Export is failed, cause: " + asyncResult.cause());
    } else {
      LOGGER.info("Export has been successfully completed");
    }
    return succeededFuture();
  }
}
