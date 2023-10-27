package org.folio.service.manager.export;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.service.file.storage.FileStorage;
import org.folio.service.job.JobExecutionService;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.manager.export.strategy.ExportStrategy;
import org.folio.service.manager.input.InputDataManager;
import org.folio.service.mapping.converter.InventoryRecordConverterService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;

/**
 * The ExportManager is a central part of the data-export.
 * Runs the main export process calling other internal services along the way.
 */
@Service
public class ExportManagerImpl implements ExportManager {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
  private static final int POOL_SIZE = 2;
  public static final int SRS_LOAD_PARTITION_SIZE = 50;
  public static final int INVENTORY_LOAD_PARTITION_SIZE = 50;
  /* WorkerExecutor provides a worker pool for export process */
  private WorkerExecutor executor;
  private JobExecutionService jobExecutionService;
  private InventoryRecordConverterService inventoryRecordService;
  private Vertx vertx;
  private FileStorage fileStorage;
  private ErrorLogService errorLogService;
  private ExportStrategy instanceExportManager;
  private ExportStrategy holdingExportManager;
  private ExportStrategy authorityExportManager;

  @Autowired
  public ExportManagerImpl(JobExecutionService jobExecutionService, InventoryRecordConverterService inventoryRecordService,
      Vertx vertx, FileStorage fileStorage, ErrorLogService errorLogService,
      @Qualifier("instanceExportStrategyImpl") ExportStrategy instanceExportManager,
      @Qualifier("holdingExportStrategyImpl") ExportStrategy holdingExportManager,
      @Qualifier("authorityExportStrategyImpl") ExportStrategy authorityExportManager) {
    this.jobExecutionService = jobExecutionService;
    this.inventoryRecordService = inventoryRecordService;
    this.vertx = vertx;
    this.fileStorage = fileStorage;
    this.errorLogService = errorLogService;
    this.instanceExportManager = instanceExportManager;
    this.holdingExportManager = holdingExportManager;
    this.authorityExportManager = authorityExportManager;
  }

  public ExportManagerImpl(Context context) {
    SpringContextUtil.autowireDependencies(this, context);
    this.executor = context.owner().createSharedWorkerExecutor("export-thread-worker", POOL_SIZE);

  }

  @Override
  public void exportData(JsonObject request) {
    ExportPayload exportPayload = request.mapTo(ExportPayload.class);
    switch (exportPayload.getIdType()) {
      case INSTANCE:
        this.executor.executeBlocking(blockingPromise -> instanceExportManager.export(exportPayload, blockingPromise), ar -> handleExportResult(ar, exportPayload));
        break;
      case HOLDING:
        this.executor.executeBlocking(blockingPromise -> holdingExportManager.export(exportPayload, blockingPromise), ar -> handleExportResult(ar, exportPayload));
        break;
      case AUTHORITY:
        this.executor.executeBlocking(blockingPromise -> authorityExportManager.export(exportPayload, blockingPromise), ar -> handleExportResult(ar, exportPayload));
        break;
    }
  }


  /**
   * Handles async result of export, this code gets processing in the main event loop
   *
   * @param asyncResult   result of export
   * @param exportPayload payload of the export request
   * @return return future
   */
  private Future<Void> handleExportResult(AsyncResult<Object> asyncResult, ExportPayload exportPayload) {
    Promise<Void> promise = Promise.promise();
    JsonObject exportPayloadJson = JsonObject.mapFrom(exportPayload);
    ExportResult exportResult = getExportResult(asyncResult, exportPayload);
    clearIdentifiers(exportPayload);
    incrementCurrentProgress(exportPayload)
      .onComplete(handler -> {
        getInputDataManager().proceed(exportPayloadJson, exportResult);
        promise.complete();
      });
    return promise.future();
  }

  private void clearIdentifiers(ExportPayload exportPayload) {
    exportPayload.setIdentifiers(Collections.emptyList());
  }

  private ExportResult getExportResult(AsyncResult<Object> asyncResult, ExportPayload exportPayload) {
    if (asyncResult.failed()) {
      LOGGER.error("Export is failed, cause: {}", asyncResult.cause().getMessage());
      if (asyncResult.cause() instanceof ServiceException) {
        ServiceException serviceException = (ServiceException) asyncResult.cause();
        errorLogService.saveGeneralError(serviceException.getErrorCode().getCode(), exportPayload.getJobExecutionId(), exportPayload.getOkapiConnectionParams().getTenantId());
        return ExportResult.failed(serviceException.getErrorCode());
      }
      errorLogService.saveGeneralError(ErrorCode.GENERIC_ERROR_CODE.getCode(), exportPayload.getJobExecutionId(), exportPayload.getOkapiConnectionParams().getTenantId());
      return ExportResult.failed(ErrorCode.GENERIC_ERROR_CODE);
    } else {
      LOGGER.info("Export batch has successfully completed");
      if (exportPayload.isLast()) {
        LOGGER.debug("Processing the last export batch");
        return getExportResultForLastBatch(exportPayload);
      }
      return getInProgressExportResult(exportPayload);
    }
  }

  private ExportResult getExportResultForLastBatch(ExportPayload exportPayload) {
    if (exportPayload.getExportedRecordsNumber() == 0) {
      if (fileStorage.isFileExist(exportPayload.getFileExportDefinition().getSourcePath())) {
        LOGGER.debug("Errors found in the entire last batch,status completing with errors");
        errorLogService.populateUUIDsNotFoundNumberErrorLog(exportPayload.getJobExecutionId(), exportPayload.getFailedRecordsNumber(), exportPayload.getOkapiConnectionParams().getTenantId());
        return ExportResult.completedWithErrors();
      }
      return ExportResult.failed(ErrorCode.NOTHING_TO_EXPORT);
    } else if (exportPayload.getFailedRecordsNumber() > 0) {
      LOGGER.debug("Errors found in few records of last batch,status completing with errors");
      errorLogService.populateUUIDsNotFoundNumberErrorLog(exportPayload.getJobExecutionId(), exportPayload.getFailedRecordsNumber(), exportPayload.getOkapiConnectionParams().getTenantId());
      return ExportResult.completedWithErrors();
    } else {
      return ExportResult.completed();
    }
  }

  private ExportResult getInProgressExportResult(ExportPayload exportPayload) {
    if (exportPayload.getFailedRecordsNumber() > 0) {
      errorLogService.populateUUIDsNotFoundNumberErrorLog(exportPayload.getJobExecutionId(), exportPayload.getFailedRecordsNumber(), exportPayload.getOkapiConnectionParams().getTenantId());
    }
    return ExportResult.inProgress();
  }

  private Future<JobExecution> incrementCurrentProgress(ExportPayload exportPayload) {
    String tenantId = exportPayload.getOkapiConnectionParams().getTenantId();
    int exported = exportPayload.getExportedRecordsNumber();
    int failed = exportPayload.getFailedRecordsNumber();
    int duplicatedSrs = exportPayload.getDuplicatedSrs();
    return jobExecutionService.incrementCurrentProgress(exportPayload.getJobExecutionId(), exported, failed, duplicatedSrs, exportPayload.getInvalidUUIDs(), tenantId);
  }

  private InputDataManager getInputDataManager() {
    return vertx.getOrCreateContext().get(InputDataManager.class.getName());
  }
}
