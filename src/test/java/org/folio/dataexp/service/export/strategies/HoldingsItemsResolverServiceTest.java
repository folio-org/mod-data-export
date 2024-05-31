package org.folio.dataexp.service.export.strategies;

import jakarta.persistence.EntityManager;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.folio.dataexp.client.SearchConsortiumHoldings;
import org.folio.dataexp.domain.dto.ConsortiumHolding;
import org.folio.dataexp.domain.dto.ConsortiumHoldingCollection;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.ItemEntity;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.spring.FolioExecutionContext;
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
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoldingsItemsResolverServiceTest {

  @Mock
  private HoldingsRecordEntityRepository holdingsRecordEntityRepository;
  @Mock
  private ItemEntityRepository itemEntityRepository;
  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private SearchConsortiumHoldings searchConsortiumHoldings;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private EntityManager entityManager;

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

    when(holdingsRecordEntityRepository.findByInstanceIdIs(instanceId)).thenReturn(List.of(holdingRecordEntity));
    when(itemEntityRepository.findByHoldingsRecordIdIn(anySet())).thenReturn(List.of(itemEntity));
    doNothing().when(entityManager).clear();

    var instanceJson = new JSONObject();

    holdingsItemsResolverService.retrieveHoldingsAndItemsByInstanceId(instanceJson, instanceId, instanceHrid, mappingProfile);

    var holdingJson = (JSONObject)((JSONArray)instanceJson .get(HOLDINGS_KEY)).get(0);
    assertEquals("instHrid", holdingJson.getAsString(INSTANCE_HRID_KEY));
    assertEquals(holdingId.toString(), holdingJson.getAsString(ID_KEY));

    var itemJsonArray = (JSONArray)holdingJson.get(ITEMS_KEY);
    assertEquals(1, itemJsonArray.size());
  }

  @Test
  void retrieveHoldingsAndItemsByInstanceIdForCentralTenantTest() {
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

    var consortiumHoldings = new ConsortiumHoldingCollection();
    consortiumHoldings.setHoldings(List.of(consortiumHolding1, consortiumHolding2));

    HashMap<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put("header", List.of("value"));

    when(folioExecutionContext.getTenantId()).thenReturn("central");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);
    when(consortiaService.isCurrentTenantCentralTenant()).thenReturn(true);
    when(searchConsortiumHoldings.getHoldingsById(instanceId)).thenReturn(consortiumHoldings);
    when(holdingsRecordEntityRepository.findByIdIn(Set.of(holdingId1))).thenReturn(List.of(holdingRecordEntity1));
    when(holdingsRecordEntityRepository.findByIdIn(Set.of(holdingId2))).thenReturn(List.of(holdingRecordEntity2));
    when(itemEntityRepository.findByHoldingsRecordIdIn(anySet())).thenReturn(List.of(itemEntity));
    doNothing().when(entityManager).clear();

    var instanceJson = new JSONObject();

    holdingsItemsResolverService.retrieveHoldingsAndItemsByInstanceId(instanceJson, instanceId, instanceHrid, mappingProfile);

    var holdings = (JSONArray)instanceJson.get(HOLDINGS_KEY);
    assertEquals(2, holdings.size());
  }
}

