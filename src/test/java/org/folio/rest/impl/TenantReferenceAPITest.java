package org.folio.rest.impl;

import io.vertx.core.Context;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.folio.rest.RestVerticle.MODULE_SPECIFIC_ARGS;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantReferenceAPITest extends RestVerticleTestBase {

  private static final String PARAMETER_LOAD_REFERENCE = "loadReference";
  private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
  private static final String OKAPI_HEADER_TOKEN = "x-okapi-token";
  private static final String OKAPI_HEADER_URL_TO = "X-Okapi-Url-to";
  private static final String OKAPI_HEADER_URL = "X-Okapi-Url";

  @Test
  void deleteTenantByOperationIdTest() {
    TenantReferenceAPI tenantReferenceAPI = new TenantReferenceAPI();
    String operationId = UUID.randomUUID().toString();
    Map<String, String> headers = new HashMap<>();
    Context ctx = vertx.getOrCreateContext();

    tenantReferenceAPI.deleteTenantByOperationId(operationId, headers, handler -> {
      assertTrue(handler.succeeded());
    }, ctx);
  }

  @Test
  void loadValidReferenceDataTest() {
    TenantReferenceAPI tenantReferenceAPI = new TenantReferenceAPI();
    TenantAttributes attributes = new TenantAttributes();

    Parameter testParam = new Parameter();
    testParam.setKey(PARAMETER_LOAD_REFERENCE);
    testParam.setValue(MODULE_SPECIFIC_ARGS.getOrDefault(PARAMETER_LOAD_REFERENCE, "false"));
    attributes.getParameters().add(testParam);

    Map<String, String> headers = new HashMap<>();
    headers.put(OKAPI_HEADER_URL, BASE_OKAPI_URL);
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    headers.put(OKAPI_HEADER_TOKEN, TOKEN);
    headers.put(OKAPI_HEADER_URL_TO, MOCK_OKAPI_URL);

    Context vertxContext = vertx.getOrCreateContext();

    tenantReferenceAPI.loadData(attributes, TENANT_ID, headers, vertxContext).onComplete(handler -> {
      assertTrue(handler.succeeded());
    });
  }

}
