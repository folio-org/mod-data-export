package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.service.cleanup.StorageCleanupService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.util.Map;

public class ModTenantAPI extends TenantAPI {

  private static final long DELAY_TIME_BETWEEN_CLEANUP_VALUE_MILLIS = 3600_000;

  private static final Logger logger = LoggerFactory.getLogger(ModTenantAPI.class);

  @Autowired
  private StorageCleanupService storageCleanupService;

  public ModTenantAPI() { //NOSONAR
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void postTenant(TenantAttributes entity, Map<String, String> headers, Handler<AsyncResult<Response>> handlers, Context context)  {
    super.postTenant(entity, headers, asyncResult -> {
      if (asyncResult.failed()) {
        handlers.handle(asyncResult);
      } else {
        initStorageCleanupService(headers, context);
        handlers.handle(asyncResult);
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
          logger.error("Error during cleaning file storage.", cleanupAsyncResult.cause());
        } else {
          logger.info("File storage was successfully cleaned of unused files");
        }
      });
  }

}
