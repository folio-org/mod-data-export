package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.resource.DataExportMappingProfiles;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.service.profiles.mappingprofile.MappingProfileService;
import org.folio.service.transformationfields.TransformationFieldsService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ExceptionToResponseMapper;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.util.Map;

public class DataExportImplMappingProfilesImpl implements DataExportMappingProfiles {
  private final String tenantId;
  @Autowired
  private MappingProfileService mappingProfileService;
  @Autowired
  private TransformationFieldsService transformationFieldsService;

  public DataExportImplMappingProfilesImpl(Vertx vertx, String tenantId) { //NOSONAR
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
    this.tenantId = TenantTool.calculateTenantId(tenantId);
  }

  @Override
  public void postDataExportMappingProfiles(String lang, MappingProfile entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    OkapiConnectionParams params = new OkapiConnectionParams(okapiHeaders);
    succeededFuture()
      .compose(ar -> mappingProfileService.validate(entity, params))
      .compose(ar -> mappingProfileService.save(entity, params))
      .map(mappingProfile -> (Response) PostDataExportMappingProfilesResponse.respond201WithApplicationJson(mappingProfile, PostDataExportMappingProfilesResponse.headersFor201()))
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);
  }

  @Override
  public void getDataExportMappingProfiles(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture()
      .compose(ar -> mappingProfileService.get(query, offset, limit, tenantId))
      .map(GetDataExportMappingProfilesResponse::respond200WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);
  }

  @Override
  public void getDataExportMappingProfilesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture()
      .compose(ar -> mappingProfileService.getById(id, new OkapiConnectionParams(okapiHeaders)))
      .map(GetDataExportMappingProfilesByIdResponse::respond200WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);
  }

  @Override
  public void deleteDataExportMappingProfilesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture()
      .compose(ar -> mappingProfileService.deleteById(id, tenantId))
      .map(isDeleted -> Boolean.TRUE.equals(isDeleted)
        ? DeleteDataExportMappingProfilesByIdResponse.respond204()
        : DeleteDataExportMappingProfilesByIdResponse.respond404WithTextPlain(format("MappingProfile with id '%s' was not found", id)))
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);
  }

  @Override
  public void putDataExportMappingProfilesById(String id, String lang, MappingProfile entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    OkapiConnectionParams params = new OkapiConnectionParams(okapiHeaders);
    succeededFuture()
      .compose(ar -> mappingProfileService.validate(entity, params))
      .compose(ar -> mappingProfileService.update(entity, params))
      .map(PutDataExportMappingProfilesByIdResponse.respond204())
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);
  }
}
