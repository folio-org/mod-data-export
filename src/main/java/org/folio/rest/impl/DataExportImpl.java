package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.resource.DataExport;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.service.file.definition.FileDefinitionService;
import org.folio.service.job.JobExecutionService;
import org.folio.service.manager.input.InputDataManager;
import org.folio.service.profiles.jobprofile.JobProfileService;
import org.folio.service.profiles.mappingprofile.MappingProfileService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ExceptionToResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import javax.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;

public class DataExportImpl implements DataExport {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  @Autowired
  private JobExecutionService jobExecutionService;

  @Autowired
  private JobProfileService jobProfileService;

  @Autowired
  private MappingProfileService mappingProfileService;

  @Autowired
  private FileDefinitionService fileDefinitionService;

  @Autowired
  private DataExportHelper dataExportHelper;

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

  private void failToFetchObjectHelper(String errorMessage, Handler<AsyncResult<Response>> asyncResultHandler) {
    LOGGER.error(errorMessage);
    succeededFuture()
      .map(PostDataExportExportResponse.respond400WithTextPlain(errorMessage))
      .map(Response.class::cast)
      .onComplete(asyncResultHandler);
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

}
