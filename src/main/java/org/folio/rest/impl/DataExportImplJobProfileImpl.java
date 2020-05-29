package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.jaxrs.resource.DataExportJobProfiles;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.service.profiles.jobprofile.JobProfileService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ExceptionToResponseMapper;
import org.springframework.beans.factory.annotation.Autowired;

public class DataExportImplJobProfileImpl implements DataExportJobProfiles {

  @Autowired
  private JobProfileService jobProfileService;

  private String tenantId;
  private static final String JOBPROFILE_LOCATION_PREFIX = "/data-export/jobProfiles/%s";

  public DataExportImplJobProfileImpl(Vertx vertx, String tenantId) { //NOSONAR
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
    this.tenantId = TenantTool.calculateTenantId(tenantId);
  }

  @Override
  public void postDataExportJobProfiles(String lang, JobProfile entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture().compose(ar -> jobProfileService.save(entity, tenantId))
      .map(jobProfileRes -> PostDataExportJobProfilesResponse.respond201WithApplicationJson(jobProfileRes,
          PostDataExportJobProfilesResponse.headersFor201()
            .withLocation(JOBPROFILE_LOCATION_PREFIX)))
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);
  }

  @Override
  public void getDataExportJobProfilesById(String fileDefinitionId, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture().compose(ar -> jobProfileService.getById(fileDefinitionId, tenantId))
      .map(GetDataExportJobProfilesByIdResponse::respond200WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);

  }

  @Override
  public void deleteDataExportJobProfilesById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture().compose(ar -> jobProfileService.deleteById(id, tenantId))
    .map(isDeleted -> Boolean.TRUE.equals(isDeleted)
        ? DeleteDataExportJobProfilesByIdResponse.respond204()
        : DeleteDataExportJobProfilesByIdResponse.respond404WithTextPlain(format("JobProfile with id '%s' was not found", id)))
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);

  }

  @Override
  public void putDataExportJobProfilesById(String id, String lang, JobProfile entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture().compose(ar -> jobProfileService.update(entity, tenantId))
      .map(updated -> PutDataExportJobProfilesByIdResponse.respond204())
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);

  }

  @Override
  public void getDataExportJobProfiles(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture().compose(ar -> jobProfileService.get(query, offset, limit, tenantId))
      .map(GetDataExportJobProfilesResponse::respond200WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);

  }
}
