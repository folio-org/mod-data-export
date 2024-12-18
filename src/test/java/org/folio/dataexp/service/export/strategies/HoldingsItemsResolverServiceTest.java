package org.folio.dataexp.service.export.strategies;

import jakarta.persistence.EntityManager;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.folio.dataexp.service.export.Constants.HOLDINGS_KEY;
import static org.folio.dataexp.service.export.Constants.ID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_HRID_KEY;
import static org.folio.dataexp.service.export.Constants.ITEMS_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoldingsItemsResolverServiceTest {

  @Mock
  private HoldingsRecordEntityRepository holdingsRecordEntityRepository;
  @Mock
  private HoldingsRecordEntityTenantRepository holdingsRecordEntityTenantRepository;
  @Mock
  private ItemEntityTenantRepository itemEntityTenantRepository;
  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private SearchConsortiumHoldings searchConsortiumHoldings;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private UserService userService;
  @Mock
  private ErrorLogService errorLogService;
  @Mock
  private EntityManager entityManager;
  @Mock
  private PermissionsValidator permissionsValidator;

  @InjectMocks
  private HoldingsItemsResolverService holdingsItemsResolverService;

  @Test
  void retrieveHoldingsAndItemsByInstanceIdForLocalTenantTest() {
    var holding = "{'id' : '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var holdingId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var instanceHrid = "instHrid";
    var item = "{'barcode' : 'itemBarcode'}";
    var holdingRecordEntity = HoldingsRecordEntity.builder().jsonb(holding).id(holdingId).instanceId(instanceId).build();
       var itemEntity = ItemEntity.builder().id(UUID.randomUUID()).holdingsRecordId(holdingId).jsonb(item).build();
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.INSTANCE, RecordTypes.HOLDINGS, RecordTypes.ITEM));

    when(folioExecutionContext.getTenantId()).thenReturn("localTenant");
    when(holdingsRecordEntityRepository.findByInstanceIdIs(instanceId)).thenReturn(List.of(holdingRecordEntity));
    when(itemEntityTenantRepository.findByHoldingsRecordIdIn(anyString(), anySet())).thenReturn(List.of(itemEntity));
    doNothing().when(entityManager).clear();

    var instanceJson = new JSONObject();

    holdingsItemsResolverService.retrieveHoldingsAndItemsByInstanceId(instanceJson, instanceId, instanceHrid, mappingProfile, UUID.randomUUID());

    var holdingJson = (JSONObject)((JSONArray)instanceJson .get(HOLDINGS_KEY)).get(0);
    assertEquals("instHrid", holdingJson.getAsString(INSTANCE_HRID_KEY));
    assertEquals(holdingId.toString(), holdingJson.getAsString(ID_KEY));

    var itemJsonArray = (JSONArray)holdingJson.get(ITEMS_KEY);
    assertEquals(1, itemJsonArray.size());
  }

  @Test
  void retrieveHoldingsAndItemsByInstanceIdForCentralTenantTest() {
    var jobExecutionId = UUID.randomUUID();
    var user = new User();
    user.setId(UUID.randomUUID().toString());
    user.setUsername("username");

    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var instanceHrid = "instHrid";
    var holding1 = "{'id' : '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var holdingId1 = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");

    var item = "{'barcode' : 'itemBarcode'}";
    var holdingRecordEntity1 = HoldingsRecordEntity.builder().jsonb(holding1).id(holdingId1).instanceId(instanceId).build();
    var itemEntity = ItemEntity.builder().id(UUID.randomUUID()).holdingsRecordId(holdingId1).jsonb(item).build();

    var holding2 = "{'id' : '1eaa1eef-1633-4c7e-af09-796315ebc576'}";
    var holdingId2 = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var holdingRecordEntity2 = HoldingsRecordEntity.builder().jsonb(holding2).id(holdingId2).instanceId(instanceId).build();

    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.INSTANCE, RecordTypes.HOLDINGS, RecordTypes.ITEM));

    var consortiumHolding1 = new ConsortiumHolding();
    consortiumHolding1.setInstanceId(instanceId.toString());
    consortiumHolding1.setId(holdingId1.toString());
    consortiumHolding1.setTenantId("member1");

    var consortiumHolding2 = new ConsortiumHolding();
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
    consortiumHoldings.setHoldings(List.of(consortiumHolding1, consortiumHolding2, consortiumHolding3, consortiumHolding4));

    HashMap<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put("header", List.of("value"));

    when(folioExecutionContext.getTenantId()).thenReturn("central");
    when(folioExecutionContext.getUserId()).thenReturn(UUID.fromString(user.getId()));
    when(consortiaService.isCurrentTenantCentralTenant("central")).thenReturn(true);
    when(consortiaService.getAffiliatedTenants(isA(String.class), isA(String.class))).thenReturn(List.of("member1", "member2"));
    when(searchConsortiumHoldings.getHoldingsById(instanceId)).thenReturn(consortiumHoldings);
    when(holdingsRecordEntityTenantRepository.findByIdIn("member1", Set.of(holdingId1))).thenReturn(List.of(holdingRecordEntity1));
    when(holdingsRecordEntityTenantRepository.findByIdIn("member2", Set.of(holdingId2))).thenReturn(List.of(holdingRecordEntity2));
    when(itemEntityTenantRepository.findByHoldingsRecordIdIn("member1", Set.of(holdingId1))).thenReturn(List.of(itemEntity));
    when(itemEntityTenantRepository.findByHoldingsRecordIdIn("member2", Set.of(holdingId2))).thenReturn(List.of());
    when(userService.getUserName("central", user.getId())).thenReturn(user.getUsername());
    doNothing().when(entityManager).clear();
    when(permissionsValidator.checkInstanceViewPermissions(any(String.class))).thenReturn(true);

    var instanceJson = new JSONObject();

    holdingsItemsResolverService.retrieveHoldingsAndItemsByInstanceId(instanceJson, instanceId, instanceHrid, mappingProfile, jobExecutionId);

    var holdings = (JSONArray)instanceJson.get(HOLDINGS_KEY);
    assertEquals(2, holdings.size());
    verify(errorLogService).saveGeneralErrorWithMessageValues(ErrorCode.ERROR_MESSAGE_INSTANCE_NO_AFFILIATION.getCode(), List.of(instanceId.toString(), user.getUsername(), "member3,member4"), jobExecutionId);
  }

  @Test
  void retrieveHoldingsAndItemsByInstanceIdForCentralTenant_whenNoPermissionForHoldingTest() {
    var jobExecutionId = UUID.randomUUID();
    var user = new User();
    user.setId(UUID.randomUUID().toString());
    user.setUsername("username");

    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var instanceHrid = "instHrid";
    var holdingId1 = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");

    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.INSTANCE, RecordTypes.HOLDINGS, RecordTypes.ITEM));

    var consortiumHolding1 = new ConsortiumHolding();
    consortiumHolding1.setInstanceId(instanceId.toString());
    consortiumHolding1.setId(holdingId1.toString());
    consortiumHolding1.setTenantId("member1");

    var consortiumHoldings = new ConsortiumHoldingCollection();
    consortiumHoldings.setHoldings(List.of(consortiumHolding1));

    when(folioExecutionContext.getTenantId()).thenReturn("central");
    when(folioExecutionContext.getUserId()).thenReturn(UUID.fromString(user.getId()));
    when(consortiaService.isCurrentTenantCentralTenant("central")).thenReturn(true);
    when(consortiaService.getAffiliatedTenants(isA(String.class), isA(String.class))).thenReturn(List.of("member1", "member2"));
    when(searchConsortiumHoldings.getHoldingsById(instanceId)).thenReturn(consortiumHoldings);
    when(userService.getUserName("central", user.getId())).thenReturn(user.getUsername());
    when(permissionsValidator.checkInstanceViewPermissions(any(String.class))).thenReturn(false);

    var instanceJson = new JSONObject();

    holdingsItemsResolverService.retrieveHoldingsAndItemsByInstanceId(instanceJson, instanceId, instanceHrid, mappingProfile, jobExecutionId);

    var holdings = (JSONArray)instanceJson.get(HOLDINGS_KEY);
    Assertions.assertNull(holdings);
    verify(errorLogService).saveGeneralErrorWithMessageValues(ErrorCode.ERROR_INSTANCE_NO_PERMISSION.getCode(), List.of(instanceId.toString(), user.getUsername(), "member1"), jobExecutionId);
  }
}

