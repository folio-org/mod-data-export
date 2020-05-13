package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.resource.DataExportMappingProfiles;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.service.profiles.mapping.MappingProfileService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ExceptionToResponseMapper;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;

public class DataExportImplMappingProfilesImpl implements DataExportMappingProfiles {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final String tenantId;
  @Autowired
  private MappingProfileService mappingProfileService;

  public DataExportImplMappingProfilesImpl(Vertx vertx, String tenantId) { //NOSONAR
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
    this.tenantId = TenantTool.calculateTenantId(tenantId);
  }

  @Override
  public void postDataExportMappingProfiles(String lang, MappingProfile entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture()
      .compose(ar -> mappingProfileService.save(entity, tenantId))
      .map(mappingProfile -> (Response) PostDataExportMappingProfilesResponse.respond201WithApplicationJson(mappingProfile, PostDataExportMappingProfilesResponse.headersFor201()))
      .otherwise(ExceptionToResponseMapper::map)
      .setHandler(asyncResultHandler);
  }

  @Override
  public void getDataExportMappingProfiles(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture()
      .compose(ar -> mappingProfileService.get(query, offset, limit, tenantId))
      .map(GetDataExportMappingProfilesResponse::respond200WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .setHandler(asyncResultHandler);
  }

  @Override
  public void getDataExportMappingProfilesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture()
      .compose(ar -> mappingProfileService.getById(id, tenantId))
      .map(GetDataExportMappingProfilesByIdResponse::respond200WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .setHandler(asyncResultHandler);
  }

  @Override
  public void deleteDataExportMappingProfilesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture()
      .compose(ar -> mappingProfileService.delete(id, tenantId))
      .map(isDeleted -> Boolean.TRUE.equals(isDeleted)
        ? DeleteDataExportMappingProfilesByIdResponse.respond204()
        : DeleteDataExportMappingProfilesByIdResponse.respond404WithTextPlain(format("MappingProfile with id '%s' was not found", id)))
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .setHandler(asyncResultHandler);
  }

  @Override
  public void putDataExportMappingProfilesById(String id, String lang, MappingProfile entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture()
      .compose(ar -> mappingProfileService.update(entity, tenantId))
      .map(PutDataExportMappingProfilesByIdResponse.respond204())
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .setHandler(asyncResultHandler);
  }
}
