package org.folio.service.inputdatamanager;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.util.OkapiConnectionParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static io.vertx.core.Future.succeededFuture;

/**
 * The ExportManager is a central part of the data-export.
 * Runs the main export process calling other internal services along the way.
 */
@SuppressWarnings({"java:S1172", "java:S125"})
class InputDataManagerImpl implements InputDataManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(InputDataManagerImpl.class);
  private static final int BATCH_SIZE = 50; //NOSONAR
  private final Vertx vertx;
  /* WorkerExecutor provides a worker pool for export process */
  private WorkerExecutor executor;

  public InputDataManagerImpl(Vertx vertx) {
    this.vertx = vertx;
    this.executor = this.vertx.createSharedWorkerExecutor("export-thread-worker");
  }

/*  @Override
  public void startExport(JsonObject jsonRequest, JsonObject jsonRequestParams) {
    ExportRequest request = jsonRequest.mapTo(ExportRequest.class);
    OkapiConnectionParams requestParams = new OkapiConnectionParams(jsonRequestParams.mapTo(HashMap.class));
    this.executor.executeBlocking(blockingFuture -> succeededFuture()
      .compose(ar -> isNoJobInProgress())
      .compose(ar -> updateJobStatus("IN_PROGRESS"))
      .compose(ar -> export(request, requestParams))
      .setHandler(ar -> {
        if (ar.failed()) {
          LOGGER.error("An error occurred while exporting data, file {}, cause: {}", request.getFileDefinition(), ar.cause());
          updateJobStatus("ERROR");
        } else {
          LOGGER.info("All the data has been successfully exported, file: {}", request.getFileDefinition());
          updateJobStatus("COMPLETED");
        }
      }), null);
  }*/

  private Future<Boolean> isNoJobInProgress() {
    return succeededFuture();
  }

  private Future<Void> updateJobStatus(String status) {
    return succeededFuture();
  }

  @Override
  public void start(JsonObject request, JsonObject params) {

  }

  @Override
  public void proceed(JsonObject request, JsonObject params) {

  }
}
