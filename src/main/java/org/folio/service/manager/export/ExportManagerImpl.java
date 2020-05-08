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
import org.drools.core.base.extractors.ArrayElementReader;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.service.export.ExportService;
import org.folio.service.job.JobExecutionService;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.manager.input.InputDataManager;
import org.folio.service.mapping.MappingService;
import org.folio.service.mapping.processor.RuleFactory;
import org.folio.service.mapping.processor.rule.Rule;
import org.folio.service.mapping.profiles.MappingProfile;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * The ExportManager is a central part of the data-export.
 * Runs the main export process calling other internal services along the way.
 */
@SuppressWarnings({"java:S1172", "java:S125"})
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
  private RuleFactory ruleFactory;
  @Autowired
  private JobExecutionService jobExecutionService;
  @Autowired
  private Vertx vertx;
  @Autowired
  private MappingService mappingService;

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
    MappingProfile mappingProfile = new MappingProfile();
    FileDefinition fileExportDefinition = exportPayload.getFileExportDefinition();
    OkapiConnectionParams params = exportPayload.getOkapiConnectionParams();
    SrsLoadResult srsLoadResult = loadSrsMarcRecordsInPartitions(identifiers, params);
    LOGGER.info("Records that are not presenting in SRS: {}", srsLoadResult.getInstanceIdsWithoutSrs());
    exportService.exportSrsRecord(srsLoadResult.getUnderlyingMarcRecords(), fileExportDefinition);
    List<JsonObject> instances = loadInventoryInstancesInPartitions(srsLoadResult.getInstanceIdsWithoutSrs(), params);
    LOGGER.info("Number of instances, that returned from inventory storage: {}", instances.size());
    LOGGER.info("Number of not found instances: {}", srsLoadResult.getInstanceIdsWithoutSrs().size() - instances.size());
    if (isNotEmpty(mappingProfile.getMappingProfileFields())) {
      instances = appendHoldingsAndItemRecords(instances, params);
    }
    List<String> mappedMarcRecords = mappingService.map(instances, mappingProfile, exportPayload.getJobExecutionId(), params);
    exportService.exportInventoryRecords(mappedMarcRecords, fileExportDefinition);
    if (exportPayload.isLast()) {
      exportService.postExport(fileExportDefinition, params.getTenantId());
    }
    exportPayload.setExportedRecordsNumber(srsLoadResult.getUnderlyingMarcRecords().size());
  }

  private List<JsonObject> appendHoldingsAndItemRecords(List<JsonObject> instances, OkapiConnectionParams params) {
    List<JsonObject> instancesWithHoldingsAndItems = new ArrayList<>();
    for (JsonObject instance : instances) {
      List<JsonObject> holdings = loadInventoryHoldingsByInstanceId(instance.getString("id"), params);
      List<JsonObject> items = new ArrayList<>();
      JsonObject instanceWithHoldingsAndItems = new JsonObject();
      for (JsonObject holding : holdings) {
        items.addAll(loadInventoryItemsByHoldingsId(holding.getString("id"), params));
      }
      instanceWithHoldingsAndItems.put("instance", instance);
      instanceWithHoldingsAndItems.put("holdings", new JsonArray(holdings));
      instanceWithHoldingsAndItems.put("item", new JsonArray(items));
      instancesWithHoldingsAndItems.add(instanceWithHoldingsAndItems);
    }
    return instancesWithHoldingsAndItems;
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

  private List<JsonObject> loadInventoryHoldingsByInstanceId(String instanceIdentifier, OkapiConnectionParams params) {
    return new ArrayList<>();
  }

  private List<JsonObject> loadInventoryItemsByHoldingsId(String holdingsIdentifier, OkapiConnectionParams params) {
    return new ArrayList<>();
  }

  /**
   * Handles async result of export, this code gets processing in the main event loop
   *
   * @param asyncResult   result of export
   * @param exportPayload payload of the export request
   * @return return future
   */
  private Future<Void> handleExportResult(AsyncResult asyncResult, ExportPayload exportPayload) {
    Promise promise = Promise.promise();
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
      LOGGER.error("Export is failed, cause: " + asyncResult.cause().getMessage());
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
      int exportedRecordsNumber = exportPayload.getExportedRecordsNumber();
      return jobExecutionService.incrementCurrentProgress(exportPayload.getJobExecutionId(), exportedRecordsNumber, tenantId);
    }
    return Future.succeededFuture();
  }

  private InputDataManager getInputDataManager() {
    return vertx.getOrCreateContext().get(InputDataManager.class.getName());
  }

}
