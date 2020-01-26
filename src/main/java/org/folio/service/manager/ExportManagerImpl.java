package org.folio.service.manager;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.service.fileexport.FileExportService;
import org.folio.service.fileupload.FileUploadService;
import org.folio.service.fileupload.storage.FileStorageService;
import org.folio.service.loader.RecordLoaderService;
import org.folio.util.OkapiConnectionParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static io.vertx.core.Future.succeededFuture;

class ExportManagerImpl implements ExportManager {
  private final Logger LOGGER = LoggerFactory.getLogger(ExportManagerImpl.class);
  private final int BATCH_SIZE = 100;

  private Vertx vertx;
  /* WorkerExecutor provides a worker pool for export process */
  private WorkerExecutor executor;

  public ExportManagerImpl(Vertx vertx) {
    this.vertx = vertx;
    this.executor = this.vertx.createSharedWorkerExecutor("worker-thread");
  }

  @Override
  public void startExport(JsonObject jsonRequest, JsonObject jsonParams) {
    OkapiConnectionParams params = new OkapiConnectionParams(jsonParams.mapTo(HashMap.class));
    ExportRequest request = jsonRequest.mapTo(ExportRequest.class);
    this.executor.executeBlocking(blockingFuture -> {
      succeededFuture()
        .compose(ar -> isNoJobInProgress())
        .compose(ar -> updateJobStatus("IN_PROGRESS"))
        .compose(ar -> export(request, params))
        .setHandler(ar -> {
          if (ar.failed()) {
            LOGGER.error("An error occurred while exporting data, file {}, cause: {}", request.getFileDefinition(), ar.cause());
            updateJobStatus("ERROR");
          } else {
            LOGGER.info("All the data has been successfully exported, file: {}", request.getFileDefinition());
            updateJobStatus("COMPLETED");
          }
        });
    }, null);
  }

  private Future<Void> export(ExportRequest request, OkapiConnectionParams params) {
    FileStorageService fileStorageService = new FileUploadService(){}.getFileStorage();
    RecordLoaderService recordLoaderService = new RecordLoaderService() {};
    FileExportService fileExportService = new FileExportService() {};
    List<Future> exportFutures = new ArrayList<>();
    String exportingFileId = UUID.randomUUID().toString();
    Iterator<List<String>> fileIterator = fileStorageService.getSourceReader()
      .getSourceStream(request.getFileDefinition(), BATCH_SIZE)
      .iterator();
    while (fileIterator.hasNext()) {
    //    lock the "thread-worker" to stop reading and wait until previous UUIDs export
    //    List<String> instanceIds = fileIterator.next();
    //    List<String> marcRecords = recordLoaderService.loadRecordsByInstanceIds(instanceIds);
    //    fileExportService.save(exportingFileId, marcRecords);
    //    unlock the "thread-worker" to continue reading more UUIDs
    }
    return CompositeFuture.all(exportFutures).mapEmpty();
  }

  private Future<Boolean> isNoJobInProgress() {
    return succeededFuture();
  }

  private Future<Void> updateJobStatus(String status) {
    return succeededFuture();
  }
}
