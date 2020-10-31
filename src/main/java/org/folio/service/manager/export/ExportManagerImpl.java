package org.folio.service.manager.export;

import com.google.common.collect.Lists;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.service.export.ExportService;
import org.folio.service.file.storage.FileStorage;
import org.folio.service.job.JobExecutionService;
import org.folio.service.loader.InventoryLoadResult;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.manager.input.InputDataManager;
import org.folio.service.mapping.converter.InventoryRecordConverterService;
import org.folio.service.mapping.converter.SrsRecordConverterService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;

/**
 * The ExportManager is a central part of the data-export.
 * Runs the main export process calling other internal services along the way.
 */
@Service
public class ExportManagerImpl implements ExportManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int POOL_SIZE = 2;
  private static final int SRS_LOAD_PARTITION_SIZE = 50;
  private static final int INVENTORY_LOAD_PARTITION_SIZE = 50;
  /* WorkerExecutor provides a worker pool for export process */
  private WorkerExecutor executor;

  @Autowired
  private RecordLoaderService recordLoaderService;
  @Autowired
  private ExportService exportService;
  @Autowired
  private JobExecutionService jobExecutionService;
  @Autowired
  private SrsRecordConverterService srsRecordService;
  @Autowired
  private InventoryRecordConverterService inventoryRecordService;
  @Autowired
  private Vertx vertx;
  @Autowired
  private FileStorage fileStorage;
  @Autowired
  ErrorLogService errorLogService;

  public ExportManagerImpl() {
  }

  public ExportManagerImpl(Context context) {
    SpringContextUtil.autowireDependencies(this, context);
    this.executor = context.owner().createSharedWorkerExecutor("export-thread-worker", POOL_SIZE);

  }

  @Override
  public void exportData(JsonObject request) {
    ExportPayload exportPayload = request.mapTo(ExportPayload.class);
    this.executor.executeBlocking(blockingFuture -> {
      exportBlocking(exportPayload);
      blockingFuture.complete();
    }, ar -> handleExportResult(ar, exportPayload));
  }

  /**
   * Runs the main export flow in blocking manner.
   *
   * @param exportPayload payload of the export request
   */
  protected void exportBlocking(ExportPayload exportPayload) {
    List<String> identifiers = exportPayload.getIdentifiers();
    FileDefinition fileExportDefinition = exportPayload.getFileExportDefinition();
    MappingProfile mappingProfile = exportPayload.getMappingProfile();
    OkapiConnectionParams params = exportPayload.getOkapiConnectionParams();
    SrsLoadResult srsLoadResult = new SrsLoadResult();
    /**
     * For Q3-2020, if a custom mapping profile is used, never fetch the underlying SRS records, always generate marc records on the
     * fly
     */
    if (isTransformationEmpty(mappingProfile)) {
      srsLoadResult = loadSrsMarcRecordsInPartitions(identifiers, exportPayload.getJobExecutionId(), params);
      LOGGER.info("Records that are not present in SRS: {}", srsLoadResult.getInstanceIdsWithoutSrs());

      List<String> marcToExport = srsRecordService.transformSrsRecords(mappingProfile, srsLoadResult.getUnderlyingMarcRecords(),
        exportPayload.getJobExecutionId(), params);
      exportService.exportSrsRecord(marcToExport, fileExportDefinition);
      LOGGER.info("Number of instances not found in SRS: {}", srsLoadResult.getInstanceIdsWithoutSrs().size());
    } else {
      srsLoadResult.setInstanceIdsWithoutSrs(identifiers);
    }

    InventoryLoadResult instances = loadInventoryInstancesInPartitions(srsLoadResult.getInstanceIdsWithoutSrs(), exportPayload.getJobExecutionId(), params);
    LOGGER.info("Number of instances, that returned from inventory storage: {}", instances.getInstances().size());
    int numberOfNotFoundRecords = instances.getNotFoundInstancesUUIDs().size();
    LOGGER.info("Number of instances not found in Inventory Storage: {}", numberOfNotFoundRecords);
    if (numberOfNotFoundRecords > 0) {
      errorLogService.populateUUIDsNotFoundErrorLog(exportPayload.getJobExecutionId(), instances.getNotFoundInstancesUUIDs(), params.getTenantId());
    }
    List<String> mappedMarcRecords = inventoryRecordService.transformInventoryRecords(instances.getInstances(), exportPayload.getJobExecutionId(), mappingProfile, params);
    exportService.exportInventoryRecords(mappedMarcRecords, fileExportDefinition, params.getTenantId());
    exportPayload.setExportedRecordsNumber(srsLoadResult.getUnderlyingMarcRecords().size() + mappedMarcRecords.size());
    exportPayload.setFailedRecordsNumber(identifiers.size() - exportPayload.getExportedRecordsNumber());
    if (exportPayload.isLast()) {
      exportService.postExport(fileExportDefinition, params.getTenantId());
    }
  }

  private boolean isTransformationEmpty(MappingProfile mappingProfile) {
    return mappingProfile.getTransformations().isEmpty();
  }

  /**
   * Loads marc records from SRS by the given instance identifiers
   *
   * @param identifiers instance identifiers
   * @param params      okapi connection parameters
   * @return @see SrsLoadResult
   */
  private SrsLoadResult loadSrsMarcRecordsInPartitions(List<String> identifiers, String jobExecutionId, OkapiConnectionParams params) {
    SrsLoadResult srsLoadResult = new SrsLoadResult();
    Lists.partition(identifiers, SRS_LOAD_PARTITION_SIZE).forEach(partition -> {
      SrsLoadResult partitionLoadResult = recordLoaderService.loadMarcRecordsBlocking(partition, jobExecutionId, params);
      srsLoadResult.getUnderlyingMarcRecords().addAll(partitionLoadResult.getUnderlyingMarcRecords());
      srsLoadResult.getInstanceIdsWithoutSrs().addAll(partitionLoadResult.getInstanceIdsWithoutSrs());
    });
    return srsLoadResult;
  }

  /**
   * Loads instances from Inventory by the given identifiers
   *
   * @param singleInstanceIdentifiers identifiers of instances that do not have underlying srs
   * @param params                    okapi connection parameters
   * @return list of instances
   */
  private InventoryLoadResult loadInventoryInstancesInPartitions(List<String> singleInstanceIdentifiers, String jobExecutionId, OkapiConnectionParams params) {
    InventoryLoadResult inventoryLoadResult = new InventoryLoadResult();
    Lists.partition(singleInstanceIdentifiers, INVENTORY_LOAD_PARTITION_SIZE).forEach(partition -> {
        InventoryLoadResult partitionLoadResult = recordLoaderService.loadInventoryInstancesBlocking(partition, jobExecutionId, params, INVENTORY_LOAD_PARTITION_SIZE);
        inventoryLoadResult.getInstances().addAll(partitionLoadResult.getInstances());
        inventoryLoadResult.getNotFoundInstancesUUIDs().addAll(partitionLoadResult.getNotFoundInstancesUUIDs());
      }
    );
    return inventoryLoadResult;
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
        errorLogService.saveGeneralError(serviceException.getMessage(), exportPayload.getJobExecutionId(), exportPayload.getOkapiConnectionParams().getTenantId());
        return ExportResult.failed(serviceException.getErrorCode());
      }
      errorLogService.saveGeneralError(ErrorCode.GENERIC_ERROR_CODE.getDescription(), exportPayload.getJobExecutionId(), exportPayload.getOkapiConnectionParams().getTenantId());
      return ExportResult.failed(ErrorCode.GENERIC_ERROR_CODE);
    } else {
      LOGGER.info("Export has been successfully passed");
      if (exportPayload.isLast()) {
        return getExportResultForLastBatch(exportPayload);
      }
      return getInProgressExportResult(exportPayload);
    }
  }

  private ExportResult getExportResultForLastBatch(ExportPayload exportPayload) {
    if (exportPayload.getExportedRecordsNumber() == 0) {
      if (fileStorage.isFileExist(exportPayload.getFileExportDefinition().getSourcePath())) {
        errorLogService.populateUUIDsNotFoundNumberErrorLog(exportPayload.getJobExecutionId(), exportPayload.getFailedRecordsNumber(), exportPayload.getOkapiConnectionParams().getTenantId());
        return ExportResult.completedWithErrors();
      }
      return ExportResult.failed(ErrorCode.NOTHING_TO_EXPORT);
    } else if (exportPayload.getFailedRecordsNumber() > 0) {
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
    return jobExecutionService.incrementCurrentProgress(exportPayload.getJobExecutionId(), exported, failed, tenantId);
  }

  private InputDataManager getInputDataManager() {
    return vertx.getOrCreateContext().get(InputDataManager.class.getName());
  }
}
