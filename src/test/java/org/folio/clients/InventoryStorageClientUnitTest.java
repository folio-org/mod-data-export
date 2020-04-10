package org.folio.clients;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.folio.rest.HttpServerTestBase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class InventoryStorageClientUnitTest extends HttpServerTestBase {
  private static final int LIMIT = 20;

  @Spy
  StorageClient client;

  @BeforeClass
  public static void beforeClass() throws Exception {
    setUpHttpServer();
    setUpMocks();
  }

  private static void setUpMocks() throws IOException {
    String instancesJson = IOUtils.toString(new FileReader("src/test/resources/inventoryStorageResponse.json"));
    JsonObject instanceData = new JsonObject(instancesJson);
    router.route("/instance-storage/instances").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "application/json");
      response.end(instanceData.toBuffer());
    });
  }

  @Test
  public void shouldReturnExistingInstances() throws IOException {
    // given
    List<String> uuids = Arrays.asList("f31a36de-fcf8-44f9-87ef-a55d06ad21ae", "3c4ae3f3-b460-4a89-a2f9-78ce3145e4fc");
    // when
    Optional<JsonObject> inventoryResponce = client.getByIdsFromInventory(uuids, okapiConnectionParams, LIMIT);
    // then
    Assert.assertTrue(inventoryResponce.isPresent());
    Assert.assertEquals(2, inventoryResponce.get().getJsonArray("instances").getList().size());
  }
}
