package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ServiceBinder;
import org.folio.config.ApplicationConfig;
import org.folio.rest.resource.interfaces.InitAPI;
import org.folio.service.manager.exportmanager.ExportManager;
import org.folio.service.inputdatamanager.InputDataManager;
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
          registerExportManager(context);
          registerInputDataManager(vertx);
          handler.handle(Future.succeededFuture(true));
        } else {
          handler.handle(Future.failedFuture(result.cause()));
        }
      });
  }

  private void registerInputDataManager(Vertx vertx) {
    new ServiceBinder(vertx)
      .setAddress(InputDataManager.QUEUE_NAME)
      .register(InputDataManager.class, InputDataManager.create(vertx));
  }
  private void registerExportManager(Context context) {
    new ServiceBinder(context.owner())
      .setAddress(ExportManager.EXPORT_MANAGER_ADDRESS)
      .register(ExportManager.class, ExportManager.create(context));
  }
}
