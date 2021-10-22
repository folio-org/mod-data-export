package org.folio.rest.impl;

import io.vertx.core.Context;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantReferenceAPITest extends RestVerticleTestBase {

  @Test
  void deleteTenantByOperationIdTest() {
    TenantReferenceAPI tenantReferenceAPI = new TenantReferenceAPI();
    String operationId = UUID.randomUUID().toString();
    Map<String, String> headers = new HashMap<>();
    Context ctx = vertx.getOrCreateContext();

    tenantReferenceAPI.deleteTenantByOperationId(operationId, headers, handler -> {
      assertTrue(handler.succeeded());
    },ctx);
  }

  @Test
  void loadInvalidReferenceDataTest() {
    TenantReferenceAPI tenantReferenceAPI = new TenantReferenceAPI();
    TenantAttributes attributes = new TenantAttributes();
    Map<String, String> headers = new HashMap<>();
    Context vertxContext = vertx.getOrCreateContext();

    assertTrue(tenantReferenceAPI.loadData(attributes, TENANT_ID, headers, vertxContext).failed());
  }

}
