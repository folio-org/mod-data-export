package org.folio.rest.impl;

import io.vertx.core.*;
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
    ExportManager exportManager = ExportManager.create(context);
    new ServiceBinder(context.owner())
      .setAddress(ExportManager.EXPORT_MANAGER_ADDRESS)
      .register(ExportManager.class, exportManager);
    context.put(ExportManager.class.getName(), exportManager);

    InputDataManager inputDataManager = InputDataManager.create(context);
    new ServiceBinder(context.owner())
      .setAddress(InputDataManager.INPUT_DATA_MANAGER_ADDRESS)
      .register(InputDataManager.class, inputDataManager);
    context.put(InputDataManager.class.getName(), inputDataManager);
  }
}
