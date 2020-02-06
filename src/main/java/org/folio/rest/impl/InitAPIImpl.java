package org.folio.rest.impl;

import io.vertx.core.*;
import io.vertx.serviceproxy.ServiceBinder;
import org.folio.config.ApplicationConfig;
import org.folio.rest.resource.interfaces.InitAPI;
import org.folio.service.manager.ExportManager;
import org.folio.spring.SpringContextUtil;

public class InitAPIImpl implements InitAPI {

  @Override
  public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> handler) {
    vertx.executeBlocking(
      future -> {
        SpringContextUtil.init(vertx, context, ApplicationConfig.class);
        future.complete();
      },
      result -> {
        if (result.succeeded()) {
          registerExportManager(vertx);
          handler.handle(Future.succeededFuture(true));
        } else {
          handler.handle(Future.failedFuture(result.cause()));
        }
      });
  }

  private void registerExportManager(Vertx vertx) {
    new ServiceBinder(vertx)
      .setAddress(ExportManager.EXPORT_MANAGER_ADDRESS)
      .register(ExportManager.class, ExportManager.create(vertx));
  }
}
