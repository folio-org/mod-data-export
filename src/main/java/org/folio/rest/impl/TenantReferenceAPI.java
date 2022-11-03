package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantLoading;
import org.folio.rest.tools.utils.TenantTool;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

import static org.folio.rest.RestVerticle.MODULE_SPECIFIC_ARGS;

public class TenantReferenceAPI extends TenantAPI {

  private static final Logger log = LogManager.getLogger(TenantReferenceAPI.class);

  private static final String PARAMETER_LOAD_REFERENCE = "loadReference";

  @Override
  public Future<Integer> loadData(TenantAttributes attributes, String tenantId, Map<String, String> headers, Context vertxContext) {
    log.info("postTenant");
    Vertx vertx = vertxContext.owner();
    Promise<Integer> promise = Promise.promise();

    TenantLoading tl = new TenantLoading();
    buildDataLoadingParameters(attributes, tl);

    if (attributes.getParameters().stream().noneMatch(param -> param.getKey().equals(PARAMETER_LOAD_REFERENCE))) {
      Parameter newParam = new Parameter();
      newParam.setKey(PARAMETER_LOAD_REFERENCE);
      newParam.setValue(MODULE_SPECIFIC_ARGS.getOrDefault(PARAMETER_LOAD_REFERENCE, "false"));

      attributes.getParameters().add(newParam);
    }

    tl.perform(attributes, headers, vertx, res1 -> {
      if (res1.failed()) {
        promise.fail(res1.cause());
      } else {
        promise.complete(res1.result());
      }
    });

    return promise.future();
  }

  private void buildDataLoadingParameters(TenantAttributes tenantAttributes, TenantLoading tl) {
    if (isLoadReference(tenantAttributes)) {
      tl.withKey(PARAMETER_LOAD_REFERENCE).withLead("data")
        .withPostIgnore()
        .add("mapping-profiles", "data-export/mapping-profiles")
        .add("job-profiles", "data-export/job-profiles");
    }
  }

  private boolean isLoadReference(TenantAttributes tenantAttributes) {
    boolean loadReference = Boolean.parseBoolean(MODULE_SPECIFIC_ARGS.getOrDefault(PARAMETER_LOAD_REFERENCE, "false"));
    List<Parameter> parameters = tenantAttributes.getParameters();
    for (Parameter parameter : parameters) {
      if (PARAMETER_LOAD_REFERENCE.equals(parameter.getKey())) {
        loadReference = Boolean.parseBoolean(parameter.getValue());
      }
    }
    return loadReference;

  }

  @Override
  public void deleteTenantByOperationId(String operationId, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {
    log.info("deleteTenant");
    super.deleteTenantByOperationId(operationId, headers, res -> {
      Vertx vertx = ctx.owner();
      String tenantId = TenantTool.tenantId(headers);
      PostgresClient.getInstance(vertx, tenantId)
        .closeClient(event -> handler.handle(res));
    }, ctx);
  }
}
