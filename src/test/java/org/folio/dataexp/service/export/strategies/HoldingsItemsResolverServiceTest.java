package org.folio.dataexp.service.export.strategies;

import static org.folio.dataexp.service.export.Constants.HOLDINGS_KEY;
import static org.folio.dataexp.service.export.Constants.ID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_HRID_KEY;
import static org.folio.dataexp.service.export.Constants.ITEMS_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.folio.dataexp.client.SearchConsortiumHoldings;
import org.folio.dataexp.domain.dto.ConsortiumHolding;
import org.folio.dataexp.domain.dto.ConsortiumHoldingCollection;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.User;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.ItemEntity;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.HoldingsRecordEntityTenantRepository;
import org.folio.dataexp.repository.ItemEntityTenantRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.service.validators.PermissionsValidator;
import org.folio.dataexp.util.ErrorCode;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class HoldingsItemsResolverServiceTest {

  @Mock private HoldingsRecordEntityRepository holdingsRecordEntityRepository;
  @Mock private HoldingsRecordEntityTenantRepository holdingsRecordEntityTenantRepository;
  @Mock private ItemEntityTenantRepository itemEntityTenantRepository;
  @Mock private ConsortiaService consortiaService;
  @Mock private SearchConsortiumHoldings searchConsortiumHoldings;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private UserService userService;
  @Mock private ErrorLogService errorLogService;
  @Mock private EntityManager entityManager;
  @Mock private PermissionsValidator permissionsValidator;

  @InjectMocks private HoldingsItemsResolverService holdingsItemsResolverService;

  @Test
  void retrieveHoldingsAndItemsByInstanceIdForLocalTenantTest() {
    var holding = "{'id' : '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var holdingId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var holdingRecordEntity =
        HoldingsRecordEntity.builder().jsonb(holding).id(holdingId).instanceId(instanceId).build();
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(
        List.of(RecordTypes.INSTANCE, RecordTypes.HOLDINGS, RecordTypes.ITEM));

    when(folioExecutionContext.getTenantId()).thenReturn("localTenant");
    when(holdingsRecordEntityRepository.findByInstanceIdIs(instanceId))
        .thenReturn(List.of(holdingRecordEntity));
    var item = "{'barcode' : 'itemBarcode'}";
    var itemEntity =
        ItemEntity.builder().id(UUID.randomUUID()).holdingsRecordId(holdingId).jsonb(item).build();
    when(itemEntityTenantRepository.findByHoldingsRecordIdIn(anyString(), anySet()))
        .thenReturn(List.of(itemEntity));
    doNothing().when(entityManager).clear();

    var instanceJson = new JSONObject();

    var instanceHrid = "instHrid";
    holdingsItemsResolverService.retrieveHoldingsAndItemsByInstanceId(
        instanceJson, instanceId, instanceHrid, mappingProfile, UUID.randomUUID());

    var holdingJson = (JSONObject) ((JSONArray) instanceJson.get(HOLDINGS_KEY)).get(0);
    assertEquals("instHrid", holdingJson.getAsString(INSTANCE_HRID_KEY));
    assertEquals(holdingId.toString(), holdingJson.getAsString(ID_KEY));

    var itemJsonArray = (JSONArray) holdingJson.get(ITEMS_KEY);
    assertEquals(1, itemJsonArray.size());
  }

  @Test
  void retrieveHoldingsAndItemsByInstanceIdForCentralTenantTest() {
    var user = new User();
    user.setId(UUID.randomUUID().toString());
    user.setUsername("username");

    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var holdingId1 = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");

    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(
        List.of(RecordTypes.INSTANCE, RecordTypes.HOLDINGS, RecordTypes.ITEM));

    var consortiumHolding1 = new ConsortiumHolding();
    consortiumHolding1.setInstanceId(instanceId.toString());
    consortiumHolding1.setId(holdingId1.toString());
    consortiumHolding1.setTenantId("member1");

    var consortiumHolding2 = new ConsortiumHolding();
    var holdingId2 = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    consortiumHolding2.setInstanceId(instanceId.toString());
    consortiumHolding2.setId(holdingId2.toString());
    consortiumHolding2.setTenantId("member2");

    var consortiumHolding3 = new ConsortiumHolding();
    consortiumHolding3.setInstanceId(instanceId.toString());
    consortiumHolding3.setId(UUID.randomUUID().toString());
    consortiumHolding3.setTenantId("member3");

    var consortiumHolding4 = new ConsortiumHolding();
    consortiumHolding4.setInstanceId(instanceId.toString());
    consortiumHolding4.setId(UUID.randomUUID().toString());
    consortiumHolding4.setTenantId("member4");

    var consortiumHoldings = new ConsortiumHoldingCollection();
    consortiumHoldings.setHoldings(
        List.of(consortiumHolding1, consortiumHolding2, consortiumHolding3, consortiumHolding4));

    HashMap<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put("header", List.of("value"));

    when(folioExecutionContext.getTenantId()).thenReturn("central");
    when(folioExecutionContext.getUserId()).thenReturn(UUID.fromString(user.getId()));
    when(consortiaService.isCurrentTenantCentralTenant("central")).thenReturn(true);
    when(consortiaService.getAffiliatedTenants(isA(String.class), isA(String.class)))
        .thenReturn(List.of("member1", "member2"));
    when(searchConsortiumHoldings.getHoldingsById(instanceId)).thenReturn(consortiumHoldings);
    var holding1 = "{'id' : '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var holdingRecordEntity1 =
        HoldingsRecordEntity.builder()
            .jsonb(holding1)
            .id(holdingId1)
            .instanceId(instanceId)
            .build();
    when(holdingsRecordEntityTenantRepository.findByIdIn("member1", Set.of(holdingId1)))
        .thenReturn(List.of(holdingRecordEntity1));
    var holding2 = "{'id' : '1eaa1eef-1633-4c7e-af09-796315ebc576'}";
    var holdingRecordEntity2 =
        HoldingsRecordEntity.builder()
            .jsonb(holding2)
            .id(holdingId2)
            .instanceId(instanceId)
            .build();
    when(holdingsRecordEntityTenantRepository.findByIdIn("member2", Set.of(holdingId2)))
        .thenReturn(List.of(holdingRecordEntity2));
    var item = "{'barcode' : 'itemBarcode'}";
    var itemEntity =
        ItemEntity.builder().id(UUID.randomUUID()).holdingsRecordId(holdingId1).jsonb(item).build();
    when(itemEntityTenantRepository.findByHoldingsRecordIdIn("member1", Set.of(holdingId1)))
        .thenReturn(List.of(itemEntity));
    when(itemEntityTenantRepository.findByHoldingsRecordIdIn("member2", Set.of(holdingId2)))
        .thenReturn(List.of());
    when(userService.getUserName("central", user.getId())).thenReturn(user.getUsername());
    doNothing().when(entityManager).clear();
    when(permissionsValidator.isInstanceViewPermissionExists(any(String.class))).thenReturn(true);

    var instanceJson = new JSONObject();

    var instanceHrid = "instHrid";
    var jobExecutionId = UUID.randomUUID();
    holdingsItemsResolverService.retrieveHoldingsAndItemsByInstanceId(
        instanceJson, instanceId, instanceHrid, mappingProfile, jobExecutionId);

    var holdings = (JSONArray) instanceJson.get(HOLDINGS_KEY);
    assertEquals(2, holdings.size());
    verify(errorLogService)
        .saveGeneralErrorWithMessageValues(
            ErrorCode.ERROR_MESSAGE_INSTANCE_NO_AFFILIATION.getCode(),
            List.of(instanceId.toString(), user.getUsername(), "member3,member4"),
            jobExecutionId);
  }

  @Test
  void retrieveHoldingsAndItemsByInstanceIdForCentralTenant_whenNoPermissionForHoldingTest() {
    var user = new User();
    user.setId(UUID.randomUUID().toString());
    user.setUsername("username");

    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var holdingId1 = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");

    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(
        List.of(RecordTypes.INSTANCE, RecordTypes.HOLDINGS, RecordTypes.ITEM));

    var consortiumHolding1 = new ConsortiumHolding();
    consortiumHolding1.setInstanceId(instanceId.toString());
    consortiumHolding1.setId(holdingId1.toString());
    consortiumHolding1.setTenantId("member1");

    var consortiumHolding2 = new ConsortiumHolding();
    consortiumHolding2.setInstanceId(instanceId.toString());
    consortiumHolding2.setId(holdingId1.toString());
    consortiumHolding2.setTenantId("member2");

    var consortiumHolding3 = new ConsortiumHolding();
    consortiumHolding3.setInstanceId(instanceId.toString());
    consortiumHolding3.setId(holdingId1.toString());
    consortiumHolding3.setTenantId("member3");

    var consortiumHoldings = new ConsortiumHoldingCollection();
    consortiumHoldings.setHoldings(
        List.of(consortiumHolding1, consortiumHolding2, consortiumHolding3));

    when(folioExecutionContext.getTenantId()).thenReturn("central");
    when(folioExecutionContext.getUserId()).thenReturn(UUID.fromString(user.getId()));
    when(consortiaService.isCurrentTenantCentralTenant("central")).thenReturn(true);
    when(consortiaService.getAffiliatedTenants(isA(String.class), isA(String.class)))
        .thenReturn(List.of("member1", "member2", "member3"));
    when(searchConsortiumHoldings.getHoldingsById(instanceId)).thenReturn(consortiumHoldings);
    when(userService.getUserName("central", user.getId())).thenReturn(user.getUsername());
    var holding1 = "{'id' : '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var holdingRecordEntity1 =
        HoldingsRecordEntity.builder()
            .jsonb(holding1)
            .id(holdingId1)
            .instanceId(instanceId)
            .build();
    when(holdingsRecordEntityTenantRepository.findByIdIn("member1", Set.of(holdingId1)))
        .thenReturn(List.of(holdingRecordEntity1));
    when(permissionsValidator.isInstanceViewPermissionExists("member1")).thenReturn(true);
    when(permissionsValidator.isInstanceViewPermissionExists("member2")).thenReturn(false);
    when(permissionsValidator.isInstanceViewPermissionExists("member3")).thenReturn(false);

    var instanceJson = new JSONObject();

    var instanceHrid = "instHrid";
    var jobExecutionId = UUID.randomUUID();
    holdingsItemsResolverService.retrieveHoldingsAndItemsByInstanceId(
        instanceJson, instanceId, instanceHrid, mappingProfile, jobExecutionId);

    var holdings = (JSONArray) instanceJson.get(HOLDINGS_KEY);
    assertEquals(1, holdings.size());
    verify(errorLogService)
        .saveGeneralErrorWithMessageValues(
            ErrorCode.ERROR_INSTANCE_NO_PERMISSION.getCode(),
            List.of(instanceId.toString(), user.getUsername(), "member2,member3"),
            jobExecutionId);
  }

    @Test
  void testRetrieveHoldingsAndItemsByInstanceIdWhenNoUpdateNeededShouldExitEarly() {
    // TestMate-62ff2cbbfccf6f13577db8b16a2985d2
    // Given
    var instanceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var instanceHrid = "inst-123";
    var instance = new JSONObject();
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.INSTANCE));
    // When
    holdingsItemsResolverService.retrieveHoldingsAndItemsByInstanceId(instance, instanceId, instanceHrid, mappingProfile, jobExecutionId);
    // Then
    assertTrue(instance.isEmpty());
    assertTrue(!instance.containsKey(HOLDINGS_KEY));
    verify(consortiaService, never()).isCurrentTenantCentralTenant(any());
    verify(holdingsRecordEntityRepository, never()).findByInstanceIdIs(any());
    verify(searchConsortiumHoldings, never()).getHoldingsById(any());
    verify(folioExecutionContext, never()).getTenantId();
  }

    @Test
  void testRetrieveHoldingsAndItemsByInstanceIdWhenOnlyHoldingsRequestedShouldNotFetchItems() {
    // TestMate-602cbca5e4f75b0695c1319ef775ab32
    // Given
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var holdingId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var jobExecutionId = UUID.fromString("c0ffee00-0000-0000-0000-000000000000");
    var instanceHrid = "instHrid-123";
    var tenantId = "test-tenant";
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.HOLDINGS));
    var holdingJson = new JSONObject();
    holdingJson.put(ID_KEY, holdingId.toString());
    var holdingRecordEntity =
        HoldingsRecordEntity.builder()
            .id(holdingId)
            .instanceId(instanceId)
            .jsonb(holdingJson.toJSONString())
            .build();
    var instance = new JSONObject();
    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(consortiaService.isCurrentTenantCentralTenant(tenantId)).thenReturn(false);
    when(holdingsRecordEntityRepository.findByInstanceIdIs(instanceId))
        .thenReturn(List.of(holdingRecordEntity));
    doNothing().when(entityManager).clear();
    // When
    holdingsItemsResolverService.retrieveHoldingsAndItemsByInstanceId(
        instance, instanceId, instanceHrid, mappingProfile, jobExecutionId);
    // Then
    verify(holdingsRecordEntityRepository).findByInstanceIdIs(instanceId);
    verify(itemEntityTenantRepository, never()).findByHoldingsRecordIdIn(anyString(), anySet());
    verify(entityManager).clear();
    var holdingsArray = (JSONArray) instance.get(HOLDINGS_KEY);
    assertEquals(1, holdingsArray.size());
    var actualHolding = (JSONObject) holdingsArray.get(0);
    assertEquals(holdingId.toString(), actualHolding.getAsString(ID_KEY));
    assertEquals(instanceHrid, actualHolding.getAsString(INSTANCE_HRID_KEY));
    // The implementation always puts the ITEMS_KEY with an empty array if RecordTypes.ITEM is
    // missing
    assertTrue(actualHolding.containsKey(ITEMS_KEY));
    assertTrue(((JSONArray) actualHolding.get(ITEMS_KEY)).isEmpty());
  }

    @Test
  void testRetrieveHoldingsAndItemsByInstanceIdWhenJsonIsInvalidShouldSkipRecord() {
    // TestMate-24384d444f63e9c1e8e166b6387e3e81
    // Given
    var instanceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    var validHoldingId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    var malformedHoldingId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    var validItemId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    var malformedItemId = UUID.fromString("55555555-5555-5555-5555-555555555555");
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    var instanceHrid = "inst-001";
    var tenantId = "test-tenant";
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.HOLDINGS, RecordTypes.ITEM));
    var validHolding = HoldingsRecordEntity.builder()
        .id(validHoldingId)
        .instanceId(instanceId)
        .jsonb("{\"id\":\"" + validHoldingId + "\", \"hrid\":\"hold-001\"}")
        .build();
    // Use a string that is syntactically invalid JSON (e.g., missing closing brace) to trigger a ParseException.
    var malformedHolding = HoldingsRecordEntity.builder()
        .id(malformedHoldingId)
        .instanceId(instanceId)
        .jsonb("{\"id\": ")
        .build();
    var validItem = ItemEntity.builder()
        .id(validItemId)
        .holdingsRecordId(validHoldingId)
        .jsonb("{\"id\":\"" + validItemId + "\", \"barcode\":\"ABC\"}")
        .build();
    var malformedItem = ItemEntity.builder()
        .id(malformedItemId)
        .holdingsRecordId(validHoldingId)
        .jsonb("{\"barcode\": ")
        .build();
    var instance = new JSONObject();
    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(consortiaService.isCurrentTenantCentralTenant(tenantId)).thenReturn(false);
    when(holdingsRecordEntityRepository.findByInstanceIdIs(instanceId))
        .thenReturn(List.of(validHolding, malformedHolding));
    when(itemEntityTenantRepository.findByHoldingsRecordIdIn(anyString(), anySet()))
        .thenReturn(List.of(validItem, malformedItem));
    doNothing().when(entityManager).clear();
    // When
    holdingsItemsResolverService.retrieveHoldingsAndItemsByInstanceId(
        instance, instanceId, instanceHrid, mappingProfile, jobExecutionId);
    // Then
    assertTrue(instance.containsKey(HOLDINGS_KEY));
    var holdingsArray = (JSONArray) instance.get(HOLDINGS_KEY);
    assertEquals(1, holdingsArray.size());
    var actualHolding = (JSONObject) holdingsArray.get(0);
    assertEquals(validHoldingId.toString(), actualHolding.getAsString(ID_KEY));
    assertEquals(instanceHrid, actualHolding.getAsString(INSTANCE_HRID_KEY));
    assertTrue(actualHolding.containsKey(ITEMS_KEY));
    var itemsArray = (JSONArray) actualHolding.get(ITEMS_KEY);
    assertEquals(1, itemsArray.size());
    var actualItem = (JSONObject) itemsArray.get(0);
    assertEquals("ABC", actualItem.getAsString("barcode"));
    // entityManager.clear() is called once in retrieveHoldingsAndItemsByInstanceIdForLocalTenant
    // and once in addHoldingsAndItems because RecordTypes.ITEM is present in the mapping profile.
    verify(entityManager, org.mockito.Mockito.atLeastOnce()).clear();
  }

    @Test
  void testRetrieveHoldingsAndItemsByInstanceIdWhenCentralTenantShouldFilterOutCurrentTenantFromRemoteLoop() {
    // TestMate-c5dd4b356577d3edc9883933eb2c34f8
    // Given
    var instanceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    var jobExecutionId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    var userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    var centralTenantId = "central";
    var memberTenantId = "member1";
    var memberHoldingId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    var centralHoldingId = UUID.fromString("55555555-5555-5555-5555-555555555555");
    var instanceHrid = "inst-001";
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.HOLDINGS));
    var centralHolding = new ConsortiumHolding();
    centralHolding.setId(centralHoldingId.toString());
    centralHolding.setTenantId(centralTenantId);
    centralHolding.setInstanceId(instanceId.toString());
    var memberHolding = new ConsortiumHolding();
    memberHolding.setId(memberHoldingId.toString());
    memberHolding.setTenantId(memberTenantId);
    memberHolding.setInstanceId(instanceId.toString());
    var consortiumHoldings = new ConsortiumHoldingCollection();
    consortiumHoldings.setHoldings(List.of(centralHolding, memberHolding));
    var memberHoldingEntity = HoldingsRecordEntity.builder()
        .id(memberHoldingId)
        .instanceId(instanceId)
        .jsonb("{\"id\":\"" + memberHoldingId + "\"}")
        .build();
    when(folioExecutionContext.getTenantId()).thenReturn(centralTenantId);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(consortiaService.isCurrentTenantCentralTenant(centralTenantId)).thenReturn(true);
    when(consortiaService.getAffiliatedTenants(centralTenantId, userId.toString())).thenReturn(List.of(memberTenantId));
    when(searchConsortiumHoldings.getHoldingsById(instanceId)).thenReturn(consortiumHoldings);
    when(permissionsValidator.isInstanceViewPermissionExists(memberTenantId)).thenReturn(true);
    when(holdingsRecordEntityTenantRepository.findByIdIn(eq(memberTenantId), anySet())).thenReturn(List.of(memberHoldingEntity));
    doNothing().when(entityManager).clear();
    var instance = new JSONObject();
    // When
    holdingsItemsResolverService.retrieveHoldingsAndItemsByInstanceId(instance, instanceId, instanceHrid, mappingProfile, jobExecutionId);
    // Then
    verify(searchConsortiumHoldings).getHoldingsById(instanceId);
    verify(holdingsRecordEntityTenantRepository).findByIdIn(eq(memberTenantId), anySet());
    verify(holdingsRecordEntityTenantRepository, never()).findByIdIn(eq(centralTenantId), anySet());
    verify(entityManager).clear();
    assertThat(instance.containsKey(HOLDINGS_KEY)).isTrue();
    var holdingsArray = (JSONArray) instance.get(HOLDINGS_KEY);
    assertEquals(1, holdingsArray.size());
    var actualHolding = (JSONObject) holdingsArray.get(0);
    assertEquals(memberHoldingId.toString(), actualHolding.getAsString(ID_KEY));
    assertEquals(instanceHrid, actualHolding.getAsString(INSTANCE_HRID_KEY));
  }

    @Test
  void testRetrieveHoldingsAndItemsByInstanceIdWhenHoldingsAlreadyExistInJsonShouldAppend() {
    // TestMate-16c9a9a5d7b3f00a00cddd54e56ffe59
    // Given
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var jobExecutionId = UUID.fromString("c0ffee00-0000-0000-0000-000000000000");
    var instanceHrid = "inst-001";
    var tenantId = "test-tenant";
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.HOLDINGS));
    var instanceJson = new JSONObject();
    var existingHoldings = new JSONArray();
    var dummyHolding = new JSONObject();
    dummyHolding.put(ID_KEY, UUID.randomUUID().toString());
    existingHoldings.add(dummyHolding);
    instanceJson.put(HOLDINGS_KEY, existingHoldings);
    var holdingId1 = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var holdingId2 = UUID.fromString("9f9f9f9f-9f9f-9f9f-9f9f-9f9f9f9f9f9f");
    var holdingEntity1 = HoldingsRecordEntity.builder()
        .id(holdingId1)
        .instanceId(instanceId)
        .jsonb("{\"id\":\"" + holdingId1 + "\"}")
        .build();
    var holdingEntity2 = HoldingsRecordEntity.builder()
        .id(holdingId2)
        .instanceId(instanceId)
        .jsonb("{\"id\":\"" + holdingId2 + "\"}")
        .build();
    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(consortiaService.isCurrentTenantCentralTenant(tenantId)).thenReturn(false);
    when(holdingsRecordEntityRepository.findByInstanceIdIs(instanceId))
        .thenReturn(List.of(holdingEntity1, holdingEntity2));
    doNothing().when(entityManager).clear();
    // When
    holdingsItemsResolverService.retrieveHoldingsAndItemsByInstanceId(
        instanceJson, instanceId, instanceHrid, mappingProfile, jobExecutionId);
    // Then
    assertTrue(instanceJson.containsKey(HOLDINGS_KEY));
    var finalHoldingsArray = (JSONArray) instanceJson.get(HOLDINGS_KEY);
    assertEquals(3, finalHoldingsArray.size());
    var firstHolding = (JSONObject) finalHoldingsArray.get(0);
    assertEquals(dummyHolding.getAsString(ID_KEY), firstHolding.getAsString(ID_KEY));
    var secondHolding = (JSONObject) finalHoldingsArray.get(1);
    assertEquals(holdingId1.toString(), secondHolding.getAsString(ID_KEY));
    assertEquals(instanceHrid, secondHolding.getAsString(INSTANCE_HRID_KEY));
    assertTrue(secondHolding.containsKey(ITEMS_KEY));
    var thirdHolding = (JSONObject) finalHoldingsArray.get(2);
    assertEquals(holdingId2.toString(), thirdHolding.getAsString(ID_KEY));
    assertEquals(instanceHrid, thirdHolding.getAsString(INSTANCE_HRID_KEY));
    verify(entityManager).clear();
  }

    @Test
  void testRetrieveHoldingsAndItemsByInstanceIdWhenNoHoldingsFoundShouldNotAddKey() {
    // TestMate-a80ffa7f1cdabfc5cc08fc23bddf12a6
    // Given
    var instanceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var instanceHrid = "inst-123";
    var tenantId = "test-tenant";
    var instance = new JSONObject();
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.HOLDINGS));
    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(consortiaService.isCurrentTenantCentralTenant(tenantId)).thenReturn(false);
    when(holdingsRecordEntityRepository.findByInstanceIdIs(instanceId)).thenReturn(Collections.emptyList());
    doNothing().when(entityManager).clear();
    // When
    holdingsItemsResolverService.retrieveHoldingsAndItemsByInstanceId(instance, instanceId, instanceHrid, mappingProfile, jobExecutionId);
    // Then
    assertTrue(instance.isEmpty());
    assertTrue(!instance.containsKey(HOLDINGS_KEY));
    verify(holdingsRecordEntityRepository).findByInstanceIdIs(instanceId);
    verify(entityManager).clear();
    verify(itemEntityTenantRepository, never()).findByHoldingsRecordIdIn(anyString(), anySet());
    verify(searchConsortiumHoldings, never()).getHoldingsById(instanceId);
  }
}
