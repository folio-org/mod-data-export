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
import org.folio.service.manager.inputdatamanager.InputDataManager;
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
          registerProxies(context);
          handler.handle(Future.succeededFuture(true));
        } else {
          handler.handle(Future.failedFuture(result.cause()));
        }
      });
  }

  private void registerProxies(Context context) {
    InputDataManager inputDataManagerProxy = InputDataManager.createProxy(context.owner());
    ExportManager exportManagerProxy = ExportManager.createProxy(context.owner());
    context.put(InputDataManager.class.getName(), inputDataManagerProxy);
    context.put(ExportManager.class.getName(), exportManagerProxy);

    new ServiceBinder(context.owner())
      .setAddress(ExportManager.EXPORT_MANAGER_ADDRESS)
      .register(ExportManager.class,  ExportManager.create(context));
    new ServiceBinder(context.owner())
      .setAddress(InputDataManager.INPUT_DATA_MANAGER_ADDRESS)
      .register(InputDataManager.class,  InputDataManager.create(context));
  }
}
