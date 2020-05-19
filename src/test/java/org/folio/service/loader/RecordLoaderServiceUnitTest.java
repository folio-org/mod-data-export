package org.folio.service.loader;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import org.folio.clients.InventoryClient;
import org.folio.clients.SourceRecordStorageClient;
import org.folio.rest.HttpServerTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.folio.TestUtil.readFileContentFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class RecordLoaderServiceUnitTest extends HttpServerTestBase {
  private static final int LIMIT = 20;
  protected static final String INVENTORY_RESPONSE_JSON = "clients/inventory/get_instances_response.json";
  protected static final String INVENTORY_EMPTY_RESPONSE_JSON = "clients/inventory/get_instances_empty_response.json";
  protected static final String SRS_RESPONSE_JSON = "mockData/srs/get_records_response.json";

  @Mock
  SourceRecordStorageClient srsClient;
  @Mock
  InventoryClient inventoryClient;
  @Spy
  @InjectMocks
  RecordLoaderServiceImpl recordLoaderService;

  static JsonObject dataFromSRS;
  static JsonObject dataFromInventory;

  @BeforeAll
  public static void setUp() {
    String json = readFileContentFromResources(SRS_RESPONSE_JSON);
    dataFromSRS = new JsonObject(json);
    String instancesJson = readFileContentFromResources(INVENTORY_RESPONSE_JSON);
    dataFromInventory = new JsonObject(instancesJson);
  }

  @Test
  public void shouldReturnExistingMarcRecords() {
    // given
    when(srsClient.getRecordsByIds(anyList(), eq(okapiConnectionParams), eq(LIMIT))).thenReturn(Optional.of(dataFromSRS));
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
    when(srsClient.getRecordsByIds(anyList(), eq(okapiConnectionParams), eq(LIMIT))).thenReturn(Optional.of(emptyResponse));
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
    // when call loadInventoryInstances method, then assert no exception thrown
    assertThatCode(() -> recordLoaderService.loadInventoryInstancesBlocking(uuids, okapiConnectionParams, LIMIT))
      .doesNotThrowAnyException();
  }

  @Test
  public void loadInstanceRecords_shouldReturnTwoRecordsByIds() {
    // given
    List<String> uuids = Arrays.asList("f31a36de-fcf8-44f9-87ef-a55d06ad21ae", "3c4ae3f3-b460-4a89-a2f9-78ce3145e4fc");
    when(inventoryClient.getInstancesByIds(anyList(), eq(okapiConnectionParams), eq(LIMIT))).thenReturn(Optional.of(dataFromInventory));
    // when
    List<JsonObject> inventoryResponse = recordLoaderService.loadInventoryInstancesBlocking(uuids, okapiConnectionParams, LIMIT);
    //then
    assertThat(inventoryResponse, hasSize(2));
  }

  @Test
  public void loadInstanceRecords_shouldReturnEmptyList_whenThereInNoRecordsInInventory() {
    // given
    String json = readFileContentFromResources(INVENTORY_EMPTY_RESPONSE_JSON);
    JsonObject data = new JsonObject(json);
    when(inventoryClient.getInstancesByIds(anyList(), eq(okapiConnectionParams), eq(LIMIT))).thenReturn(Optional.of(data));
    List<String> uuids = Collections.singletonList(UUID.randomUUID().toString());
    // when
    List<JsonObject> inventoryResponse = recordLoaderService.loadInventoryInstancesBlocking(uuids, okapiConnectionParams, LIMIT);
    //then
    assertThat(inventoryResponse, empty());
  }

  @Test
  public void loadInstanceRecords_shouldReturnEmptyList_whenOptionalResponseIsNotPresent() {
    // given
    when(inventoryClient.getInstancesByIds(anyList(), eq(okapiConnectionParams), eq(LIMIT))).thenReturn(Optional.empty());
    List<String> uuids = Collections.singletonList(UUID.randomUUID().toString());
    // when
    List<JsonObject> inventoryResponse = recordLoaderService.loadInventoryInstancesBlocking(uuids, okapiConnectionParams, LIMIT);
    //then
    assertThat(inventoryResponse, empty());
  }

}
