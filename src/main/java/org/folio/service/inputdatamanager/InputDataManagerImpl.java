package org.folio.service.inputdatamanager;

import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import org.folio.service.inputdatamanager.reader.SourceStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.stream.Stream;

import static io.vertx.core.Future.succeededFuture;

/**
 * Acts a source of a uuids to be exported.
 *
 */
@SuppressWarnings({"java:S1172", "java:S125"})
@Component
class InputDataManagerImpl implements InputDataManager {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int BATCH_SIZE = 50; //NOSONAR
  private final Vertx vertx;

  @Autowired
  private SourceStreamReader sourceStreamReader;

  private Stream<String> sourceStream;

  public InputDataManagerImpl(Vertx vertx) {
    this.vertx = vertx;
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

  @Override
  public void start(JsonObject request, JsonObject params) throws IOException {
    log.info("Export started");
    sourceStream = sourceStreamReader.getSourceStream(null, BATCH_SIZE);
  }

  @Override
  public void proceed(JsonObject request, JsonObject params) {

  }
}
