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
import org.folio.service.job.JobExecutionService;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.manager.input.InputDataManager;
import org.folio.service.mapping.convertor.InventoryRecordConvertorService;
import org.folio.service.mapping.convertor.SrsRecordConvertorService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
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
  private static final int SRS_LOAD_PARTITION_SIZE = 5;
  private static final int INVENTORY_LOAD_PARTITION_SIZE = 5;
  /* WorkerExecutor provides a worker pool for export process */
  private WorkerExecutor executor;

  @Autowired
  private RecordLoaderService recordLoaderService;
  @Autowired
  private ExportService exportService;
  @Autowired
  private JobExecutionService jobExecutionService;
  @Autowired
  private SrsRecordConvertorService srsRecordService;
  @Autowired
  private InventoryRecordConvertorService inventoryRecordService;
  @Autowired
  private Vertx vertx;

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

    //Export srs results at once get the first partition of the records
    Lists.partition(identifiers, SRS_LOAD_PARTITION_SIZE).stream().forEach(partition -> {
      SrsLoadResult srsLoadResult = recordLoaderService.loadMarcRecordsBlocking(partition, params);
      LOGGER.info("Records that are not present in SRS: {}", srsLoadResult.getInstanceIdsWithoutSrs());
      List<String> marcToExport = srsRecordService.transformSrsRecords(mappingProfile, srsLoadResult.getUnderlyingMarcRecords(),
        exportPayload.getJobExecutionId(), params);
      exportService.exportSrsRecord(marcToExport, fileExportDefinition);

      //Move increment of the progress at once srs results exported for one
      int failedSrsRecords = partition.size() - srsLoadResult.getUnderlyingMarcRecords().size();
      jobExecutionService.incrementCurrentProgress(exportPayload.getJobExecutionId(), srsLoadResult.getUnderlyingMarcRecords().size(), failedSrsRecords, params.getTenantId());

      //Map and export instances at once get the first partition of the records
      Lists.partition(srsLoadResult.getInstanceIdsWithoutSrs(), INVENTORY_LOAD_PARTITION_SIZE).forEach(inventoryPartition -> {
          List<JsonObject> instances = recordLoaderService.loadInventoryInstancesBlocking(inventoryPartition, params, INVENTORY_LOAD_PARTITION_SIZE);
          LOGGER.info("Number of instances, that returned from inventory storage: {}", instances.size());
          LOGGER.info("Number of instances not found either in SRS or Inventory Storage: {}", srsLoadResult.getInstanceIdsWithoutSrs().size() - instances.size());

          List<String> mappedMarcRecords = inventoryRecordService.transformInventoryRecords(instances, exportPayload.getJobExecutionId(), mappingProfile, params);
          exportService.exportInventoryRecords(mappedMarcRecords, fileExportDefinition);

          //Move increment of the progress at once mapped records exported for one partition
          int failedMappedRecords = inventoryPartition.size() -  mappedMarcRecords.size();
          jobExecutionService.incrementCurrentProgress(exportPayload.getJobExecutionId(), mappedMarcRecords.size(), failedMappedRecords, params.getTenantId());
        }
      );
    });
    if (exportPayload.isLast()) {
      exportService.postExport(fileExportDefinition, params.getTenantId());
    }
  }


  /**
   * Loads marc records from SRS by the given instance identifiers
   *
   * @param identifiers instance identifiers
   * @param params      okapi connection parameters
   * @return @see SrsLoadResult
   */
  private SrsLoadResult loadSrsMarcRecordsInPartitions(List<String> identifiers, OkapiConnectionParams params) {
    SrsLoadResult srsLoadResult = new SrsLoadResult();
    Lists.partition(identifiers, SRS_LOAD_PARTITION_SIZE).forEach(partition -> {
      SrsLoadResult partitionLoadResult = recordLoaderService.loadMarcRecordsBlocking(partition, params);
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
  private List<JsonObject> loadInventoryInstancesInPartitions(List<String> singleInstanceIdentifiers, OkapiConnectionParams params) {
    List<JsonObject> instances = new ArrayList<>();
    Lists.partition(singleInstanceIdentifiers, INVENTORY_LOAD_PARTITION_SIZE).forEach(partition -> {
        List<JsonObject> partitionLoadResult = recordLoaderService.loadInventoryInstancesBlocking(partition, params, INVENTORY_LOAD_PARTITION_SIZE);
        instances.addAll(partitionLoadResult);
      }
    );
    return instances;
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
    ExportResult exportResult = getExportResult(asyncResult, exportPayload.isLast());
    clearIdentifiers(exportPayload);
    getInputDataManager().proceed(exportPayloadJson, exportResult);
    promise.complete();
    return promise.future();
  }

  private void clearIdentifiers(ExportPayload exportPayload) {
    exportPayload.setIdentifiers(Collections.emptyList());
  }

  private ExportResult getExportResult(AsyncResult<Object> asyncResult, boolean isLast) {
    if (asyncResult.failed()) {
      LOGGER.error("Export is failed, cause: {}", asyncResult.cause().getMessage());
      if (asyncResult.cause() instanceof ServiceException) {
        ServiceException serviceException = (ServiceException) asyncResult.cause();
        return ExportResult.failed(serviceException.getErrorCode());
      }
      return ExportResult.failed(ErrorCode.GENERIC_ERROR_CODE);
    } else {
      LOGGER.info("Export has been successfully passed");
      if (isLast) {
        return ExportResult.completed();
      }
      return ExportResult.inProgress();
    }
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
