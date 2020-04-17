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
public class InventoryStorageClientUnitTest extends HttpServerTestBase {
  private static final int LIMIT = 20;

  @BeforeClass
  public static void beforeClass() throws Exception {
    setUpHttpServer();
    setUpMocks();
  }

  private static void setUpMocks() {
    String instancesJson = getResourceAsString(INVENTORY_RESPONSE_JSON);
    JsonObject data = new JsonObject(instancesJson);
    router.route(INSTANCE_BY_ID_URL).method(HttpMethod.GET).handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "application/json");
      response.end(data.toBuffer());
    });
  }

  @Test
  public void shouldReturnExistingInstances() {
    // given
    StorageClient storageClient = new StorageClient();
    List<String> uuids = Arrays.asList("f31a36de-fcf8-44f9-87ef-a55d06ad21ae", "3c4ae3f3-b460-4a89-a2f9-78ce3145e4fc");
    // when
    Optional<JsonObject> inventoryResponce = storageClient.getByIdsFromInventory(uuids, okapiConnectionParams, LIMIT);
    // then
    Assert.assertTrue(inventoryResponce.isPresent());
    Assert.assertEquals(2, inventoryResponce.get().getJsonArray("instances").getList().size());
  }
}
