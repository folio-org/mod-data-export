package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.jaxrs.model.QuickExportRequest;
import org.folio.rest.jaxrs.model.QuickExportResponse;
import org.folio.rest.jaxrs.resource.DataExport;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.service.file.cleanup.StorageCleanupService;
import org.folio.service.file.definition.FileDefinitionService;
import org.folio.service.file.upload.FileUploadService;
import org.folio.service.job.JobExecutionService;
import org.folio.service.manager.input.InputDataManager;
import org.folio.service.profiles.jobprofile.JobProfileService;
import org.folio.service.profiles.mappingprofile.MappingProfileService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ExceptionToResponseMapper;
import org.folio.util.OkapiConnectionParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;

public class DataExportImpl implements DataExport {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());


  @Autowired
  private JobExecutionService jobExecutionService;

  @Autowired
  private JobProfileService jobProfileService;

  @Autowired
  private MappingProfileService mappingProfileService;

  @Autowired
  private FileDefinitionService fileDefinitionService;

  @Autowired
  private FileUploadService fileUploadService;

  @Autowired
  private DataExportHelper dataExportHelper;

  @Autowired
  private StorageCleanupService storageCleanupService;

  private InputDataManager inputDataManager;

  private String tenantId;

  public DataExportImpl(Vertx vertx, String tenantId) {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
    this.tenantId = TenantTool.calculateTenantId(tenantId);
    this.inputDataManager = vertx.getOrCreateContext().get(InputDataManager.class.getName());
  }

  @Override
  @Validate
  public void postDataExportExport(ExportRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LOGGER.info("Starting the data-export process, request: {}", entity);
    fileDefinitionService.getById(entity.getFileDefinitionId(), tenantId)
      .onSuccess(requestFileDefinition ->
        jobProfileService.getById(entity.getJobProfileId(), tenantId)
          .onSuccess(jobProfile ->
            mappingProfileService.getById(jobProfile.getMappingProfileId(), tenantId)
              .onSuccess(mappingProfile ->
                jobExecutionService.getById(requestFileDefinition.getJobExecutionId(), tenantId)
                  .onSuccess(jobExecution ->
                    jobExecutionService.update(jobExecution.withJobProfileId(jobProfile.getId()), tenantId)
                      .onSuccess(updatedJobExecution -> {
                        inputDataManager.init(JsonObject.mapFrom(entity), JsonObject.mapFrom(requestFileDefinition), JsonObject.mapFrom(mappingProfile), JsonObject.mapFrom(updatedJobExecution), okapiHeaders);
                        succeededFuture()
                          .map(PostDataExportExportResponse.respond204())
                          .map(Response.class::cast)
                          .onComplete(asyncResultHandler);
                      }).onFailure(ar -> failToFetchObjectHelper(ar.getMessage(), asyncResultHandler)))
                  .onFailure(ar -> failToFetchObjectHelper(ar.getMessage(), asyncResultHandler)))
              .onFailure(ar -> failToFetchObjectHelper(ar.getMessage(), asyncResultHandler)))
          .onFailure(ar -> failToFetchObjectHelper(ar.getMessage(), asyncResultHandler)))
      .onFailure(ar -> failToFetchObjectHelper(ar.getMessage(), asyncResultHandler));
  }

  @Override
  public void postDataExportQuickExport(QuickExportRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LOGGER.info("Starting the data-quick-export process, request: {}", entity);
    getJobProfileForQuickExport(entity)
      .onSuccess(jobProfile ->
        getFileDefinitionForQuickExport(entity, jobProfile.getId(), new OkapiConnectionParams(okapiHeaders), asyncResultHandler)
          .onSuccess(requestFileDefinition ->
            mappingProfileService.getById(jobProfile.getMappingProfileId(), tenantId)
              .onSuccess(mappingProfile ->
                jobExecutionService.getById(requestFileDefinition.getJobExecutionId(), tenantId)
                  .onSuccess(jobExecution ->
                    jobExecutionService.update(jobExecution.withJobProfileId(jobProfile.getId()), tenantId)
                      .onSuccess(updatedJobExecution -> {
                        inputDataManager.init(JsonObject.mapFrom(buildExportRequest(requestFileDefinition.getId(), jobProfile.getId(), entity)), JsonObject.mapFrom(requestFileDefinition), JsonObject.mapFrom(mappingProfile), JsonObject.mapFrom(updatedJobExecution), okapiHeaders);
                        succeededFuture()
                          .map(PostDataExportQuickExportResponse.respond200WithApplicationJson(new QuickExportResponse()
                            .withJobExecutionId(jobExecution.getId())
                            .withJobExecutionHrId(jobExecution.getHrId())))
                          .map(Response.class::cast)
                          .onComplete(asyncResultHandler);
                      }).onFailure(ar -> failToFetchObjectHelper(ar.getMessage(), asyncResultHandler)))
                  .onFailure(ar -> failToFetchObjectHelper(ar.getMessage(), asyncResultHandler)))
              .onFailure(ar -> failToFetchObjectHelper(ar.getMessage(), asyncResultHandler)))
          .onFailure(ar -> failToFetchObjectHelper(ar.getMessage(), asyncResultHandler)))
      .onFailure(ar -> failToFetchObjectHelper(ar.getMessage(), asyncResultHandler));
  }

  @Override
  @Validate
  public void getDataExportJobExecutions(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext(v -> jobExecutionService.get(query, offset, limit, tenantId)
      .map(GetDataExportJobExecutionsResponse::respond200WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler));
  }

  @Override
  @Validate
  public void getDataExportJobExecutionsDownloadByJobExecutionIdAndExportFileId(String jobId, String exportFileId,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture().compose(ar -> dataExportHelper.getDownloadLink(jobId, exportFileId, tenantId))
      .map(GetDataExportJobExecutionsDownloadByJobExecutionIdAndExportFileIdResponse::respond200WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);
  }

  @Override
  public void postDataExportExpireJobs(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext(v -> jobExecutionService.expireJobExecutions(tenantId)
      .map(PostDataExportExpireJobsResponse.respond204())
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler));
  }

  @Override
  public void postDataExportCleanUpFiles(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext(v -> storageCleanupService.cleanStorage(new OkapiConnectionParams(okapiHeaders))
      .map(PostDataExportCleanUpFilesResponse.respond204())
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler));
  }

  @Override
  public void deleteDataExportJobExecutionsById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture().compose(ar -> jobExecutionService.deleteById(id, tenantId))
        .map(isDeleted -> Boolean.TRUE.equals(isDeleted)
            ? DeleteDataExportJobExecutionsByIdResponse.respond204()
            : DeleteDataExportJobExecutionsByIdResponse
                .respond404WithTextPlain(format("JobExecution with id '%s' was not found", id)))
        .map(Response.class::cast).otherwise(ExceptionToResponseMapper::map)
        .onComplete(asyncResultHandler);

  }

  private void failToFetchObjectHelper(String errorMessage, Handler<AsyncResult<Response>> asyncResultHandler) {
    LOGGER.error(errorMessage);
    succeededFuture()
      .map(PostDataExportExportResponse.respond400WithTextPlain(errorMessage))
      .map(Response.class::cast)
      .onComplete(asyncResultHandler);
  }

  private Future<FileDefinition> getFileDefinitionForQuickExport(QuickExportRequest request, String jobProfileId, OkapiConnectionParams params, Handler<AsyncResult<Response>> asyncResultHandler) {
    Promise<FileDefinition> promise = Promise.promise();
    fileDefinitionService.prepareFileDefinitionForQuickExport(request, jobProfileId, tenantId)
      .onSuccess(fileDefinition -> fileUploadService.uploadFileDependsOnTypeForQuickExport(request, fileDefinition, params)
        .onSuccess(uploadedFileDefinition -> fileUploadService.completeUploading(uploadedFileDefinition, tenantId))
        .onSuccess(promise::complete)
        .onFailure(ar -> promise.fail(ar.getCause()))
        .onFailure(ar -> promise.fail(ar.getCause())))
      .onFailure(handler -> failToFetchObjectHelper(handler.getMessage(), asyncResultHandler));
    return promise.future();
  }

  /**
   * In the current implementation UI will not send jobProfileId. Remove this method and make jobProfileId
   * as required in quickExportRequest when we support all profiles.
   */
  private Future<JobProfile> getJobProfileForQuickExport(QuickExportRequest request) {
    return StringUtils.isNotEmpty(request.getJobProfileId())
      ? jobProfileService.getById(request.getJobProfileId(), tenantId)
      : jobProfileService.getDefault(tenantId);
  }

  private ExportRequest buildExportRequest(String fileDefinitionId, String jobProfileId, QuickExportRequest entity) {
    return new ExportRequest()
      .withFileDefinitionId(fileDefinitionId)
      .withJobProfileId(jobProfileId)
      .withMetadata(entity.getMetadata())
      .withRecordType(ExportRequest.RecordType.fromValue(entity.getRecordType().toString()));
  }

}
