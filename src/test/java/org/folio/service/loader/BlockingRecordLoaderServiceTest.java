package org.folio.service.loader;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.IOUtils;
import org.folio.clients.impl.SourceRecordStorageClient;
import org.folio.rest.HttpServerTestBase;
import org.folio.util.OkapiConnectionParams;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

@RunWith(VertxUnitRunner.class)
public class BlockingRecordLoaderServiceTest extends HttpServerTestBase {
  @BeforeClass
  public static void beforeClass() throws Exception {
    setUpHttpServer();
    setUpMocks();
  }

  private static void setUpMocks() throws IOException {
    String json = IOUtils.toString(new FileReader("src/test/resources/marc_records.json"));
    JsonObject data = new JsonObject(json);
    router.route("/source-storage/records").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "application/json");
      response.end(data.toBuffer());
    });
  }

  @Test
  public void shouldReturnExistingMarcRecords() {
    // given
    SourceRecordStorageClient client = Mockito.spy(SourceRecordStorageClient.class);
    RecordLoaderService recordLoaderService = new BlockingRecordLoaderService(client);
    // when
    SrsLoadResult srsLoadResult = recordLoaderService.loadMarcRecords(new ArrayList<>(), okapiConnectionParams);
    // then
    assertThat(srsLoadResult.getUnderlyingMarcRecords(), hasSize(2));
    assertThat(srsLoadResult.getUnderlyingMarcRecords(), hasItems("812eaaa7-5d67-4c1a-a6dc-6050e6f08c92 content", "47178cad-a892-4c2a-b9e4-bb33dea6fc31 content"));
  }

  @Test
  public void shouldReturnNotFoundInstanceIds() {
    // given
    List<String> uuids = Arrays.asList("6fc04e92-70dd-46b8-97ea-194015762a61", "be573875-fbc8-40e7-bda7-0ac283354227");
    JsonObject emptyResponse = new JsonObject().put("records", new JsonArray());
    SourceRecordStorageClient client = Mockito.mock(SourceRecordStorageClient.class);
    Mockito.when(client.getByIds(anyList(), any(OkapiConnectionParams.class))).thenReturn(Optional.of(emptyResponse));
    RecordLoaderService recordLoaderService = new BlockingRecordLoaderService(client);
    // when
    SrsLoadResult srsLoadResult = recordLoaderService.loadMarcRecords(uuids, okapiConnectionParams);
    // then
    assertThat(srsLoadResult.getInstanceIdsWithoutSrs(), hasSize(2));
    assertThat(srsLoadResult.getUnderlyingMarcRecords(), empty());
  }

  @Test
  public void loadInstanceRecords_doesNotThrowAnyException() {
    // given
    List<String> uuids = new ArrayList<>();
    SourceRecordStorageClient client = Mockito.spy(SourceRecordStorageClient.class);
    RecordLoaderService recordLoaderService = new BlockingRecordLoaderService(client);
    // when call loadInventoryInstances method, then assert no exception thrown
    assertThatCode(() -> recordLoaderService.loadInventoryInstances(uuids, okapiConnectionParams))
      .doesNotThrowAnyException();
  }
}
