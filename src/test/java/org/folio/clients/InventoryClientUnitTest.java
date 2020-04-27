package org.folio.clients;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.rest.HttpServerTestBase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.folio.TestUtil.getResourceAsString;

@RunWith(VertxUnitRunner.class)
public class InventoryClientUnitTest extends HttpServerTestBase {
  private static final String INSTANCE_BY_ID_URL = "/instance-storage/instances";
  private static final String GET_INSTANCES_RESPONSE = "clients/inventory/get_instances_response.json";
  private static final int LIMIT = 20;
  private static final String NATURE_OF_CONTENT_TERMS_URL = "/nature-of-content-terms";
  private static final String GET_NATURE_OF_CONTENT_TERMS_RESPONSE = "clients/inventory/get_nature_of_content_terms_response.json";

  @BeforeClass
  public static void beforeClass() throws Exception {
    setUpHttpServer();
    setUpMocks();
  }

  private static void setUpMocks() {
    router.route(INSTANCE_BY_ID_URL).method(HttpMethod.GET).handler(routingContext -> {
      String responseData = getResourceAsString(GET_INSTANCES_RESPONSE);
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "application/json");
      response.end(responseData);
    });
    router.route(NATURE_OF_CONTENT_TERMS_URL).method(HttpMethod.GET).handler(routingContext -> {
      String responseData = getResourceAsString(GET_NATURE_OF_CONTENT_TERMS_RESPONSE);
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "application/json");
      response.end(responseData);
    });
  }

  @Test
  public void shouldRetrieveExistingInstances() {
    // given
    InventoryClient inventoryClient = new InventoryClient();
    List<String> uuids = Arrays.asList("f31a36de-fcf8-44f9-87ef-a55d06ad21ae", "3c4ae3f3-b460-4a89-a2f9-78ce3145e4fc");
    // when
    Optional<JsonObject> inventoryResponse = inventoryClient.getInstancesByIds(uuids, okapiConnectionParams, LIMIT);
    // then
    Assert.assertTrue(inventoryResponse.isPresent());
    Assert.assertEquals(2, inventoryResponse.get().getJsonArray("instances").getList().size());
  }

  @Test
  public void shouldRetrieveNatureOfContentTerms() {
    // given
    InventoryClient inventoryClient = new InventoryClient();
    // when
    List<JsonObject> natureOfContentTerms = inventoryClient.getNatureOfContentTerms(okapiConnectionParams);
    // then
    Assert.assertFalse(natureOfContentTerms.isEmpty());
    Assert.assertEquals(2, natureOfContentTerms.size());
  }
}
