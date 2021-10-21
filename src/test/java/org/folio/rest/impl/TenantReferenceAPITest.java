package org.folio.rest.impl;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;

import java.util.Map;

public class TenantReferenceAPITest extends RestVerticleTestBase{

  @Override
  public Future<Integer> loadData(TenantAttributes attributes, String tenantId, Map<String, String> headers, Context vertxContext) {

    Parameter param1 = new Parameter();
    param1.setKey("loadSample");
    param1.setValue("true");

    attributes.getParameters().add(param1);
    return super.loadData(attributes, tenantId, headers, vertxContext);
  }
}
