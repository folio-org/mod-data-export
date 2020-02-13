package org.folio.service.loader;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.folio.rest.impl.HttpServerTestBase;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;

@RunWith(VertxUnitRunner.class)
public class BlockingRecordLoaderServiceTest extends HttpServerTestBase {

  @BeforeClass
  public static void setUpClass() throws Exception {
    setUpMockSRS(router);
  }

  @Test
  public void shouldReturnExistingMarcRecords() {

    RecordLoaderService rls = new BlockingRecordLoaderService(clients.getSourceRecordStorageClient());
    List<String> uuids = Arrays.asList("5fc04e92-70dd-46b8-97ea-194015762a60", "ae573875-fbc8-40e7-bda7-0ac283354226");
    SrsLoadResult srsLoadResult = rls.loadMarcRecords(uuids);
    Collection<String> actualRecords = srsLoadResult.getUnderlyingMarcRecords();

    assertThat(srsLoadResult.getUnderlyingMarcRecords(), hasSize(2));
    assertThat(srsLoadResult.getSingleInstanceIdentifiers(), hasSize(0));

    assertThat(actualRecords, hasItems(
      "812eaaa7-5d67-4c1a-a6dc-6050e6f08c92 content",
      "47178cad-a892-4c2a-b9e4-bb33dea6fc31 content"));
  }

  @Test
  public void shouldReturnNotFoundInstanceIds() {

    RecordLoaderService rls = new BlockingRecordLoaderService(clients.getSourceRecordStorageClient());
    List<String> uuids = Arrays.asList("6fc04e92-70dd-46b8-97ea-194015762a60", "be573875-fbc8-40e7-bda7-0ac283354226");
    SrsLoadResult srsLoadResult = rls.loadMarcRecords(uuids);

    assertThat(srsLoadResult.getUnderlyingMarcRecords(), hasSize(0));
    assertThat(srsLoadResult.getSingleInstanceIdentifiers(), hasSize(2));
  }

  @Test
  public void shouldReturnExistingMarcRecordsAndNotFoundIds() {

    RecordLoaderService rls = new BlockingRecordLoaderService(clients.getSourceRecordStorageClient());
    List<String> uuids = Arrays.asList("6fc04e92-70dd-46b8-97ea-194015762a60",
      "be573875-fbc8-40e7-bda7-0ac283354226", "5fc04e92-70dd-46b8-97ea-194015762a60");
    SrsLoadResult srsLoadResult = rls.loadMarcRecords(uuids);
    Collection<String> actualRecords = srsLoadResult.getUnderlyingMarcRecords();

    assertThat(srsLoadResult.getUnderlyingMarcRecords(), hasSize(1));
    assertThat(srsLoadResult.getSingleInstanceIdentifiers(), hasSize(2));
    assertThat(actualRecords, hasItem("47178cad-a892-4c2a-b9e4-bb33dea6fc31 content"));
  }

  private static void setUpMockSRS(Router router) throws IOException {
    JsonArray data;
    String json = IOUtils.toString(new FileReader("src/test/resources/marcs_db.json"));
    data = new JsonArray(json);

    router.route("/source-storage/formattedRecords/:id").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "application/json");
      String id = routingContext.pathParam("id");
      getRecord(data, id).ifPresent(record -> response.end(record.toBuffer()));
      response.setStatusCode(404);
      response.end();
    });
  }

  @NotNull
  private static Optional<JsonObject> getRecord(JsonArray data, String id) {
    for (Object object : data) {
      JsonObject datum = (JsonObject) object;
      JsonObject externalIdsHolder = datum.getJsonObject("externalIdsHolder");
      String instanceId = externalIdsHolder.getString("instanceId");
      if (instanceId.equals(id)) {
        return Optional.of(datum);
      }
    }
    return Optional.empty();
  }


}
