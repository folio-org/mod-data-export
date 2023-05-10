package org.folio.service.manager.input;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.core.shareddata.LocalMap;
import org.apache.commons.io.FilenameUtils;
import org.folio.clients.UsersClient;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.service.file.definition.FileDefinitionService;
import org.folio.service.file.reader.LocalStorageCsvSourceReader;
import org.folio.service.file.reader.SourceReader;
import org.folio.service.job.JobExecutionService;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.manager.export.ExportManager;
import org.folio.service.manager.export.ExportPayload;
import org.folio.service.manager.export.ExportResult;
import org.folio.service.profiles.mappingprofile.MappingProfileServiceImpl;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.rest.jaxrs.model.FileDefinition.UploadFormat.CQL;
import static org.folio.service.manager.export.ExportResult.ExportStatus.COMPLETED_WITH_ERRORS;
import static org.folio.util.ErrorCode.errorCodesAccordingToExport;

/**
 * Acts a source of a uuids to be exported.
 */
@Service
class InputDataManagerImpl implements InputDataManager {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
  private static final int POOL_SIZE = 2;
  private static final String INPUT_DATA_LOCAL_MAP_KEY = "inputDataLocalMap";
  private static final String SHARED_WORKER_EXECUTOR_NAME = "input-data-manager-thread-worker";
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
  @Autowired
  private ErrorLogService errorLogService;

  private WorkerExecutor executor; //NOSONAR
  private LocalMap<String, InputDataContext> inputDataLocalMap; //NOSONAR

  public InputDataManagerImpl() {
  }

  public InputDataManagerImpl(Context context) {
    SpringContextUtil.autowireDependencies(this, context);
    this.executor = context.owner().createSharedWorkerExecutor(SHARED_WORKER_EXECUTOR_NAME, POOL_SIZE);
    this.inputDataLocalMap = context.owner().sharedData().getLocalMap(INPUT_DATA_LOCAL_MAP_KEY);
  }

  @Override
  public void init(JsonObject request, JsonObject requestFileDefinition, JsonObject mappingProfileJson, JsonObject jobExecution, Map<String, String> params) {
    executor.executeBlocking(blockingFuture -> {
      CaseInsensitiveMap<String, String> paramsMap = new CaseInsensitiveMap<>(params);
      initBlocking(request, requestFileDefinition, mappingProfileJson, jobExecution, paramsMap);
      blockingFuture.complete();
    }, this::handleExportInitResult);
  }

  @Override
  public void proceed(JsonObject payload, ExportResult exportResult) {
    executor.executeBlocking(blockingFuture -> {
      proceedBlocking(payload, exportResult);
      blockingFuture.complete();
    }, this::handleExportResult);
  }

