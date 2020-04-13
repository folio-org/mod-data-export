package org.folio.clients;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.folio.rest.HttpServerTestBase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

@RunWith(MockitoJUnitRunner.class)
public class InventoryStorageClientUnitTest extends HttpServerTestBase {
  private static final int LIMIT = 20;

  @Spy
  StorageClient client;

  @Test
  public void shouldReturnExistingInstances() throws IOException {
    // given
    String instancesJson = IOUtils.toString(new FileReader("src/test/resources/inventoryStorageResponse.json"));
    JsonObject data = new JsonObject(instancesJson);
    WireMock.stubFor(get(urlPathMatching("/instance-storage/instances"))
      .willReturn(WireMock.ok().withBody(Json.encode(data))));
    List<String> uuids = Arrays.asList("f31a36de-fcf8-44f9-87ef-a55d06ad21ae", "3c4ae3f3-b460-4a89-a2f9-78ce3145e4fc");
    // when
    Optional<JsonObject> inventoryResponse = client.getByIdsFromInventory(uuids, okapiConnectionParams, LIMIT);
    // then
    Assert.assertTrue(inventoryResponse.isPresent());
    Assert.assertEquals(2, inventoryResponse.get().getJsonArray("instances").getList().size());
  }
}
