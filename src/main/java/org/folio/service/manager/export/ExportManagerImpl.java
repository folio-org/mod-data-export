package org.folio.service.manager.export;

import com.google.common.collect.Lists;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.export.ExportService;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.manager.input.InputDataManager;
import org.folio.service.mapping.MappingService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.OkapiConnectionParams;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.vertx.core.Future.succeededFuture;

/**
 * The ExportManager is a central part of the data-export.
 * Runs the main export process calling other internal services along the way.
 */
@SuppressWarnings({"java:S1172", "java:S125"})
@Service
public class ExportManagerImpl implements ExportManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExportManagerImpl.class);
  private static final int POOL_SIZE = 1;
  private static final int SRS_LOAD_PARTITION_SIZE = 15;
  private static final int INVENTORY_LOAD_PARTITION_SIZE = 15;
  /* WorkerExecutor provides a worker pool for export process */
  private WorkerExecutor executor;

  @Autowired
  private RecordLoaderService recordLoaderService;
  @Autowired
  private ExportService exportService;
  @Autowired
  private MappingService mappingService;
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
    OkapiConnectionParams params = exportPayload.getOkapiConnectionParams();
    SrsLoadResult srsLoadResult = loadSrsMarcRecordsInPartitions(identifiers, params);
    exportService.export(srsLoadResult.getUnderlyingMarcRecords(), fileExportDefinition);
    List<JsonObject> instances = loadInventoryInstancesInPartitions(srsLoadResult.getInstanceIdsWithoutSrs(), params);
    List<String> mappedMarcRecords = mappingService.map(instances);
    exportService.export(mappedMarcRecords, fileExportDefinition);

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
        List<JsonObject> partitionLoadResult = recordLoaderService.loadInventoryInstancesBlocking(partition, params);
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
    clearIdentifiers(exportPayload);
    JsonObject exportPayloadJson = JsonObject.mapFrom(exportPayload);
    ExportResult exportResult = getExportResult(asyncResult, exportPayload.isLast());
    getInputDataManager().proceed(exportPayloadJson, exportResult);
    return succeededFuture();
  }

  private void clearIdentifiers(ExportPayload exportPayload) {
    exportPayload.setIdentifiers(Collections.emptyList());
  }

  @NotNull
  private ExportResult getExportResult(AsyncResult asyncResult, boolean isLast) {
    if (asyncResult.failed()) {
      LOGGER.error("Export is failed, cause: " + asyncResult.cause());
      return ExportResult.ERROR;
    } else {
      LOGGER.info("Export has been successfully passed");
      // update job progress
      if (isLast) {
        return ExportResult.COMPLETED;
      } else {
        return ExportResult.IN_PROGRESS;
      }
    }
  }

  private InputDataManager getInputDataManager() {
    return vertx.getOrCreateContext().get(InputDataManager.class.getName());
  }
}