  protected void initBlocking(JsonObject exportRequestJson, JsonObject requestFileDefinitionJson, JsonObject mappingProfileJson, JsonObject jobExecutionJson, Map<String, String> params) {
    FileDefinition requestFileDefinition = requestFileDefinitionJson.mapTo(FileDefinition.class);
    MappingProfile mappingProfile = mappingProfileJson.mapTo(MappingProfile.class);
    JobExecution jobExecution = jobExecutionJson.mapTo(JobExecution.class);
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    String tenantId = okapiConnectionParams.getTenantId();
    String jobExecutionId = jobExecution.getId();
    ExportRequest exportRequest = exportRequestJson.mapTo(ExportRequest.class);
    FileDefinition fileExportDefinition = createExportFileDefinition(exportRequest, requestFileDefinition, jobExecution);
    Optional<JsonObject> optionalUser = usersClient.getById(exportRequest.getMetadata().getCreatedByUserId(), jobExecutionId, okapiConnectionParams);
    List<ErrorCode> errorCodes = new ArrayList<>();
    if (exportRequest.getIdType().equals(ExportRequest.IdType.HOLDING)) {
      if (requestFileDefinition.getUploadFormat().equals(CQL)) {
        errorCodes.add(ErrorCode.INVALID_UPLOADED_FILE_EXTENSION_FOR_HOLDING_ID_TYPE);
      }
    } else if (exportRequest.getIdType().equals(ExportRequest.IdType.AUTHORITY)) {
      if (!MappingProfileServiceImpl.isDefaultAuthorityProfile(mappingProfile.getId())) {
        errorCodes.add(ErrorCode.ERROR_ONLY_DEFAULT_AUTHORITY_JOB_PROFILE_IS_SUPPORTED);
      }
      if (requestFileDefinition.getUploadFormat().equals(CQL)) {
        errorCodes.add(ErrorCode.INVALID_UPLOADED_FILE_EXTENSION_FOR_AUTHORITY_ID_TYPE);
      }
    }

    if (isNotEmpty(errorCodes)) {
      errorCodes.forEach(errorCode -> errorLogService.saveGeneralErrorWithMessageValues(errorCode.getCode(), Collections.singletonList(errorCode.getDescription()), jobExecutionId, tenantId));
      fileDefinitionService.save(fileExportDefinition.withStatus(FileDefinition.Status.ERROR), tenantId).onSuccess(savedFileDefinition -> {
        if (optionalUser.isPresent()) {
          jobExecutionService.prepareAndSaveJobForFailedExport(jobExecution, fileExportDefinition, optionalUser.get(), 0, true, tenantId);
        } else {
          ExportPayload exportPayload = createExportPayload(exportRequest, fileExportDefinition, mappingProfile, jobExecutionId, okapiConnectionParams);
          finalizeExport(exportPayload, jobExecution, ExportResult.failed(ErrorCode.USER_NOT_FOUND));
        }
      });
      return;
    }

    SourceReader sourceReader = initSourceReader(requestFileDefinition, jobExecutionId, tenantId, getBatchSize());
    if (sourceReader.hasNext()) {
      fileDefinitionService.save(fileExportDefinition, tenantId).onSuccess(savedFileExportDefinition -> {
        initInputDataContext(sourceReader, jobExecutionId);
        ExportPayload exportPayload = createExportPayload(exportRequest, savedFileExportDefinition, mappingProfile, jobExecutionId, okapiConnectionParams);
        LOGGER.debug("Trying to fetch created User name for user ID {}", exportRequest.getMetadata().getCreatedByUserId());
        if (optionalUser.isPresent()) {
          JsonObject user = optionalUser.get();
          jobExecutionService.prepareJobForExport(jobExecution, fileExportDefinition, user, sourceReader.totalCount(), isNotCQL(requestFileDefinition), tenantId)
          .onSuccess(jobExec -> exportNextChunk(exportPayload, sourceReader))
          .onFailure(ar -> {
            jobExecutionService.prepareAndSaveJobForFailedExport(jobExecution, fileExportDefinition, optionalUser.get(), 0, true, tenantId);
            finalizeExport(exportPayload, jobExecution, ExportResult.failed(ErrorCode.FAIL_TO_UPDATE_JOB));
          });
        } else {
          finalizeExport(exportPayload, jobExecution, ExportResult.failed(ErrorCode.USER_NOT_FOUND));
        }
      })
        .onFailure(throwable -> LOGGER.error("Failed to save file definition.", throwable));
    } else {
      errorLogService.saveGeneralError(ErrorCode.ERROR_READING_FROM_INPUT_FILE.getCode(), jobExecutionId, tenantId);
      fileDefinitionService.save(fileExportDefinition.withStatus(FileDefinition.Status.ERROR), tenantId).onSuccess(savedFileDefinition -> {
        if (optionalUser.isPresent()) {
          jobExecutionService.prepareAndSaveJobForFailedExport(jobExecution, fileExportDefinition, optionalUser.get(), 0, true, tenantId);
        } else {
          ExportPayload exportPayload = createExportPayload(exportRequest, fileExportDefinition, mappingProfile, jobExecutionId, okapiConnectionParams);
          finalizeExport(exportPayload, jobExecution, ExportResult.failed(ErrorCode.USER_NOT_FOUND));
        }
      });
      sourceReader.close();
    }
  }

  protected void proceedBlocking(JsonObject payloadJson, ExportResult exportResult) {
    ExportPayload exportPayload = payloadJson.mapTo(ExportPayload.class);
    if (exportResult.isInProgress()) {
      proceedInProgress(exportPayload);
    } else {
      finalizeExport(exportPayload, null, exportResult);
    }
  }

  private void proceedInProgress(ExportPayload exportPayload) {
    InputDataContext inputDataContext = getInputDataContext(exportPayload.getJobExecutionId());
    SourceReader sourceReader = inputDataContext.getSourceReader();
    if (nonNull(sourceReader) && sourceReader.hasNext()) {
      exportNextChunk(exportPayload, sourceReader);
    } else {
      finalizeExport(exportPayload, null, ExportResult.failed(ErrorCode.GENERIC_ERROR_CODE));
    }
  }

  protected SourceReader initSourceReader(FileDefinition requestFileDefinition, String jobExecutionId, String tenantId, int batchSize) {
    SourceReader sourceReader = new LocalStorageCsvSourceReader();
    sourceReader.init(requestFileDefinition, errorLogService, jobExecutionId, tenantId, batchSize);
    return sourceReader;
  }

  private void exportNextChunk(ExportPayload exportPayload, SourceReader sourceReader) {
    List<String> identifiers = sourceReader.readNext();
    exportPayload.setIdentifiers(identifiers);
    exportPayload.setLast(!sourceReader.hasNext());
    getExportManager().exportData(JsonObject.mapFrom(exportPayload));
  }

