package org.folio.service.loader;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.IOUtils;
import org.folio.clients.StorageClient;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
public class RecordLoaderServiceUnitTest extends HttpServerTestBase {
  private static final int LIMIT = 20;

  @BeforeClass
  public static void beforeClass() throws Exception {
    setUpHttpServer();
    setUpMocks();
  }

  private static void setUpMocks() throws IOException {
    String json = IOUtils.toString(new FileReader("src/test/resources/srsResponse.json"));
    JsonObject data = new JsonObject(json);
    String instancesJson = IOUtils.toString(new FileReader("src/test/resources/inventoryStorageResponse.json"));
    JsonObject instanceData = new JsonObject(instancesJson);
    router.route("/source-storage/records").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "application/json");
      response.end(data.toBuffer());
    });
    router.route("/instance-storage/instances").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "application/json");
      response.end(instanceData.toBuffer());
    });
  }

  @Test
  public void shouldReturnExistingMarcRecords() {
    // given
    StorageClient client = Mockito.spy(StorageClient.class);
    RecordLoaderService recordLoaderService = new RecordLoaderServiceImpl(client);
    // when
    SrsLoadResult srsLoadResult = recordLoaderService.loadMarcRecordsBlocking(new ArrayList<>(), okapiConnectionParams, LIMIT);
    // then
    assertThat(srsLoadResult.getUnderlyingMarcRecords(), hasSize(2));
  }

  @Test
  public void shouldReturnNotFoundInstanceIds() {
    // given
    List<String> uuids = Arrays.asList("6fc04e92-70dd-46b8-97ea-194015762a61", "be573875-fbc8-40e7-bda7-0ac283354227");
    JsonObject emptyResponse = new JsonObject().put("records", new JsonArray());
    StorageClient client = Mockito.mock(StorageClient.class);
    when(client.getByIdsFromSRS(anyList(), any(OkapiConnectionParams.class), eq(LIMIT))).thenReturn(Optional.of(emptyResponse));
    RecordLoaderService recordLoaderService = new RecordLoaderServiceImpl(client);
    // when
    SrsLoadResult srsLoadResult = recordLoaderService.loadMarcRecordsBlocking(uuids, okapiConnectionParams, LIMIT);
    // then
    assertThat(srsLoadResult.getInstanceIdsWithoutSrs(), hasSize(2));
    assertThat(srsLoadResult.getUnderlyingMarcRecords(), empty());
  }

  @Test
  public void loadInstanceRecords_doesNotThrowAnyException() {
    // given
    List<String> uuids = new ArrayList<>();
    StorageClient client = Mockito.spy(StorageClient.class);
    RecordLoaderService recordLoaderService = new RecordLoaderServiceImpl(client);
    // when call loadInventoryInstances method, then assert no exception thrown
    assertThatCode(() -> recordLoaderService.loadInventoryInstancesBlocking(uuids, okapiConnectionParams, LIMIT))
      .doesNotThrowAnyException();
  }

  @Test
  public void loadInstanceRecords_shouldReturnTwoRecordsByIds() {
    // given
    List<String> uuids = Arrays.asList("f31a36de-fcf8-44f9-87ef-a55d06ad21ae", "3c4ae3f3-b460-4a89-a2f9-78ce3145e4fc");
    StorageClient client = Mockito.spy(StorageClient.class);
    RecordLoaderService recordLoaderService = new RecordLoaderServiceImpl(client);
    // when
    List<JsonObject> inventoryResponse = recordLoaderService.loadInventoryInstancesBlocking(uuids, okapiConnectionParams, LIMIT);
    //then
    assertThat(inventoryResponse, hasSize(2));
  }

  @Test
  public void loadInstanceRecords_shouldReturnEmptyList_whenThereInNoRecordsInInventory() throws IOException {
    // given
    StorageClient client = Mockito.mock(StorageClient.class);
    String json = IOUtils.toString(new FileReader("src/test/resources/InventoryStorageEmptyResponse.json"));
    JsonObject data = new JsonObject(json);
    when(client.getByIdsFromInventory(anyList(), eq(okapiConnectionParams), eq(LIMIT))).thenReturn(Optional.of(data));
    List<String> uuids = Collections.singletonList(UUID.randomUUID().toString());
    RecordLoaderService recordLoaderService = new RecordLoaderServiceImpl(client);
    // when
    List<JsonObject> inventoryResponse = recordLoaderService.loadInventoryInstancesBlocking(uuids, okapiConnectionParams, LIMIT);
    //then
    assertThat(inventoryResponse, empty());
  }

  @Test
  public void loadInstanceRecords_shouldReturnEmptyList_whenOptionalResponseIsNotPresent() {
    // given
    StorageClient client = Mockito.mock(StorageClient.class);
    when(client.getByIdsFromInventory(anyList(), eq(okapiConnectionParams), eq(LIMIT))).thenReturn(Optional.empty());
    List<String> uuids = Collections.singletonList(UUID.randomUUID().toString());
    RecordLoaderService recordLoaderService = new RecordLoaderServiceImpl(client);
    // when
    List<JsonObject> inventoryResponse = recordLoaderService.loadInventoryInstancesBlocking(uuids, okapiConnectionParams, LIMIT);
    //then
    assertThat(inventoryResponse, empty());
  }

}
