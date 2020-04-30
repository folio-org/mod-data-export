package org.folio.clients;

import static org.folio.util.ExternalPathResolver.SRS;
import static org.folio.util.ExternalPathResolver.resourcesPath;

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
public class SourceRecordStorageUnitTest extends HttpServerTestBase {
  protected static final String SRS_RESPONSE_JSON = "clients/srs/get_records_response.json";
  private static final int LIMIT = 20;

  @BeforeClass
  public static void beforeClass() throws Exception {
    setUpHttpServer();
    setUpMocks();
  }

  private static void setUpMocks() {
    String json = getResourceAsString(SRS_RESPONSE_JSON);
    JsonObject data = new JsonObject(json);
    router.route(resourcesPath(SRS)).method(HttpMethod.GET).handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "application/json");
      response.end(data.toBuffer());
    });
  }

  @Test
  public void shouldReturnExistingMarcRecords() {
    // given
    SourceRecordStorageClient srsClient = new SourceRecordStorageClient();
    List<String> uuids = Arrays.asList("6fc04e92-70dd-46b8-97ea-194015762a61", "be573875-fbc8-40e7-bda7-0ac283354227");
    // when
    Optional<JsonObject> srsResponse = srsClient.getRecordsByIds(uuids, okapiConnectionParams, LIMIT);
    // then
    Assert.assertTrue(srsResponse.isPresent());
    Assert.assertEquals(2, srsResponse.get().getJsonArray("records").getList().size());
  }
}
