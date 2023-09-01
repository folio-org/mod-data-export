package org.folio.service.loader;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import org.folio.clients.InventoryClient;
import org.folio.clients.SourceRecordStorageClient;
import org.folio.service.manager.export.strategy.AbstractExportStrategy;
import org.folio.util.OkapiConnectionParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import static org.folio.service.loader.RecordLoaderServiceImpl.CONSORTIUM_MARC_INSTANCE_SOURCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class RecordLoaderServiceUnitTest{
  private static final int LIMIT = 20;
  protected static final String INVENTORY_RESPONSE_JSON = "clients/inventory/get_instances_response.json";
  protected static final String INVENTORY_RESPONSE_WITH_CONSORTIUM_MARC_SOURCE_JSON = "clients/inventory/get_instances_response_with_consortium_marc_source.json";
  protected static final String EMPTY_RESPONSE_JSON = "clients/inventory/get_empty_response.json";
  protected static final String SRS_RESPONSE_JSON = "mockData/srs/get_marc_bib_records_response.json";
  protected static final String SRS_SINGLE_MARC_RECORD_RESPONSE_JSON = "mockData/srs/single_marc_record_response.json";
  protected static final String HOLDINGS_RESPONSE_JSON = "mockData/inventory/holdings_in000005.json";
  private static final String JOB_EXECUTION_ID = UUID.randomUUID().toString();
  private static final String PRECEDING_TITLES = "precedingTitles";
  private static final String SUCCEEDING_TITLES = "succeedingTitles";

  @Mock
  SourceRecordStorageClient srsClient;
  @Mock
  InventoryClient inventoryClient;
  @Spy
  @InjectMocks
  RecordLoaderServiceImpl recordLoaderService;

  protected OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams();
  static JsonObject dataFromSRS;
  static JsonObject dataSingleMarcRecordFromSRS;
  static JsonObject dataFromInventory;
  static JsonObject dataWithConsortiumMarcSourceFromInventory;
  static JsonObject dataFromInventoryHoldings;

  RecordLoaderServiceUnitTest() {
    String json = readFileContentFromResources(SRS_RESPONSE_JSON);
    dataFromSRS = new JsonObject(json);
    String singleMarcRecord = readFileContentFromResources(SRS_SINGLE_MARC_RECORD_RESPONSE_JSON);
    dataSingleMarcRecordFromSRS = new JsonObject(singleMarcRecord);
    String instancesJson = readFileContentFromResources(INVENTORY_RESPONSE_JSON);
    dataFromInventory = new JsonObject(instancesJson);
    String instancesWithConsortiumMarcSourceJson = readFileContentFromResources(INVENTORY_RESPONSE_WITH_CONSORTIUM_MARC_SOURCE_JSON);
    dataWithConsortiumMarcSourceFromInventory = new JsonObject(instancesWithConsortiumMarcSourceJson);
    String holdingssJson = readFileContentFromResources(HOLDINGS_RESPONSE_JSON);
    dataFromInventoryHoldings = new JsonObject(holdingssJson);
  }

  @Test
  void shouldReturnExistingMarcRecords() {
    // given
    when(srsClient.getRecordsByIdsFromLocalTenant(anyList(), eq(AbstractExportStrategy.EntityType.INSTANCE), anyString(), eq(okapiConnectionParams))).thenReturn(Optional.of(dataFromSRS));
    // when
    SrsLoadResult srsLoadResult = recordLoaderService.loadMarcRecordsBlocking(List.of("uuid"), AbstractExportStrategy.EntityType.INSTANCE, JOB_EXECUTION_ID, okapiConnectionParams);
    // then
    assertThat(srsLoadResult.getUnderlyingMarcRecords(), hasSize(2));
  }

  @ParameterizedTest
  @EnumSource(value = AbstractExportStrategy.EntityType.class, names = {"INSTANCE", "AUTHORITY"},  mode = EnumSource.Mode.INCLUDE)
  void shouldReturnExistingMarcRecordsFromConsortiumAndLocalTenantWhenTenantInConsortium(AbstractExportStrategy.EntityType type) {
    // given
    var uuids = List.of("f31a36de-fcf8-44f9-87ef-a55d06ad21ae", "3c4ae3f3-b460-4a89-a2f9-78ce3145e4fc");
    var uuidInConsortium = List.of("f31a36de-fcf8-44f9-87ef-a55d06ad21ae");
    var uuidNotInConsortium = List.of("3c4ae3f3-b460-4a89-a2f9-78ce3145e4fc");
    when(inventoryClient.getInstancesByIds(eq(uuids), anyString(), eq(okapiConnectionParams), eq(CONSORTIUM_MARC_INSTANCE_SOURCE))).thenReturn(Optional.of(dataWithConsortiumMarcSourceFromInventory));

    when(srsClient.getRecordsByIdsFromCentralTenant(eq(uuidInConsortium), eq(type), anyString(), eq(okapiConnectionParams))).thenReturn(Optional.of(dataSingleMarcRecordFromSRS));
    when(srsClient.getRecordsByIdsFromLocalTenant(eq(uuidNotInConsortium), eq(type), anyString(), eq(okapiConnectionParams))).thenReturn(Optional.of(dataSingleMarcRecordFromSRS));
    // when
    SrsLoadResult srsLoadResult = recordLoaderService.loadMarcRecordsBlocking(uuids, type, JOB_EXECUTION_ID, okapiConnectionParams);
    // then
    assertThat(srsLoadResult.getUnderlyingMarcRecords(), hasSize(2));
  }

  @ParameterizedTest
  @EnumSource(value = AbstractExportStrategy.EntityType.class, names = {"INSTANCE", "AUTHORITY"},  mode = EnumSource.Mode.INCLUDE)
  void shouldReturnExistingMarcRecordsFromConsortiumWhenMarcRecordFromLocalNotExist(AbstractExportStrategy.EntityType type) {
    // given
    var uuids = List.of("f31a36de-fcf8-44f9-87ef-a55d06ad21ae", "3c4ae3f3-b460-4a89-a2f9-78ce3145e4fc");
    var uuidInConsortium = List.of("f31a36de-fcf8-44f9-87ef-a55d06ad21ae");
    var uuidNotInConsortium = List.of("3c4ae3f3-b460-4a89-a2f9-78ce3145e4fc");
    when(inventoryClient.getInstancesByIds(eq(uuids), anyString(), eq(okapiConnectionParams), eq(CONSORTIUM_MARC_INSTANCE_SOURCE))).thenReturn(Optional.of(dataWithConsortiumMarcSourceFromInventory));

    when(srsClient.getRecordsByIdsFromCentralTenant(eq(uuidInConsortium), eq(type), anyString(), eq(okapiConnectionParams))).thenReturn(Optional.of(dataSingleMarcRecordFromSRS));
    when(srsClient.getRecordsByIdsFromLocalTenant(eq(uuidNotInConsortium), eq(type), anyString(), eq(okapiConnectionParams))).thenReturn(Optional.empty());
    // when
    SrsLoadResult srsLoadResult = recordLoaderService.loadMarcRecordsBlocking(uuids, type, JOB_EXECUTION_ID, okapiConnectionParams);
    // then
    assertThat(srsLoadResult.getUnderlyingMarcRecords(), hasSize(1));
  }

  @ParameterizedTest
  @EnumSource(value = AbstractExportStrategy.EntityType.class, names = {"INSTANCE", "AUTHORITY"},  mode = EnumSource.Mode.INCLUDE)
  void shouldReturnExistingMarcRecordsFromLocalWhenMarcRecordFromConsortiumNotExist(AbstractExportStrategy.EntityType type) {
    // given
    var uuids = List.of("f31a36de-fcf8-44f9-87ef-a55d06ad21ae", "3c4ae3f3-b460-4a89-a2f9-78ce3145e4fc");
    var uuidInConsortium = List.of("f31a36de-fcf8-44f9-87ef-a55d06ad21ae");
    var uuidNotInConsortium = List.of("3c4ae3f3-b460-4a89-a2f9-78ce3145e4fc");
    when(inventoryClient.getInstancesByIds(eq(uuids), anyString(), eq(okapiConnectionParams), eq(CONSORTIUM_MARC_INSTANCE_SOURCE))).thenReturn(Optional.of(dataWithConsortiumMarcSourceFromInventory));

    when(srsClient.getRecordsByIdsFromCentralTenant(eq(uuidInConsortium), eq(type), anyString(), eq(okapiConnectionParams))).thenReturn(Optional.empty());
    when(srsClient.getRecordsByIdsFromLocalTenant(eq(uuidNotInConsortium), eq(type), anyString(), eq(okapiConnectionParams))).thenReturn(Optional.of(dataSingleMarcRecordFromSRS));
    // when
    SrsLoadResult srsLoadResult = recordLoaderService.loadMarcRecordsBlocking(uuids, type, JOB_EXECUTION_ID, okapiConnectionParams);
    // then
    assertThat(srsLoadResult.getUnderlyingMarcRecords(), hasSize(1));
  }

  @Test
  void shouldReturnNotFoundInstanceIds() {
    // given
    List<String> uuids = Arrays.asList("6fc04e92-70dd-46b8-97ea-194015762a61", "be573875-fbc8-40e7-bda7-0ac283354227");
    JsonObject emptyResponse = new JsonObject().put("sourceRecords", new JsonArray());
    when(srsClient.getRecordsByIdsFromLocalTenant(anyList(), eq(AbstractExportStrategy.EntityType.INSTANCE), anyString(), eq(okapiConnectionParams))).thenReturn(Optional.of(emptyResponse));
    // when
    SrsLoadResult srsLoadResult = recordLoaderService.loadMarcRecordsBlocking(uuids, AbstractExportStrategy.EntityType.INSTANCE, JOB_EXECUTION_ID, okapiConnectionParams);
    // then
    assertThat(srsLoadResult.getIdsWithoutSrs(), hasSize(2));
    assertThat(srsLoadResult.getUnderlyingMarcRecords(), empty());
  }

  @Test
  void loadInstanceRecords_doesNotThrowAnyException() {
    // given
    List<String> uuids = new ArrayList<>();
    // when call loadInventoryInstances method, then assert no exception thrown
    assertThatCode(() -> recordLoaderService.loadInventoryInstancesBlocking(uuids, JOB_EXECUTION_ID, okapiConnectionParams, LIMIT))
      .doesNotThrowAnyException();
  }

  @Test
  void loadInstanceRecords_shouldReturnTwoRecordsByIds() {
    // given
    List<String> uuids = Arrays.asList("f31a36de-fcf8-44f9-87ef-a55d06ad21ae", "3c4ae3f3-b460-4a89-a2f9-78ce3145e4fc");
    when(inventoryClient.getInstancesWithPrecedingSucceedingTitlesByIds(anyList(), anyString(), eq(okapiConnectionParams), eq(LIMIT))).thenReturn(Optional.of(dataFromInventory));
    // when
    LoadResult inventoryResponse = recordLoaderService.loadInventoryInstancesBlocking(uuids, JOB_EXECUTION_ID, okapiConnectionParams, LIMIT);
    //then
    assertThat(inventoryResponse.getEntities(), hasSize(2));
    inventoryResponse.getEntities().forEach(entity -> {
      assertTrue(entity.containsKey(PRECEDING_TITLES));
      assertTrue(entity.containsKey(SUCCEEDING_TITLES));
    });
  }

  @Test
  void loadInstanceRecords_shouldReturnEmptyList_whenThereInNoRecordsInInventory() {
    // given
    JsonObject data = buildEmptyResponse("instances");
    when(inventoryClient.getInstancesWithPrecedingSucceedingTitlesByIds(anyList(), anyString(), eq(okapiConnectionParams), eq(LIMIT))).thenReturn(Optional.of(data));
    List<String> uuids = Collections.singletonList(UUID.randomUUID().toString());
    // when
    LoadResult inventoryResponse = recordLoaderService.loadInventoryInstancesBlocking(uuids, JOB_EXECUTION_ID, okapiConnectionParams, LIMIT);
    //then
    assertThat(inventoryResponse.getEntities(), empty());
  }

  @Test
  void loadInstanceRecords_shouldReturnEmptyList_whenOptionalResponseIsNotPresent() {
    // given
    when(inventoryClient.getInstancesWithPrecedingSucceedingTitlesByIds(anyList(), anyString(), eq(okapiConnectionParams), eq(LIMIT))).thenReturn(Optional.empty());
    List<String> uuids = Collections.singletonList(UUID.randomUUID().toString());
    // when
    LoadResult inventoryResponse = recordLoaderService.loadInventoryInstancesBlocking(uuids, JOB_EXECUTION_ID, okapiConnectionParams, LIMIT);
    //then
    assertThat(inventoryResponse.getEntities(), empty());
  }

  @Test
  void getHoldingsRecords_shouldReturnTwoRecordsByIds() {
    // given
    String instanceUUID = "f31a36de-fcf8-44f9-87ef-a55d06ad21ae";
    when(inventoryClient.getHoldingsByInstanceId(anyString(), anyString(), eq(okapiConnectionParams))).thenReturn(Optional.of(dataFromInventoryHoldings));
    // when
    List<JsonObject> holdingsResponse = recordLoaderService.getHoldingsForInstance(instanceUUID, JOB_EXECUTION_ID, okapiConnectionParams);
    //then
    assertThat(holdingsResponse, hasSize(2));
  }

  @Test
  void getHoldingsRecords_shouldReturnEmptyList_whenThereInNoHoldingsRecords() {
    // given
    JsonObject data = buildEmptyResponse("holdingsRecords");
    String instanceUUID = "f31a36de-fcf8-44f9-87ef-a55d06ad21ae";
    when(inventoryClient.getHoldingsByInstanceId(anyString(), anyString(), eq(okapiConnectionParams))).thenReturn(Optional.of(data));
    // when
    List<JsonObject> holdingsResponse = recordLoaderService.getHoldingsForInstance(instanceUUID, JOB_EXECUTION_ID, okapiConnectionParams);
    //then
    assertThat(holdingsResponse, empty());
  }

  @Test
  void getItemsRecords_shouldReturnEmptyList_whenThereInNoItemRecords() {
    // given
    JsonObject data = buildEmptyResponse("items");
    List<String> holdingIDs = Collections.singletonList(UUID.randomUUID().toString());
    when(inventoryClient.getItemsByHoldingIds(anyList(), anyString(), eq(okapiConnectionParams))).thenReturn(Optional.of(data));
    // when
    List<JsonObject> itemsResponse = recordLoaderService.getAllItemsForHolding(holdingIDs, JOB_EXECUTION_ID, okapiConnectionParams);
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
