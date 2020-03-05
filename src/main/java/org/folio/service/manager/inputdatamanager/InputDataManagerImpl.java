package org.folio.service.manager.inputdatamanager;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.folio.dao.impl.JobExecutionDaoImpl;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.manager.exportmanager.ExportManager;
import org.folio.service.manager.exportmanager.ExportPayload;
import org.folio.service.manager.inputdatamanager.datacontext.InputDataContext;
import org.folio.service.manager.inputdatamanager.reader.SourceReader;
import org.folio.service.manager.exportresult.ExportResult;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.vertx.core.Future.succeededFuture;

/**
 * Acts a source of a uuids to be exported.
 */
@SuppressWarnings({"java:S1172", "java:S125"})
@Service
class InputDataManagerImpl implements InputDataManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int POOL_SIZE = 1;
  private static final String INPUT_DATA_LOCAL_MAP_KEY = "inputDataLocalMap";
  private static final String SHARED_WORKER_EXECUTOR_NAME = "input-data-manager-thread-worker";
  private static final String TIMESTAMP_PATTERN = "yyyyMMddHHmmss";
  private static final String DELIMITER = "-";
  private static final int BATCH_SIZE = 1;


  @Autowired
  private SourceReader sourceReader;
  @Autowired
  private JobExecutionDaoImpl jobExecutionDao;
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
  public void init(JsonObject request, JsonObject params) {
    executor.executeBlocking(blockingFuture -> {
      initBlocking(request, params);
      blockingFuture.complete();
    }, this::handleExportInitResult);
  }

  protected void initBlocking(JsonObject request, JsonObject params) {
    ExportRequest exportRequest = request.mapTo(ExportRequest.class);
    FileDefinition requestFileDefinition = exportRequest.getFileDefinition();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params.mapTo(Map.class));
    FileDefinition fileExportDefinition = createExportFileDefinition(requestFileDefinition);
    String tenantId = okapiConnectionParams.getTenantId();
    Iterator<List<String>> readFileContent = sourceReader.getFileContentIterator(requestFileDefinition, getBatchSize());
    if (readFileContent.hasNext()) {
      fileDefinitionService.save(fileExportDefinition, tenantId)
        .compose(savedFileExportDefinition -> {
          //get executionJob and update exportresult to in-progress
          String jobExecutionId = requestFileDefinition.getJobExecutionId();
          initInputDataContext(readFileContent, jobExecutionId);
          ExportPayload exportPayload = createExportPayload(okapiConnectionParams, savedFileExportDefinition, jobExecutionId);
          exportNextChunk(exportPayload, readFileContent);
          return Future.succeededFuture();
        });
    } else {
      fileDefinitionService.save(fileExportDefinition.withStatus(FileDefinition.Status.ERROR), tenantId);
      //update job execution with error exportresult
    }
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
    if (ExportResult.IN_PROGRESS.equals(exportResult)) {
      InputDataContext inputDataContext = inputDataLocalMap.get(exportPayload.getJobExecutionId());
      Iterator<List<String>> fileContentIterator = inputDataContext.getFileContentIterator();
      if (Objects.nonNull(fileContentIterator) && fileContentIterator.hasNext()) {
        exportNextChunk(exportPayload, fileContentIterator);
      } else {
        finalizeExport(exportPayload, ExportResult.ERROR);
      }
    } else {
      finalizeExport(exportPayload, exportResult);
    }
  }

  private void exportNextChunk(ExportPayload exportPayload, Iterator<List<String>> fileContentIterator) {
    List<String> identifiers = fileContentIterator.next();
    exportPayload.setIdentifiers(identifiers);
    exportPayload.setLast(!fileContentIterator.hasNext());
    getExportManager().exportData(JsonObject.mapFrom(exportPayload));
  }

  private void finalizeExport(ExportPayload exportPayload, ExportResult exportResult) {
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

  private void initInputDataContext(Iterator<List<String>> fileContentIterator, String jobExecutionId) {
    InputDataContext inputDataContext = new InputDataContext(fileContentIterator);
    inputDataLocalMap.put(jobExecutionId, inputDataContext);
  }

  protected int getBatchSize() {
    return BATCH_SIZE;
  }

}
