package org.folio.rest.impl;

import org.folio.config.ApplicationConfig;
import org.folio.rest.resource.interfaces.InitAPI;
import org.folio.service.manager.exportmanager.ExportManager;
import org.folio.service.inputdatamanager.InputDataManager;
import org.folio.spring.SpringContextUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ServiceBinder;

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
          registerProxies(context);
          handler.handle(Future.succeededFuture(true));
        } else {
          handler.handle(Future.failedFuture(result.cause()));
        }
      });
  }

  private void registerProxies(Context context) {
    InputDataManager inputDataManager = InputDataManager.create(context.owner());
    new ServiceBinder(context.owner())
      .setAddress(InputDataManager.QUEUE_NAME)
      .register(InputDataManager.class, inputDataManager);
    context.put(InputDataManager.class.getName(), inputDataManager);

    ExportManager exportManager = ExportManager.create(context.owner());
    new ServiceBinder(context.owner())
      .setAddress(ExportManager.QUEUE_NAME)
      .register(ExportManager.class, exportManager);
    context.put(ExportManager.class.getName(), exportManager);

  }
}
