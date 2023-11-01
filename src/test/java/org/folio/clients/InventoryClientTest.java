package org.folio.clients;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.rest.impl.RestVerticleTestBase;
import org.folio.service.ApplicationTestConfig;
import org.folio.spring.SpringContextUtil;
import org.folio.util.OkapiConnectionParams;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.impl.StorageTestSuite.mockPort;
import static org.junit.jupiter.api.Assertions.assertNull;

class InventoryClientTest extends RestVerticleTestBase {

  private static OkapiConnectionParams okapiConnectionParams;

  @Autowired
  private InventoryClient inventoryClient;

  public InventoryClientTest() {
    Context vertxContext = Vertx.vertx().getOrCreateContext();
    SpringContextUtil.init(vertxContext.owner(), vertxContext, ApplicationTestConfig.class);
    SpringContextUtil.autowireDependencies(this, vertxContext);
  }

  @BeforeAll
  public static void beforeClass() {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    headers.put("x-okapi-url", "http://localhost:" + mockPort);
    okapiConnectionParams = new OkapiConnectionParams(headers);
  }

  @Test
  void testGetInstanceByIdNotFound() {
    var instanceId = UUID.randomUUID().toString();
    var jobExecutionId = UUID.randomUUID().toString();
    var actual = inventoryClient.getInstanceById(jobExecutionId, instanceId, okapiConnectionParams);
    assertNull(actual);
  }

}
