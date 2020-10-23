package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.spring.SpringContextUtil;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Objects;

import static io.vertx.core.Future.succeededFuture;

public class ModTenantAPI extends TenantAPI {
  private static final Logger LOGGER = LoggerFactory.getLogger(ModTenantAPI.class);


  public ModTenantAPI() { //NOSONAR
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void postTenant(TenantAttributes entity, Map<String, String> headers, Handler<AsyncResult<Response>> handlers, Context context) {
    super.postTenant(entity, headers, asyncResult -> {
      if (asyncResult.failed()) {
        if (Objects.nonNull(asyncResult.cause())) {
          LOGGER.error("Post tenant is failed, cause: " + asyncResult.cause().getMessage());
        }
        handlers.handle(asyncResult);
      } else {
        LOGGER.info("Post tenant is complete successfully");
        handlers.handle(succeededFuture(PostTenantResponse.respond201WithApplicationJson("Post tenant is complete")));
      }
    }, context);
  }

}
