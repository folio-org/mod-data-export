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
public class SrsClientUnitTest extends HttpServerTestBase {
  private static final int LIMIT = 20;

  @Spy
  StorageClient client;

  @Test
  public void shouldReturnExistingMarcRecords() throws IOException {
    // given
    String instancesJson = IOUtils.toString(new FileReader("src/test/resources/srsResponse.json"));
    JsonObject data = new JsonObject(instancesJson);
    WireMock.stubFor(get(urlPathMatching("/source-storage/records"))
      .willReturn(WireMock.ok().withBody(Json.encode(data))));
    List<String> uuids = Arrays.asList("6fc04e92-70dd-46b8-97ea-194015762a61", "be573875-fbc8-40e7-bda7-0ac283354227");
    // when
    Optional<JsonObject> srsResponce = client.getByIdsFromSRS(uuids, okapiConnectionParams, LIMIT);
    // then
    Assert.assertTrue(srsResponce.isPresent());
    Assert.assertEquals(2, srsResponce.get().getJsonArray("records").getList().size());
  }
}
