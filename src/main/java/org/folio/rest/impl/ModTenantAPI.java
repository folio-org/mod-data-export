package org.folio.rest.impl;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.service.cleanup.StorageCleanupService;
import org.folio.service.storage.aws.AwsService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ModTenantAPI extends TenantAPI {

  private static final long DELAY_TIME_BETWEEN_CLEANUP_VALUE_MILLIS = 3600_000;

  private static final Logger LOGGER = LoggerFactory.getLogger(ModTenantAPI.class);

  @Autowired
  private StorageCleanupService storageCleanupService;
  @Autowired
  AwsService awsService;

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
        context.owner().executeBlocking(blockingFuture -> {
          initStorageCleanupService(headers, context);
          String tenantId = TenantTool.calculateTenantId(headers.get(OKAPI_HEADER_TENANT));
          final Future<Void> future = initializeS3BucketForTenant(tenantId);
          future.setHandler(ar -> blockingFuture.complete(future));
        }, ar -> {
          if (ar.failed()) {
            LOGGER.error("Error during module initialization.", ar.cause());
          } else {
            LOGGER.info("Module initialization completed");
          }
          handlers.handle(asyncResult);
        });
      }
    }, context);
  }

  private Future<Void> initializeS3BucketForTenant(String tenantId) {
    return awsService.setUpS33BucketForTenant(tenantId);
  }

  private void initStorageCleanupService(Map<String, String> headers, Context context) {
    Vertx vertx = context.owner();
    OkapiConnectionParams params = new OkapiConnectionParams(headers);
    vertx.setPeriodic(DELAY_TIME_BETWEEN_CLEANUP_VALUE_MILLIS, periodicHandler -> storageCleanupService.cleanStorage(params));
  }
}
