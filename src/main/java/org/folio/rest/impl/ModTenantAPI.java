package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.tools.utils.TenantLoading;
import org.folio.service.file.cleanup.StorageCleanupService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.util.Map;

public class ModTenantAPI extends TenantAPI {
  private static final Logger LOGGER = LoggerFactory.getLogger(ModTenantAPI.class);

  private static final long DELAY_TIME_BETWEEN_CLEANUP_VALUE_MILLIS = 3600_000;
  private static final String LOAD_DEFAULT_DATA_PARAMETER = "loadDefaultData";
  private static final String DATA = "data";
  private static final String MAPPING_PROFILES = "mappingProfiles";
  private static final String JOB_PROFILES = "jobProfiles";
  private static final String MAPPING_PROFILES_URI = "data-export/mappingProfiles";
  private static final String JOB_PROFILES_URI = "data-export/jobProfiles";

  @Autowired
  private StorageCleanupService storageCleanupService;

  public ModTenantAPI() { //NOSONAR
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void postTenant(TenantAttributes entity, Map<String, String> headers, Handler<AsyncResult<Response>> handlers, Context context) {
    super.postTenant(entity, headers, asyncResult -> {
      if (asyncResult.failed()) {
        handlers.handle(asyncResult);
      } else {
        initStorageCleanupService(headers, context);
        initDefaultData(entity, headers, handlers, context);
      }
    }, context);
  }

  private void initStorageCleanupService(Map<String, String> headers, Context context) {
    Vertx vertx = context.owner();
    OkapiConnectionParams params = new OkapiConnectionParams(headers);
    vertx.setPeriodic(DELAY_TIME_BETWEEN_CLEANUP_VALUE_MILLIS, periodicHandler -> executeStorageCleanUpJob(vertx, params));
  }

  private void executeStorageCleanUpJob(Vertx vertx, OkapiConnectionParams params) {
    vertx.<Void>executeBlocking(blockingCodeHandler -> storageCleanupService.cleanStorage(params),
      cleanupAsyncResult -> {
        if (cleanupAsyncResult.failed()) {
          LOGGER.error("Error during cleaning file storage.", cleanupAsyncResult.cause().getMessage());
        } else {
          LOGGER.info("File storage was successfully cleaned of unused files");
        }
      });
  }

  private void initDefaultData(TenantAttributes entity, Map<String, String> headers, Handler<AsyncResult<Response>> handlers, Context context) {
    TenantLoading tenantLoading = new TenantLoading();
    Parameter parameter = new Parameter().withKey(LOAD_DEFAULT_DATA_PARAMETER).withValue("true");
    entity.getParameters().add(parameter);
    buildDataLoadingParameters(tenantLoading);
    tenantLoading.perform(entity, headers, context.owner(), response -> {
      if (response.failed()) {
        handlers.handle(io.vertx.core.Future.succeededFuture(PostTenantResponse
          .respond500WithTextPlain(response.cause().getLocalizedMessage())));
        return;
      }
      handlers.handle(io.vertx.core.Future.succeededFuture(PostTenantResponse
        .respond201WithApplicationJson("")));
    });
  }

  private void buildDataLoadingParameters(TenantLoading tenantLoading) {
    tenantLoading.withKey(LOAD_DEFAULT_DATA_PARAMETER)
      .withLead(DATA)
      .add(MAPPING_PROFILES, MAPPING_PROFILES_URI)
      .add(JOB_PROFILES, JOB_PROFILES_URI);
  }

}
