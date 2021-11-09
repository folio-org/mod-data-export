package org.folio.rest.impl;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;

import io.vertx.core.Context;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.clients.SearchClient;
import org.folio.config.ApplicationConfig;
import org.folio.spring.SpringContextUtil;
import org.folio.util.OkapiConnectionParams;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
@RunWith(VertxUnitRunner.class)
public class SearchClientTest extends RestVerticleTestBase {
  private static OkapiConnectionParams okapiConnectionParams;

  @Autowired
  private SearchClient searchClient;

  @BeforeAll
  void beforeClass() {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    headers.put(OKAPI_HEADER_URL, MOCK_OKAPI_URL);
    okapiConnectionParams = new OkapiConnectionParams(headers);
    Context context = vertx.getOrCreateContext();
    SpringContextUtil.init(vertx, context, ApplicationConfig.class);
    SpringContextUtil.autowireDependencies(this, context);
  }

  @Test
  void shouldRetrieveInstanceBulkUUIDS(VertxTestContext testContext) {
    // given
    String query = "cql query";
    // when
    searchClient.getInstancesBulkUUIDsAsync(query, okapiConnectionParams).onSuccess(inventoryResponse -> {
      //then
      Assert.assertTrue(inventoryResponse.isPresent());
      Assert.assertEquals(2, inventoryResponse.get().getJsonArray("ids").getList().size());
      testContext.completeNow();
    }).onFailure(testContext::failNow);
  }

  @ParameterizedTest
  @ValueSource(strings = {"inventory 500", "empty json response", "invalid json returned"})
  void shouldReturnEmptyOptional_whenRequestInstanceBulkUUIDsAndInvalidResponseReturned(VertxTestContext testContext, String query) {
    // given query parameter
    // when
    searchClient.getInstancesBulkUUIDsAsync(query, okapiConnectionParams).onSuccess(inventoryResponse -> {
      //then
      Assert.assertTrue(inventoryResponse.isEmpty());
      testContext.completeNow();
    }).onFailure(testContext::failNow);
  }
}
