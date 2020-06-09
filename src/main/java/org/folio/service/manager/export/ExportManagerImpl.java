package org.folio.service.manager.export;

import com.google.common.collect.Lists;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.export.ExportService;
import org.folio.service.job.JobExecutionService;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.manager.input.InputDataManager;
import org.folio.service.mapping.MappingService;
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
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * The ExportManager is a central part of the data-export.
 * Runs the main export process calling other internal services along the way.
 */
@Service
public class ExportManagerImpl implements ExportManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int POOL_SIZE = 1;
  private static final int SRS_LOAD_PARTITION_SIZE = 20;
  private static final int INVENTORY_LOAD_PARTITION_SIZE = 20;
  /* WorkerExecutor provides a worker pool for export process */
  private WorkerExecutor executor;

  @Autowired
  private RecordLoaderService recordLoaderService;
  @Autowired
  private ExportService exportService;
  @Autowired
  private MappingService mappingService;
  @Autowired
  private JobExecutionService jobExecutionService;
  @Autowired
  private SrsRecordConvertorService srsRecordService;
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
    SrsLoadResult srsLoadResult = loadSrsMarcRecordsInPartitions(identifiers, params);
    LOGGER.info("Records that are not present in SRS: {}", srsLoadResult.getInstanceIdsWithoutSrs());

    List<String> marcToExport = srsRecordService.transformSrsRecords(mappingProfile, srsLoadResult.getUnderlyingMarcRecords(),
          exportPayload.getJobExecutionId(), params);
    exportService.exportSrsRecord(marcToExport, fileExportDefinition);
    List<JsonObject> instances = loadInventoryInstancesInPartitions(srsLoadResult.getInstanceIdsWithoutSrs(), params);
    LOGGER.info("Number of instances, that returned from inventory storage: {}", instances.size());
    LOGGER.info("Number of instances not found either in SRS or Inventory Storage: {}", srsLoadResult.getInstanceIdsWithoutSrs().size() - instances.size());
    instances = fetchHoldingsAndItems(instances, mappingProfile, params);
    List<String> mappedMarcRecords = mappingService.map(instances, mappingProfile, exportPayload.getJobExecutionId(), params);
    exportService.exportInventoryRecords(mappedMarcRecords, fileExportDefinition);
    if (exportPayload.isLast()) {
      exportService.postExport(fileExportDefinition, params.getTenantId());
    }
    exportPayload.setExportedRecordsNumber(srsLoadResult.getUnderlyingMarcRecords().size() + mappedMarcRecords.size());
    exportPayload.setFailedRecordsNumber(identifiers.size() - exportPayload.getExportedRecordsNumber());
  }

  /**
   * For Each instance UUID fetches all the holdings and also items for each holding and appends it to a single record
   *
   * @param instances list of instance objects
   * @param params
   */
  private List<JsonObject> fetchHoldingsAndItems(List<JsonObject> instances, MappingProfile mappingProfile, OkapiConnectionParams params) {
    List<JsonObject> instancesWithHoldingsAndItems = new ArrayList<>();
    for (JsonObject instance : instances) {
      JsonObject instanceWithHoldingsAndItems = new JsonObject();
      instanceWithHoldingsAndItems.put("instance", instance);
      appendHoldingsAndItems(mappingProfile, params, instance.getString("id"), instanceWithHoldingsAndItems);
      instancesWithHoldingsAndItems.add(instanceWithHoldingsAndItems);
    }
    return instancesWithHoldingsAndItems;

  }

  private void appendHoldingsAndItems(MappingProfile mappingProfile, OkapiConnectionParams params, String instanceUUID,
      JsonObject appendHoldingsItems) {
    if (isTransformationRequired(mappingProfile)) {
      List<JsonObject> holdings = recordLoaderService.getHoldingsForInstance(instanceUUID, params);
      appendHoldingsItems.put("holdings", new JsonArray(holdings));
      if (mappingProfile.getRecordTypes().contains(RecordType.ITEM)) {
        List<String> holdingIds = holdings.stream()
          .map(record -> record.getString("id"))
          .collect(Collectors.toList());
        List<JsonObject> items = recordLoaderService.getAllItemsForHolding(holdingIds, params);
        appendHoldingsItems.put("items", new JsonArray(items));
      }
    }
  }

  private boolean isTransformationRequired(MappingProfile mappingProfile) {
    List<Transformations> transformations = mappingProfile.getTransformations();
    List<RecordType> recordTypes = mappingProfile.getRecordTypes();
    return isNotEmpty(transformations) && (recordTypes.contains(RecordType.HOLDINGS) || recordTypes.contains(RecordType.ITEM));

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
      SrsLoadResult partitionLoadResult = recordLoaderService.loadMarcRecordsBlocking(partition, params, SRS_LOAD_PARTITION_SIZE);
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
  private Future<Void> handleExportResult(AsyncResult asyncResult, ExportPayload exportPayload) {
    Promise<Void> promise = Promise.promise();
    JsonObject exportPayloadJson = JsonObject.mapFrom(exportPayload);
    ExportResult exportResult = getExportResult(asyncResult, exportPayload.isLast());
    clearIdentifiers(exportPayload);
    incrementCurrentProgress(exportPayload, exportResult)
      .onComplete(handler -> {
        getInputDataManager().proceed(exportPayloadJson, exportResult);
        promise.complete();
      });
    return promise.future();
  }

  private void clearIdentifiers(ExportPayload exportPayload) {
    exportPayload.setIdentifiers(Collections.emptyList());
  }

  private ExportResult getExportResult(AsyncResult asyncResult, boolean isLast) {
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

  private Future<JobExecution> incrementCurrentProgress(ExportPayload exportPayload, ExportResult exportResult) {
    if (!exportResult.isFailed()) {
      String tenantId = exportPayload.getOkapiConnectionParams().getTenantId();
      int exported = exportPayload.getExportedRecordsNumber();
      int failed = exportPayload.getFailedRecordsNumber();
      return jobExecutionService.incrementCurrentProgress(exportPayload.getJobExecutionId(), exported, failed, tenantId);
    }
    return Future.succeededFuture();
  }

  private InputDataManager getInputDataManager() {
    return vertx.getOrCreateContext().get(InputDataManager.class.getName());
  }
}
