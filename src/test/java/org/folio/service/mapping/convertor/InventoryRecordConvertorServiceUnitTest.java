package org.folio.service.mapping.convertor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import io.vertx.core.json.JsonObject;
import java.util.*;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.mapping.MappingService;
import org.folio.service.mapping.MappingServiceImpl;
import org.folio.util.OkapiConnectionParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class InventoryRecordConvertorServiceUnitTest {

  @InjectMocks
  InventoryRecordConvertorService inventoryRecordConvertorService;
  @InjectMocks
  private MappingService mappingService = Mockito.spy(new MappingServiceImpl());

  @Mock
  private RecordLoaderService recordLoaderService;

  private static final String INSTANCE_ID = "c8b50e3f-0446-429c-960e-03774b88223f";
  private static final String HOLDINGS_ID = "65cb2bf0-d4c2-4886-8ad0-b76f1ba75d61";
  private static final String ITEM_ID_1 = "0b96a642-5e7f-452d-9cae-9cee66c9a892";
  private static final String ITEM_ID_2 = "5b31ec8c-95a7-4b91-95cc-b551a74b91ca";
  private static final String JOB_EXECUTION_ID = UUID.randomUUID().toString();

  InventoryRecordConvertorServiceUnitTest() {}

  @Test
  void appendHoldingsAndItems() {
    // given
    List<JsonObject> identifiers = new ArrayList<>();
    JsonObject instance = new JsonObject();
    instance.put("id", INSTANCE_ID);
    identifiers.add(instance);
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    MappingProfile mappingProfile = new MappingProfile();
    // when
    List<JsonObject> instancesHoldItem = inventoryRecordConvertorService.appendHoldingsAndItems(identifiers, mappingProfile, JOB_EXECUTION_ID, okapiConnectionParams);
    // then
    Mockito.verify(recordLoaderService, Mockito.times(0)).getHoldingsForInstance(anyString(), anyString(), any(OkapiConnectionParams.class));
    Mockito.verify(recordLoaderService, Mockito.times(0)).getAllItemsForHolding(anyList(), anyString(), any(OkapiConnectionParams.class));
    assertEquals(INSTANCE_ID, instancesHoldItem.get(0).getJsonObject("instance").getString("id"));
    assertNull(instancesHoldItem.get(0).getJsonArray("holdings"));
    assertNull( instancesHoldItem.get(0).getJsonArray("items"));

  }

  @Test
  void exportBlocking_shouldNotPopulateHoldingsItemsFor_MappingProfileTransformation_whenRecordTypesContainsInstanceOnly() {
    // given
    List<JsonObject> identifiers = new ArrayList<>();
    JsonObject instance = new JsonObject();
    instance.put("id", INSTANCE_ID);
    identifiers.add(instance);
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    MappingProfile mappingProfile = new MappingProfile()
      .withRecordTypes(Arrays.asList(RecordType.INSTANCE));
    // when
    List<JsonObject> instancesHoldItem = inventoryRecordConvertorService.appendHoldingsAndItems(identifiers, mappingProfile, JOB_EXECUTION_ID, okapiConnectionParams);
    // then
    Mockito.verify(recordLoaderService, Mockito.times(0)).getHoldingsForInstance(anyString(), anyString(), any(OkapiConnectionParams.class));
    Mockito.verify(recordLoaderService, Mockito.times(0)).getAllItemsForHolding(anyList(), anyString(), any(OkapiConnectionParams.class));
    assertEquals(INSTANCE_ID, instancesHoldItem.get(0).getJsonObject("instance").getString("id"));
    assertNull(instancesHoldItem.get(0).getJsonArray("holdings"));
    assertNull( instancesHoldItem.get(0).getJsonArray("items"));

  }

  @Test
  void appendHoldingsAndItems_shouldNotPopulateItemsFor_MappingProfileTransformation_whenRecordTypesContainsHoldingsOnly() {
    // given
    Mockito.when(recordLoaderService.getHoldingsForInstance(eq(INSTANCE_ID), anyString(), any(OkapiConnectionParams.class)))
    .thenReturn(Arrays.asList(new JsonObject().put("id", HOLDINGS_ID)));
    List<JsonObject> identifiers = new ArrayList<>();
    JsonObject instance = new JsonObject();
    instance.put("id", INSTANCE_ID);
    identifiers.add(instance);
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    MappingProfile mappingProfile = new MappingProfile()
      .withRecordTypes(Arrays.asList(RecordType.INSTANCE, RecordType.HOLDINGS))
      .withTransformations(Arrays.asList(new Transformations()));
    // when
    List<JsonObject> instancesHoldItem = inventoryRecordConvertorService.appendHoldingsAndItems(identifiers, mappingProfile, JOB_EXECUTION_ID, okapiConnectionParams);
    // then
    Mockito.verify(recordLoaderService, Mockito.times(1)).getHoldingsForInstance(anyString(), anyString(), any(OkapiConnectionParams.class));
    Mockito.verify(recordLoaderService, Mockito.times(0)).getAllItemsForHolding(anyList(), anyString(), any(OkapiConnectionParams.class));
    assertEquals(INSTANCE_ID, instancesHoldItem.get(0).getJsonObject("instance").getString("id"));
    assertNotNull(instancesHoldItem.get(0).getJsonArray("holdings"));
    assertEquals(HOLDINGS_ID, instancesHoldItem.get(0).getJsonArray("holdings").getJsonObject(0).getString("id"));
    assertNull( instancesHoldItem.get(0).getJsonArray("items"));

  }

  @Test
  void appendHoldingsAndItems_shouldPopulateHoldingsItemsFor_MappingProfileTransformation_whenRecordTypesContainsHoldingsnAndItem() {
    // given
    Mockito.when(recordLoaderService.getHoldingsForInstance(eq(INSTANCE_ID), anyString(), any(OkapiConnectionParams.class)))
    .thenReturn(Arrays.asList(new JsonObject().put("id", HOLDINGS_ID)));
    Mockito.when(recordLoaderService.getAllItemsForHolding(eq(Arrays.asList(HOLDINGS_ID)), anyString(), any(OkapiConnectionParams.class)))
    .thenReturn(Arrays.asList(new JsonObject().put("id", ITEM_ID_1),
                              new JsonObject().put("id", ITEM_ID_2)));
    List<JsonObject> identifiers = new ArrayList<>();
    JsonObject instance = new JsonObject();
    instance.put("id", INSTANCE_ID);
    identifiers.add(instance);
    Map<String, String> params = new HashMap<>();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(params);
    MappingProfile mappingProfile = new MappingProfile()
      .withRecordTypes(Arrays.asList(RecordType.INSTANCE, RecordType.HOLDINGS, RecordType.ITEM))
      .withTransformations(Arrays.asList(new Transformations()));
    // when
    List<JsonObject> instancesHoldItem = inventoryRecordConvertorService.appendHoldingsAndItems(identifiers, mappingProfile, JOB_EXECUTION_ID,okapiConnectionParams);
    // then
    Mockito.verify(recordLoaderService, Mockito.times(1)).getHoldingsForInstance(anyString(), anyString(), any(OkapiConnectionParams.class));
    Mockito.verify(recordLoaderService, Mockito.times(1)).getAllItemsForHolding(anyList(), anyString(), any(OkapiConnectionParams.class));
    assertEquals(INSTANCE_ID, instancesHoldItem.get(0).getJsonObject("instance").getString("id"));
    assertNotNull(instancesHoldItem.get(0).getJsonArray("holdings"));
    assertNotNull( instancesHoldItem.get(0).getJsonArray("items"));
    assertEquals(INSTANCE_ID, instancesHoldItem.get(0).getJsonObject("instance").getString("id"));
    assertEquals(HOLDINGS_ID, instancesHoldItem.get(0).getJsonArray("holdings").getJsonObject(0).getString("id"));
    assertEquals(ITEM_ID_1, instancesHoldItem.get(0).getJsonArray("items").getJsonObject(0).getString("id"));
    assertEquals(ITEM_ID_2, instancesHoldItem.get(0).getJsonArray("items").getJsonObject(1).getString("id"));

  }

}

