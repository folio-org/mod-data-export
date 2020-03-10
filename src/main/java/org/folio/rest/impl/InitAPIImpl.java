package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ServiceBinder;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import org.folio.config.ApplicationConfig;
import org.folio.rest.resource.interfaces.InitAPI;
import org.folio.service.manager.exportmanager.ExportManager;
import org.folio.service.manager.inputdatamanager.InputDataManager;
import org.folio.spring.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitAPIImpl implements InitAPI {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


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
          initializeSystemProperties(handler);
          handler.handle(Future.succeededFuture(true));
        } else {
          handler.handle(Future.failedFuture(result.cause()));
        }
      });
  }

  private void initializeSystemProperties(Handler<AsyncResult<Boolean>> handler) {
    String configPath = System.getProperty("configPath");
    if (configPath != null) {
      try (InputStream configFile = InitAPIImpl.class.getClassLoader()
        .getResourceAsStream(configPath)) {
        if (configFile == null) {
          throw new IllegalStateException(String.format("The config file %s is missing.", configPath));
        }
        final Properties confProperties = new Properties();
        confProperties.load(configFile);

        Properties sysProps = System.getProperties();
        confProperties.forEach(sysProps::putIfAbsent);
      } catch (Exception e) {
        handler.handle(Future.failedFuture(e));
        LOGGER.error("Unable to populate system properties", e);
      }
    }
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
