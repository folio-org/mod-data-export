package org.folio.service.loader;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.folio.clients.impl.SourceRecordStorageClient;
import org.folio.rest.impl.HttpServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

@RunWith(MockitoJUnitRunner.class)
public class BlockingRecordLoaderServiceTest extends HttpServerTestBase {
  @Spy
  private SourceRecordStorageClient client;
  @InjectMocks
  private RecordLoaderService recordLoaderService = new BlockingRecordLoaderService();

  @BeforeClass
  public static void beforeClass() throws Exception {
    setUpHttpServer();
    setUpMocks();
  }

  private static void setUpMocks() throws IOException {
    String json = IOUtils.toString(new FileReader("src/test/resources/marcs_db.json"));
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
    List<String> uuids = Arrays.asList("5fc04e92-70dd-46b8-97ea-194015762a60", "ae573875-fbc8-40e7-bda7-0ac283354226", "be573875-fbc8-40e7-bda7-0ac283354227");
    // when
    SrsLoadResult srsLoadResult = recordLoaderService.loadMarcRecords(uuids, okapiConnectionParams);
    Collection<String> actualRecords = srsLoadResult.getUnderlyingMarcRecords();
    // then
    assertThat(actualRecords, hasSize(2));
    assertThat(srsLoadResult.getInstanceIdsWithoutSrs(), hasSize(1));
    assertThat(actualRecords, hasItems("812eaaa7-5d67-4c1a-a6dc-6050e6f08c92 content", "47178cad-a892-4c2a-b9e4-bb33dea6fc31 content"));
  }

  @Test
  public void shouldReturnNotFoundInstanceIds() {
    // given
    List<String> uuids = Arrays.asList("6fc04e92-70dd-46b8-97ea-194015762a61", "be573875-fbc8-40e7-bda7-0ac283354227");
    // when
    SrsLoadResult srsLoadResult = recordLoaderService.loadMarcRecords(uuids, okapiConnectionParams);
    // then
    assertThat(srsLoadResult.getInstanceIdsWithoutSrs(), hasSize(2));
  }

  @Test
  public void loadInstanceRecords_doesNotThrowAnyException() {
    List<String> uuids = new ArrayList<>();
    // given
    assertThatCode(() -> recordLoaderService.loadInventoryInstances(uuids, okapiConnectionParams))
      .doesNotThrowAnyException();
  }

}
