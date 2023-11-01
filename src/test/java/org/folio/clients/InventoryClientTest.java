package org.folio.clients;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.rest.impl.RestVerticleTestBase;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.service.ApplicationTestConfig;
import org.folio.service.logs.ErrorLogService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.OkapiConnectionParams;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.impl.StorageTestSuite.mockPort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class InventoryClientTest extends RestVerticleTestBase {

  private static OkapiConnectionParams okapiConnectionParams;

  @Spy
  @InjectMocks
  private InventoryClient inventoryClient;

  @Spy
  private ErrorLogService errorLogService;

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
  void testGetInstanceById() {
    var instanceId = UUID.randomUUID().toString();
    var jobExecutionId = UUID.randomUUID().toString();
    var expected = new JsonObject().put("id", instanceId);
    when(errorLogService.saveGeneralErrorWithMessageValues(any(String.class), any(List.class), any(String.class), any(String.class)))
        .thenReturn(Future.succeededFuture(new ErrorLog()));
    when(inventoryClient.getInstanceById(jobExecutionId, instanceId, okapiConnectionParams)).thenReturn(expected);
    var actual = inventoryClient.getInstanceById(jobExecutionId, instanceId, okapiConnectionParams);
    assertEquals(expected, actual);
  }

}