  private void finalizeExport(ExportPayload exportPayload, JobExecution jobExecution, ExportResult exportResult) {
    FileDefinition fileExportDefinition = exportPayload.getFileExportDefinition();
    String jobExecutionId = fileExportDefinition.getJobExecutionId();
    String tenantId = exportPayload.getOkapiConnectionParams().getTenantId();
    JobExecution.Status status = getJobExecutionStatus(exportResult);
    ExportResult expResult = new ExportResult(exportResult.toJson());
    //the status obtained here is for the last batch
    if (status.equals(JobExecution.Status.COMPLETED)) {
      // check if there were any errors in the previous batches , if yes then complete the job with
      // "Completed with errors" status
      isSelectedErrorsPresent(jobExecutionId, tenantId)
          .onComplete(
              isAnyErrorPresent -> {
                if (isAnyErrorPresent.succeeded()
                    && Boolean.TRUE.equals(isAnyErrorPresent.result())) {
                  expResult.setStatus(COMPLETED_WITH_ERRORS);
                }
                jobExecutionService.updateJobStatusById(jobExecutionId, jobExecution, getJobExecutionStatus(expResult), tenantId);
                updateFileDefinitionStatusByResult(fileExportDefinition, expResult, tenantId);
              });
    } else {
      jobExecutionService.updateJobStatusById(jobExecutionId, jobExecution, status, tenantId);
      updateFileDefinitionStatusByResult(fileExportDefinition, exportResult, tenantId);
    }
    closeSourceReader(jobExecutionId);
    removeInputDataContext(jobExecutionId);
  }

  private Future<Boolean> isSelectedErrorsPresent(String jobExecutionId, String tenantId) {
    Promise<Boolean> promise = Promise.promise();
    errorLogService
        .isErrorsByErrorCodePresent(errorCodesAccordingToExport(), jobExecutionId, tenantId)
        .onSuccess(promise::complete)
        .onFailure(ar -> promise.complete(false));

    return promise.future();
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
      LOGGER.error("Export of identifiers chunk is failed: {}", asyncResult.cause().getMessage());
    } else {
      LOGGER.info("Export of identifiers chunk has been successfully completed");
    }
    return succeededFuture();
  }

  private ExportPayload createExportPayload(ExportRequest exportRequest, FileDefinition fileExportDefinition, MappingProfile mappingProfile, String jobExecutionId, OkapiConnectionParams okapiParams) {
    ExportPayload exportPayload = new ExportPayload();
    exportPayload.setFileExportDefinition(fileExportDefinition);
    exportPayload.setMappingProfile(mappingProfile);
    exportPayload.setJobExecutionId(jobExecutionId);
    exportPayload.setOkapiConnectionParams(okapiParams);
    if (Objects.nonNull(exportRequest.getRecordType())) {
      exportPayload.setRecordType(exportRequest.getRecordType());
    }
    exportPayload.setIdType(exportRequest.getIdType());
    return exportPayload;
  }

  private FileDefinition createExportFileDefinition(ExportRequest exportRequest, FileDefinition requestFileDefinition, JobExecution jobExecution) {
    String fileNameWithoutExtension = FilenameUtils.getBaseName(requestFileDefinition.getFileName());
    return new FileDefinition()
      .withFileName(fileNameWithoutExtension + DELIMITER + jobExecution.getHrId() + MARC_FILE_EXTENSION)
      .withStatus(FileDefinition.Status.IN_PROGRESS)
      .withJobExecutionId(requestFileDefinition.getJobExecutionId())
      .withMetadata(exportRequest.getMetadata());
  }

  private void initInputDataContext(SourceReader sourceReader, String jobExecutionId) {
    InputDataContext inputDataContext = new InputDataContext(sourceReader);
    inputDataLocalMap.put(jobExecutionId, inputDataContext);
  }

  protected int getBatchSize() {
    return BATCH_SIZE;
  }

  private JobExecution.Status getJobExecutionStatus(ExportResult exportResult) {
    if (exportResult.isCompleted()) {
      return JobExecution.Status.COMPLETED;
    }
    if (exportResult.isCompletedWithErrors()) {
      return JobExecution.Status.COMPLETED_WITH_ERRORS;
    }
    return JobExecution.Status.FAIL;
  }

  private FileDefinition.Status getFileDefinitionStatus(ExportResult exportResult) {
    if (exportResult.isCompleted()) {
      return FileDefinition.Status.COMPLETED;
    }
    return FileDefinition.Status.ERROR;
  }

  private void updateFileDefinitionStatusByResult(FileDefinition fileDefinition, ExportResult exportResult, String tenantId) {
    fileDefinition.withStatus(getFileDefinitionStatus(exportResult));
    fileDefinitionService.update(fileDefinition, tenantId);
  }

  private void closeSourceReader(String jobExecutionId) {
    SourceReader sourceReader = getInputDataContext(jobExecutionId).getSourceReader();
    if (nonNull(sourceReader)) {
      sourceReader.close();
    }
  }

  private void removeInputDataContext(String jobExecutionId) {
    if (inputDataLocalMap.containsKey(jobExecutionId)) {
      inputDataLocalMap.remove(jobExecutionId);
    }
  }

  private boolean isNotCQL(FileDefinition requestFileDefinition) {
    return !CQL.equals(requestFileDefinition.getUploadFormat());
  }

  private InputDataContext getInputDataContext(String jobExecutionId) {
    return inputDataLocalMap.get(jobExecutionId);
  }

}
