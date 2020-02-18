package org.folio.service.loader;


import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import org.apache.commons.io.IOUtils;
import org.folio.rest.impl.HttpServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

@RunWith(VertxUnitRunner.class)
public class BlockingRecordLoaderServiceTest extends HttpServerTestBase {

  @BeforeClass
  public static void setUpClass() throws Exception {
    setUpMockSRS(router);
  }

  @Test
  public void shouldReturnExistingMarcRecords() {

    RecordLoaderService rls = new BlockingRecordLoaderService(clients.getSourceRecordStorageClient());
    List<String> uuids = Arrays.asList("5fc04e92-70dd-46b8-97ea-194015762a60", "ae573875-fbc8-40e7-bda7-0ac283354226",
      "be573875-fbc8-40e7-bda7-0ac283354227");
    SrsLoadResult srsLoadResult = rls.loadMarcRecords(uuids);
    Collection<String> actualRecords = srsLoadResult.getUnderlyingMarcRecords();

    assertThat(actualRecords, hasSize(2));
    assertThat(srsLoadResult.getSingleInstanceIdentifiers(), hasSize(1));

    assertThat(actualRecords, hasItems(
      "812eaaa7-5d67-4c1a-a6dc-6050e6f08c92 content",
      "47178cad-a892-4c2a-b9e4-bb33dea6fc31 content"));
  }

  @Test
  public void shouldReturnNotFoundInstanceIds() {

    RecordLoaderService rls = new BlockingRecordLoaderService(clients.getSourceRecordStorageClient());
    List<String> uuids = Arrays.asList("6fc04e92-70dd-46b8-97ea-194015762a61", "be573875-fbc8-40e7-bda7-0ac283354227");
    SrsLoadResult srsLoadResult = rls.loadMarcRecords(uuids);

    assertThat(srsLoadResult.getSingleInstanceIdentifiers(), hasSize(2));
  }

  private static void setUpMockSRS(Router router) throws IOException {
    String json = IOUtils.toString(new FileReader("src/test/resources/marcs_db.json"));
    JsonObject data = new JsonObject(json);

    router.route("/source-storage/records").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "application/json");
      response.end(data.toBuffer());
    });
  }
}
