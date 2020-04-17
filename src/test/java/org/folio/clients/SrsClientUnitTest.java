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
public class SrsClientUnitTest extends HttpServerTestBase {
  private static final int LIMIT = 20;

  @BeforeClass
  public static void beforeClass() throws Exception {
    setUpHttpServer();
    setUpMocks();
  }

  private static void setUpMocks() {
    String json = getResourceAsString(SRS_RESPONSE_JSON);
    JsonObject data = new JsonObject(json);
    router.route(RECORDS_BY_ID_URL).method(HttpMethod.GET).handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "application/json");
      response.end(data.toBuffer());
    });
  }

  @Test
  public void shouldReturnExistingMarcRecords() {
    // given
    StorageClient storageClient = new StorageClient();
    List<String> uuids = Arrays.asList("6fc04e92-70dd-46b8-97ea-194015762a61", "be573875-fbc8-40e7-bda7-0ac283354227");
    // when
    Optional<JsonObject> srsResponce = storageClient.getByIdsFromSRS(uuids, okapiConnectionParams, LIMIT);
    // then
    Assert.assertTrue(srsResponce.isPresent());
    Assert.assertEquals(2, srsResponce.get().getJsonArray("records").getList().size());
  }
}
