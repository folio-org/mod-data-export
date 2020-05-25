package org.folio.service.loader;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.folio.TestUtil.readFileContentFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import java.util.*;
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

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class RecordLoaderServiceUnitTest extends HttpServerTestBase {
  private static final int LIMIT = 20;
  protected static final String INVENTORY_RESPONSE_JSON = "clients/inventory/get_instances_response.json";
  protected static final String EMPTY_RESPONSE_JSON = "clients/inventory/get_empty_response.json";
  protected static final String SRS_RESPONSE_JSON = "mockData/srs/get_records_response.json";
  protected static final String HOLDINGS_RESPONSE_JSON = "mockData/inventory/holdings_in000005.json";

  @Mock
  SourceRecordStorageClient srsClient;
  @Mock
  InventoryClient inventoryClient;
  @Spy
  @InjectMocks
  RecordLoaderServiceImpl recordLoaderService;

  static JsonObject dataFromSRS;
  static JsonObject dataFromInventory;
  static JsonObject dataFromInventoryHoldings;

  @BeforeAll
  public static void setUp() {
    String json = readFileContentFromResources(SRS_RESPONSE_JSON);
    dataFromSRS = new JsonObject(json);
    String instancesJson = readFileContentFromResources(INVENTORY_RESPONSE_JSON);
    dataFromInventory = new JsonObject(instancesJson);

    String holdingssJson = readFileContentFromResources(HOLDINGS_RESPONSE_JSON);
    dataFromInventoryHoldings = new JsonObject(holdingssJson);
  }

  @Test
  void shouldReturnExistingMarcRecords() {
    // given
    when(srsClient.getRecordsByIds(anyList(), eq(okapiConnectionParams), eq(LIMIT))).thenReturn(Optional.of(dataFromSRS));
    // when
    SrsLoadResult srsLoadResult = recordLoaderService.loadMarcRecordsBlocking(new ArrayList<>(), okapiConnectionParams, LIMIT);
    // then
    assertThat(srsLoadResult.getUnderlyingMarcRecords(), hasSize(2));
  }

  @Test
  void shouldReturnNotFoundInstanceIds() {
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
  void loadInstanceRecords_doesNotThrowAnyException() {
    // given
    List<String> uuids = new ArrayList<>();
    // when call loadInventoryInstances method, then assert no exception thrown
    assertThatCode(() -> recordLoaderService.loadInventoryInstancesBlocking(uuids, okapiConnectionParams, LIMIT))
      .doesNotThrowAnyException();
  }

  @Test
  void loadInstanceRecords_shouldReturnTwoRecordsByIds() {
    // given
    List<String> uuids = Arrays.asList("f31a36de-fcf8-44f9-87ef-a55d06ad21ae", "3c4ae3f3-b460-4a89-a2f9-78ce3145e4fc");
    when(inventoryClient.getInstancesByIds(anyList(), eq(okapiConnectionParams), eq(LIMIT))).thenReturn(Optional.of(dataFromInventory));
    // when
    List<JsonObject> inventoryResponse = recordLoaderService.loadInventoryInstancesBlocking(uuids, okapiConnectionParams, LIMIT);
    //then
    assertThat(inventoryResponse, hasSize(2));
  }

  @Test
  void loadInstanceRecords_shouldReturnEmptyList_whenThereInNoRecordsInInventory() {
    // given
    JsonObject data = buildEmptyResponse("instances");
    when(inventoryClient.getInstancesByIds(anyList(), eq(okapiConnectionParams), eq(LIMIT))).thenReturn(Optional.of(data));
    List<String> uuids = Collections.singletonList(UUID.randomUUID().toString());
    // when
    List<JsonObject> inventoryResponse = recordLoaderService.loadInventoryInstancesBlocking(uuids, okapiConnectionParams, LIMIT);
    //then
    assertThat(inventoryResponse, empty());
  }

  @Test
  void loadInstanceRecords_shouldReturnEmptyList_whenOptionalResponseIsNotPresent() {
    // given
    when(inventoryClient.getInstancesByIds(anyList(), eq(okapiConnectionParams), eq(LIMIT))).thenReturn(Optional.empty());
    List<String> uuids = Collections.singletonList(UUID.randomUUID().toString());
    // when
    List<JsonObject> inventoryResponse = recordLoaderService.loadInventoryInstancesBlocking(uuids, okapiConnectionParams, LIMIT);
    //then
    assertThat(inventoryResponse, empty());
  }

  @Test
  void getHoldingsRecords_shouldReturnTwoRecordsByIds() {
    // given
    String instanceUUID = "f31a36de-fcf8-44f9-87ef-a55d06ad21ae";
    when(inventoryClient.getHoldingsByInstanceId(anyString(), eq(okapiConnectionParams))).thenReturn(Optional.of(dataFromInventoryHoldings));
    // when
    List<JsonObject> holdingsResponse = recordLoaderService.getHoldingsForInstance(instanceUUID, okapiConnectionParams);
    //then
    assertThat(holdingsResponse, hasSize(2));
  }

  @Test
  void getHoldingsRecords_shouldReturnEmptyList_whenThereInNoHoldingsRecords() {
    // given
    JsonObject data = buildEmptyResponse("holdingsRecords");
    String instanceUUID = "f31a36de-fcf8-44f9-87ef-a55d06ad21ae";
    when(inventoryClient.getHoldingsByInstanceId(anyString(), eq(okapiConnectionParams))).thenReturn(Optional.of(data));
    // when
    List<JsonObject> holdingsResponse = recordLoaderService.getHoldingsForInstance(instanceUUID, okapiConnectionParams);
    //then
    assertThat(holdingsResponse, empty());
  }

  @Test
  void getItemsRecords_shouldReturnEmptyList_whenThereInNoItemRecords() {
    // given
    JsonObject data = buildEmptyResponse("items");
    List<String> holdingIDs = Collections.singletonList(UUID.randomUUID().toString());
    when(inventoryClient.getItemsByHoldingIds(anyList(), eq(okapiConnectionParams))).thenReturn(Optional.of(data));
    // when
    List<JsonObject> itemsResponse = recordLoaderService.getAllItemsForHolding(holdingIDs, okapiConnectionParams);
    //then
    assertThat(itemsResponse, empty());
  }

  private JsonObject buildEmptyResponse(String entity) {
    String json = readFileContentFromResources(EMPTY_RESPONSE_JSON);
    JsonObject data = new JsonObject(json);
    data.put(entity, new JsonArray());
    return data;
  }
}
