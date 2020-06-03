package org.folio.clients;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.util.ExternalPathResolver.CONTENT_TERMS;
import static org.folio.util.ExternalPathResolver.INSTANCE;
import static org.folio.util.ExternalPathResolver.resourcesPath;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.rest.RestVerticleTestBase;
import org.folio.util.OkapiConnectionParams;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
class InventoryClientUnitTest extends RestVerticleTestBase {
  private static final int LIMIT = 20;
  private static OkapiConnectionParams okapiConnectionParams;

  @BeforeAll
  public static void beforeClass() throws Exception {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    headers.put(OKAPI_HEADER_URL, MOCK_OKAPI_URL);
    okapiConnectionParams = new OkapiConnectionParams(headers);
  }


  @Test
  void shouldRetrieveExistingInstances() {
    // given
    InventoryClient inventoryClient = new InventoryClient();
    List<String> uuids = Arrays.asList("7fbd5d84-62d1-44c6-9c45-6cb173998bbd", "3c4ae3f3-b460-4a89-a2f9-78ce3145e4fc");
    // when
    Optional<JsonObject> inventoryResponse = inventoryClient.getInstancesByIds(uuids, okapiConnectionParams, LIMIT);
    // then
    Assert.assertTrue(inventoryResponse.isPresent());
    Assert.assertEquals(1, inventoryResponse.get().getJsonArray("instances").getList().size());
  }

  @Test
  void shouldRetrieveNatureOfContentTerms() {
    // given
    InventoryClient inventoryClient = new InventoryClient();
    // when
    Map<String, JsonObject> natureOfContentTerms = inventoryClient.getNatureOfContentTerms(okapiConnectionParams);
    // then
    Assert.assertFalse(natureOfContentTerms.isEmpty());
    Assert.assertEquals(2, natureOfContentTerms.size());
  }

  @Test
  void shouldRetrieveLocations() {
    // given
    InventoryClient inventoryClient = new InventoryClient();
    // when
    Map<String, JsonObject> locations = inventoryClient.getLocations(okapiConnectionParams);
    // then
    Assert.assertFalse(locations.isEmpty());
    Assert.assertEquals(2, locations.size());
  }

  @Test
  void shouldRetrieveExistingHoldings() {
    // given
    InventoryClient inventoryClient = new InventoryClient();
    String instanceID = "7fbd5d84-62d1-44c6-9c45-6cb173998bbd";
    // when
    Optional<JsonObject> holdingsResponse = inventoryClient.getHoldingsByInstanceId(instanceID, okapiConnectionParams);
    // then
    Assert.assertTrue(holdingsResponse.isPresent());
    Assert.assertEquals(2, holdingsResponse.get().getJsonArray("holdingsRecords").getList().size());
  }

  @Test
  void shouldRetrieveExistingItems() {
    // given
    InventoryClient inventoryClient = new InventoryClient();
    List<String> holdingIDs = Arrays.asList("65cb2bf0-d4c2-4886-8ad0-b76f1ba75d61", "65cb2bf0-d4c2-4886-8ad0-b76f1ba75d61");
    // when
    Optional<JsonObject> itemsResponse = inventoryClient.getItemsByHoldingIds(holdingIDs, okapiConnectionParams);
    // then
    Assert.assertTrue(itemsResponse.isPresent());
    Assert.assertEquals(2, itemsResponse.get().getJsonArray("items").getList().size());
  }
}
