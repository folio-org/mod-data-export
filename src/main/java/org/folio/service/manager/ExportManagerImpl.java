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

import static io.vertx.core.Future.succeededFuture;

/**
 * The ExportManager component is a central part of the data-export.
 * Runs the main export process calling other internal services along the way.
 */
class ExportManagerImpl implements ExportManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExportManagerImpl.class);
  private static final int BATCH_SIZE = 50;
  private final Vertx vertx;
  /* WorkerExecutor provides a worker pool for export process */
  private WorkerExecutor executor;

  public ExportManagerImpl(Vertx vertx) {
    this.vertx = vertx;
    this.executor = this.vertx.createSharedWorkerExecutor("export-thread-worker");
  }

  @Override
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
  }

  /**
   *  Runs the main export flow.
   *
   * @param request   HTTP request
   * @param params    HTTP request params
   * @return Future. Futures get complete when all the records are exported
   */
  private Future<Void> export(ExportRequest request, OkapiConnectionParams params) {
    List<Future> exportFutures = new ArrayList<>();
    FileStorageService fileStorageService = new FileUploadService() {}.getFileStorageService();
    RecordLoaderService recordLoaderService = new RecordLoaderService() {};
    FileExportService fileExportService = new FileExportService() {};
//  String exportingFileId = UUID.randomUUID().toString();
    Iterator<List<String>> fileIterator = fileStorageService.getReader()
      .getSourceStream(request.getFileDefinition(), BATCH_SIZE)
      .iterator();
//  while (fileIterator.hasNext()) {
  //  lock the "export-thread-worker" to wait until previous file data has been exported
  //  List<String> instanceIds = fileIterator.next();
  //  List<String> marcRecords = recordLoaderService.loadRecordsByInstanceIds(instanceIds);
  //  fileExportService.save(exportingFileId, marcRecords);
  //  unlock the "export-thread-worker" to continue reading file data
// }
    return CompositeFuture.all(exportFutures).mapEmpty();
  }

  private Future<Boolean> isNoJobInProgress() {
    return succeededFuture();
  }

  private Future<Void> updateJobStatus(String status) {
    return succeededFuture();
  }
}
