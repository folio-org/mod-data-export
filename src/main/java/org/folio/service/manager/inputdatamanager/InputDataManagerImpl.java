package org.folio.service.manager.inputdatamanager;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import org.folio.dao.impl.JobExecutionDaoImpl;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.manager.exportmanager.ExportManager;
import org.folio.service.manager.exportmanager.ExportPayload;
import org.folio.service.manager.inputdatamanager.datacontext.InputDataContext;
import org.folio.service.manager.inputdatamanager.reader.SourceReader;
import org.folio.service.manager.status.ExportStatus;
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
  public static final String SHARED_WORKER_EXECUTOR_NAME = "input-data-manager-thread-worker";
  public static final String TIMESTAMP_PATTERN = "yyyyMMddHHmmss";
  public static final String DELIMITER = "-";

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
    this.executor.executeBlocking(blockingFuture -> initBlocking(request, params), this::handleExportResult);
  }

  protected void initBlocking(JsonObject request, JsonObject params) {
    ExportRequest exportRequest = request.mapTo(ExportRequest.class);
    FileDefinition requestFileDefinition = exportRequest.getFileDefinition();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params.mapTo(Map.class));
    FileDefinition fileExportDefinition = createExportFileDefinition(requestFileDefinition);
    String tenantId = okapiConnectionParams.getTenantId();
    Iterator<List<String>> sourceStream = sourceReader.getSourceStream(requestFileDefinition, exportRequest.getBatchSize());
    if (sourceStream.hasNext()) {
      fileDefinitionService.save(fileExportDefinition, tenantId)
        .compose(savedFileExportDefinition -> {
          //TODO
          //get executionJob and update status to in-progress
          String jobExecutionId = requestFileDefinition.getJobExecutionId();
          initInputDataContext(sourceStream, jobExecutionId);
          ExportPayload exportPayload = createExportPayload(okapiConnectionParams, savedFileExportDefinition, jobExecutionId);
          exportNextChunk(exportPayload, sourceStream);
          return Future.succeededFuture();
        });
    } else {
      fileDefinitionService.save(fileExportDefinition.withStatus(FileDefinition.Status.ERROR), tenantId);
      //TODO
      //update job execution with error status
    }
  }

  @Override
  public void proceed(JsonObject payload, ExportStatus exportStatus) {
    ExportPayload exportPayload = payload.mapTo(ExportPayload.class);
    if (ExportStatus.IN_PROGRESS.equals(exportStatus)) {
      InputDataContext inputDataContext = inputDataLocalMap.get(exportPayload.getJobExecutionId());
      Iterator<List<String>> sourceStream = inputDataContext.getSourceStream();
      if (Objects.nonNull(sourceStream) && sourceStream.hasNext()) {
        exportNextChunk(exportPayload, sourceStream);
      } else {
        finalizeExport(exportPayload, ExportStatus.ERROR);
      }
    } else {
      finalizeExport(exportPayload, exportStatus);
    }
  }

  private void exportNextChunk(ExportPayload exportPayload, Iterator<List<String>> sourceStream) {
    List<String> identifiers = sourceStream.next();
    exportPayload.setIdentifiers(identifiers);
    exportPayload.setLast(!sourceStream.hasNext());
    getExportManager().exportData(JsonObject.mapFrom(exportPayload));
  }

  private void finalizeExport(ExportPayload exportPayload, ExportStatus exportStatus) {
    //TODO
    //update jobExecution status to Failed
    FileDefinition fileExportDefinition = exportPayload.getFileExportDefinition();
    if (ExportStatus.COMPLETED.equals(exportStatus))
      fileExportDefinition.withStatus(FileDefinition.Status.COMPLETED);
    if (ExportStatus.ERROR.equals(exportStatus))
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

  private Future<Void> handleExportResult(AsyncResult asyncResult) {
    if (asyncResult.failed()) {
      LOGGER.error("Export is failed, cause: " + asyncResult.cause());
    } else {
      LOGGER.info("Export has been successfully completed");
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

  private void initInputDataContext(Iterator<List<String>> sourceStream, String jobExecutionId) {
    InputDataContext inputDataContext = new InputDataContext(sourceStream);
    inputDataLocalMap.put(jobExecutionId, inputDataContext);
  }

}
