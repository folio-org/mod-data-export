package org.folio.clients;

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
public class SrsClientUnitTest extends HttpServerTestBase {
  private static final int LIMIT = 20;

  @Spy
  StorageClient client;

  @BeforeClass
  public static void beforeClass() throws Exception {
    setUpHttpServer();
    setUpMocks();
  }

  private static void setUpMocks() throws IOException {
    String json = IOUtils.toString(new FileReader("src/test/resources/srsResponse.json"));
    JsonObject data = new JsonObject(json);
    httpServer.requestHandler(request -> {
      request.response().putHeader("content-type", "application/json");
      if (request.path().contains("/source-storage/records")) {
        request.response().end(data.toBuffer());
      }
    }).listen(port);
  }

  @Test
  public void shouldReturnExistingMarcRecords() throws IOException {
    // given
    List<String> uuids = Arrays.asList("6fc04e92-70dd-46b8-97ea-194015762a61", "be573875-fbc8-40e7-bda7-0ac283354227");
    // when
    Optional<JsonObject> srsResponce = client.getByIdsFromSRS(uuids, okapiConnectionParams, LIMIT);
    // then
    Assert.assertTrue(srsResponce.isPresent());
    Assert.assertEquals(2, srsResponce.get().getJsonArray("records").getList().size());
  }
}
