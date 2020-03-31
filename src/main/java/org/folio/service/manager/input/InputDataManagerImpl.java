package org.folio.service.manager.input;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import org.apache.commons.io.FilenameUtils;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.folio.clients.UsersClient;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.service.job.JobExecutionService;
import org.folio.service.manager.export.ExportManager;
import org.folio.service.manager.export.ExportPayload;
import org.folio.service.manager.export.ExportResult;
import org.folio.service.file.reader.LocalStorageCsvSourceReader;
import org.folio.service.file.reader.SourceReader;
import org.folio.service.file.definition.FileDefinitionService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
  private static final String MARC_FILE_EXTENSION = ".mrc";

  @Autowired
  private JobExecutionService jobExecutionService;
  @Autowired
  private FileDefinitionService fileDefinitionService;
  @Autowired
  private Vertx vertx;
  @Autowired
  private UsersClient usersClient;

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
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    String tenantId = okapiConnectionParams.getTenantId();
    fileDefinitionService.findFileDefinition(exportRequest.getFileDefinitionId(), tenantId).onSuccess(requestFileDefinition -> {
      String jobExecutionId = requestFileDefinition.getJobExecutionId();
      if (requestFileDefinition.getStatus().equals(FileDefinition.Status.COMPLETED)) {
        FileDefinition fileExportDefinition = createExportFileDefinition(exportRequest, requestFileDefinition);
        SourceReader sourceReader = initSourceReader(requestFileDefinition, getBatchSize());
        if (sourceReader.hasNext()) {
          fileDefinitionService.save(fileExportDefinition, tenantId).onSuccess(savedFileExportDefinition -> {
            initInputDataContext(sourceReader, jobExecutionId);
            ExportPayload exportPayload = createExportPayload(okapiConnectionParams, savedFileExportDefinition, jobExecutionId);
            Optional<JsonObject> optionalUser = usersClient.getById(exportRequest.getMetadata().getCreatedByUserId(), okapiConnectionParams);
            if (optionalUser.isPresent()) {
              JsonObject user = optionalUser.get();
              jobExecutionService.prepareJobForExport(jobExecutionId, fileExportDefinition, user, tenantId);
              exportNextChunk(exportPayload, sourceReader);
            } else {
              finalizeExport(exportPayload, ExportResult.failed(ErrorCode.USER_NOT_FOUND), sourceReader);
            }
          });
        } else {
          fileDefinitionService.save(fileExportDefinition.withStatus(FileDefinition.Status.ERROR), tenantId);
          jobExecutionService.updateJobStatusById(jobExecutionId, JobExecution.Status.FAIL, tenantId);
          sourceReader.close();
        }
      } else {
        LOGGER.error(String.format("Failed to start export process, file definition status with id %s is not COMPLETED", exportRequest.getFileDefinitionId()));
        jobExecutionService.updateJobStatusById(jobExecutionId, JobExecution.Status.FAIL, tenantId);
      }
    });
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

  protected void proceedBlocking(JsonObject payloadJson, ExportResult exportResult) {
    ExportPayload exportPayload = payloadJson.mapTo(ExportPayload.class);
    InputDataContext inputDataContext = inputDataLocalMap.get(exportPayload.getJobExecutionId());
    SourceReader sourceReader = inputDataContext.getSourceReader();
    if (exportResult.isInProgress()) {
      if (nonNull(sourceReader) && sourceReader.hasNext()) {
        exportNextChunk(exportPayload, sourceReader);
      } else {
        finalizeExport(exportPayload, ExportResult.failed(ErrorCode.GENERIC_ERROR_CODE), sourceReader);
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
    if (nonNull(sourceReader)) {
      sourceReader.close();
    }
    FileDefinition fileExportDefinition = exportPayload.getFileExportDefinition();
    String jobExecutionId = fileExportDefinition.getJobExecutionId();
    String tenantId = exportPayload.getOkapiConnectionParams().getTenantId();
    if (exportResult.isCompleted()) {
      fileExportDefinition.withStatus(FileDefinition.Status.COMPLETED);
      jobExecutionService.updateJobStatusById(jobExecutionId, JobExecution.Status.SUCCESS, tenantId);
    }
    if (exportResult.isFailed()) {
      fileExportDefinition.withStatus(FileDefinition.Status.ERROR);
      // update job cause
      jobExecutionService.updateJobStatusById(jobExecutionId, JobExecution.Status.FAIL, tenantId);
    }
    fileDefinitionService.update(fileExportDefinition, tenantId)
      .onComplete(saveAsyncResult -> {
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
      LOGGER.error("Initialization of export is failed", asyncResult.cause().getMessage());
    } else {
      LOGGER.info("Initialization of export has been successfully completed");
    }
    return succeededFuture();
  }

  private Future<Void> handleExportResult(AsyncResult asyncResult) {
    if (asyncResult.failed()) {
      LOGGER.error("Export of identifiers chunk is failed", asyncResult.cause().getMessage());
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

  private FileDefinition createExportFileDefinition(ExportRequest exportRequest, FileDefinition requestFileDefinition) {
    String fileNameWithoutExtension = FilenameUtils.getBaseName(requestFileDefinition.getFileName());
    return new FileDefinition()
      .withFileName(fileNameWithoutExtension + DELIMITER + getCurrentTimestamp() + MARC_FILE_EXTENSION)
      .withStatus(FileDefinition.Status.IN_PROGRESS)
      .withJobExecutionId(requestFileDefinition.getJobExecutionId())
      .withMetadata(exportRequest.getMetadata());
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
