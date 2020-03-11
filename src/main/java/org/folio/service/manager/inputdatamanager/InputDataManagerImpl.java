package org.folio.service.manager.inputdatamanager;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.manager.exportmanager.ExportManager;
import org.folio.service.manager.exportmanager.ExportPayload;
import org.folio.service.manager.exportresult.ExportResult;
import org.folio.service.manager.inputdatamanager.reader.LocalStorageCsvSourceReader;
import org.folio.service.manager.inputdatamanager.reader.SourceReader;
import org.folio.service.upload.definition.FileDefinitionService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.OkapiConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Objects.nonNull;

/**
 * Acts a source of a uuids to be exported.
 */
@Service
class InputDataManagerImpl implements InputDataManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int POOL_SIZE = 1;
  private static final String INPUT_DATA_LOCAL_MAP_KEY = "inputDataLocalMap";
  private static final String SHARED_WORKER_EXECUTOR_NAME = "input-data-manager-thread-worker";
  private static final String TIMESTAMP_PATTERN = "yyyyMMddHHmmss";
  private static final String DELIMITER = "-";
  private static final int BATCH_SIZE = 50;


  @Autowired
  private SourceReader sourceReader;
  @Autowired
  private FileDefinitionService fileDefinitionService;
  @Autowired
  private Vertx vertx;

  private WorkerExecutor executor;
  private LocalMap<String, InputDataContext> inputDataLocalMap;

  public InputDataManagerImpl() {
  }

  public InputDataManagerImpl(Context context) {
    SpringContextUtil.autowireDependencies(this, context);
    this.executor = context.owner().createSharedWorkerExecutor(SHARED_WORKER_EXECUTOR_NAME, POOL_SIZE);
    this.inputDataLocalMap = context.owner().sharedData().getLocalMap(INPUT_DATA_LOCAL_MAP_KEY);
  }

  @Override
  public void init(JsonObject request, Map<String, String> params) {
    executor.executeBlocking(blockingFuture -> {
      initBlocking(request, params);
      blockingFuture.complete();
    }, this::handleExportInitResult);
  }

  protected void initBlocking(JsonObject request, Map<String, String> params) {
    ExportRequest exportRequest = request.mapTo(ExportRequest.class);
    FileDefinition requestFileDefinition = exportRequest.getFileDefinition();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    FileDefinition fileExportDefinition = createExportFileDefinition(requestFileDefinition);
    String tenantId = okapiConnectionParams.getTenantId();
    SourceReader sourceReader = initSourceReader(requestFileDefinition, getBatchSize());
    if (sourceReader.hasNext()) {
      fileDefinitionService.save(fileExportDefinition, tenantId)
        .compose(savedFileExportDefinition -> {
          //get executionJob and update exportresult to in-progress
          String jobExecutionId = requestFileDefinition.getJobExecutionId();
          initInputDataContext(sourceReader, jobExecutionId);
          ExportPayload exportPayload = createExportPayload(okapiConnectionParams, savedFileExportDefinition, jobExecutionId);
          exportNextChunk(exportPayload, sourceReader);
          return Future.succeededFuture();
        });
    } else {
      fileDefinitionService.save(fileExportDefinition.withStatus(FileDefinition.Status.ERROR), tenantId);
      sourceReader.close();
      //update job execution with error exportresult
    }
  }

  protected SourceReader initSourceReader(FileDefinition requestFileDefinition, int batchSize) {
    SourceReader sourceReader = new LocalStorageCsvSourceReader();
    sourceReader.init(requestFileDefinition, batchSize);
    return sourceReader;
  }

  @Override
  public void proceed(JsonObject payload, ExportResult exportResult) {
    executor.executeBlocking(blockingFuture -> {
      proceedBlocking(payload, exportResult);
      blockingFuture.complete();
    }, this::handleExportResult);
  }

  protected void proceedBlocking(JsonObject payload, ExportResult exportResult) {
    ExportPayload exportPayload = payload.mapTo(ExportPayload.class);
    InputDataContext inputDataContext = inputDataLocalMap.get(exportPayload.getJobExecutionId());
    SourceReader sourceReader = inputDataContext.getSourceReader();
    if (ExportResult.IN_PROGRESS.equals(exportResult)) {
      if (nonNull(sourceReader) && sourceReader.hasNext()) {
        exportNextChunk(exportPayload, sourceReader);
      } else {
        finalizeExport(exportPayload, ExportResult.ERROR, sourceReader);
      }
    } else {
      finalizeExport(exportPayload, exportResult, sourceReader);
    }
  }

  private void exportNextChunk(ExportPayload exportPayload, SourceReader sourceReader) {
    List<String> identifiers = sourceReader.readNext();
    exportPayload.setIdentifiers(identifiers);
    exportPayload.setLast(!sourceReader.hasNext());
    getExportManager().exportData(JsonObject.mapFrom(exportPayload));
  }

  private void finalizeExport(ExportPayload exportPayload, ExportResult exportResult, SourceReader sourceReader) {
    if(nonNull(sourceReader)) {
      sourceReader.close();
    }
    //update jobExecution status to Failed
    FileDefinition fileExportDefinition = exportPayload.getFileExportDefinition();
    if (ExportResult.COMPLETED.equals(exportResult))
      fileExportDefinition.withStatus(FileDefinition.Status.COMPLETED);
    if (ExportResult.ERROR.equals(exportResult))
      fileExportDefinition.withStatus(FileDefinition.Status.ERROR);
    String tenantId = exportPayload.getOkapiConnectionParams().getTenantId();
    fileDefinitionService.update(fileExportDefinition, tenantId)
      .onComplete(saveAsyncResult -> {
        String jobExecutionId = exportPayload.getJobExecutionId();
        if (inputDataLocalMap.containsKey(jobExecutionId)) {
          inputDataLocalMap.remove(jobExecutionId);
        }
      });
  }

  protected ExportManager getExportManager() {
    return vertx.getOrCreateContext().get(ExportManager.class.getName());
  }

  private Future<Void> handleExportInitResult(AsyncResult asyncResult) {
    if (asyncResult.failed()) {
      LOGGER.error("Initialization of export is failed, cause: {}", asyncResult.cause());
    } else {
      LOGGER.info("Initialization of export has been successfully completed");
    }
    return succeededFuture();
  }

  private Future<Void> handleExportResult(AsyncResult asyncResult) {
    if (asyncResult.failed()) {
      LOGGER.error("Export of identifiers chunk is failed, cause: {}", asyncResult.cause());
    } else {
      LOGGER.info("Export of identifiers chunk has been successfully completed");
    }
    return succeededFuture();
  }

  private ExportPayload createExportPayload(OkapiConnectionParams okapiParams, FileDefinition fileExportDefinition, String jobExecutionId) {
    ExportPayload exportPayload = new ExportPayload();
    exportPayload.setOkapiConnectionParams(okapiParams);
    exportPayload.setFileExportDefinition(fileExportDefinition);
    exportPayload.setJobExecutionId(jobExecutionId);
    return exportPayload;
  }

  private FileDefinition createExportFileDefinition(FileDefinition requestFileDefinition) {
    return new FileDefinition()
      .withFileName(requestFileDefinition.getFileName() + DELIMITER + getCurrentTimestamp())
      .withStatus(FileDefinition.Status.IN_PROGRESS);
  }

  protected String getCurrentTimestamp() {
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN);
    return now.format(formatter);
  }

  private void initInputDataContext(SourceReader sourceReader, String jobExecutionId) {
    InputDataContext inputDataContext = new InputDataContext(sourceReader);
    inputDataLocalMap.put(jobExecutionId, inputDataContext);
  }

  protected int getBatchSize() {
    return BATCH_SIZE;
  }

}
