package org.folio.dataexp.service.export.strategies;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.folio.dataexp.client.SearchConsortiumHoldings;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.ConsortiumHolding;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.ItemEntity;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.folio.dataexp.service.export.Constants.HOLDINGS_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_HRID_KEY;
import static org.folio.dataexp.service.export.Constants.ITEMS_KEY;
import static org.folio.dataexp.service.export.strategies.AbstractExportStrategy.getAsJsonObject;
import static org.folio.dataexp.util.FolioExecutionContextUtils.prepareContextForTenant;


@Log4j2
@Service
@AllArgsConstructor
public class HoldingsItemsResolverService {

  private final static String ERROR_USER_NOT_HAVE_PERMISSIONS_FOR_HOLDINGS = "%s - the user %s does not have permissions to access the holdings record in %s data tenant.";

  private final HoldingsRecordEntityRepository holdingsRecordEntityRepository;
  private final ItemEntityRepository itemEntityRepository;
  private final SearchConsortiumHoldings searchConsortiumHoldings;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;
  private final ConsortiaService consortiaService;
  private final UserClient userClient;

  @PersistenceContext
  protected EntityManager entityManager;

  public void retrieveHoldingsAndItemsByInstanceId(JSONObject instance, UUID instanceId, String instanceHrid, MappingProfile mappingProfile) {
    if (!isNeedUpdateWithHoldingsOrItems(mappingProfile)) {
      return;
    }
    if (consortiaService.isCurrentTenantCentralTenant()) {
      retrieveHoldingsAndItemsByInstanceIdForCentralTenant(instance, instanceId, instanceHrid, mappingProfile);
    } else {
      retrieveHoldingsAndItemsByInstanceIdForLocalTenant(instance, instanceId, instanceHrid, mappingProfile);
    }
  }

  private void retrieveHoldingsAndItemsByInstanceIdForLocalTenant(JSONObject instance, UUID instanceId, String instanceHrid, MappingProfile mappingProfile) {
    var holdingsEntities = holdingsRecordEntityRepository.findByInstanceIdIs(instanceId);
    entityManager.clear();
    addHoldingsAndItems(instance, holdingsEntities, instanceHrid, mappingProfile);
  }

  private void retrieveHoldingsAndItemsByInstanceIdForCentralTenant(JSONObject instance, UUID instanceId, String instanceHrid, MappingProfile mappingProfile) {
    var consortiumHoldings = searchConsortiumHoldings.getHoldingsById(instanceId).getHoldings();
    Map<String, List<String>> consortiaHoldingsIdsPerTenant = consortiumHoldings.stream()
      .filter(h -> !folioExecutionContext.getTenantId().equals(h.getTenantId()))
      .collect(Collectors.groupingBy(ConsortiumHolding::getTenantId, Collectors.mapping(ConsortiumHolding::getId, Collectors.toList())));
    var userTenants = consortiaService.getAffiliatedTenants(folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString());
    for (var entry : consortiaHoldingsIdsPerTenant.entrySet()) {
      var localTenant = entry.getKey();
      var holdingsIds = entry.getValue().stream().map(UUID::fromString).collect(Collectors.toSet());
      if (userTenants.contains(localTenant)) {
        try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(localTenant, folioModuleMetadata, folioExecutionContext))) {
          var holdingsEntities = holdingsRecordEntityRepository.findByIdIn(holdingsIds);
          entityManager.clear();
          addHoldingsAndItems(instance, holdingsEntities, instanceHrid, mappingProfile);
        }
      } else {
        var userName = userClient.getUserById(folioExecutionContext.getUserId().toString()).getUsername();
        holdingsIds.forEach(holdingId -> {
          var errorMessage = String.format(ERROR_USER_NOT_HAVE_PERMISSIONS_FOR_HOLDINGS, holdingId, userName, localTenant);
          log.error(errorMessage);
        });
      }
    }
  }

  private void addHoldingsAndItems(JSONObject jsonToUpdateWithHoldingsAndItems, List<HoldingsRecordEntity> holdingsEntities,
                                   String instanceHrid, MappingProfile mappingProfile) {
    if (holdingsEntities.isEmpty()) {
      return;
    }
    HashMap<UUID, List<ItemEntity>> itemsByHoldingId = new HashMap<>();
    if (mappingProfile.getRecordTypes().contains(RecordTypes.ITEM)) {
      var ids = holdingsEntities.stream().map(HoldingsRecordEntity::getId).collect(Collectors.toSet());
      itemsByHoldingId  = itemEntityRepository.findByHoldingsRecordIdIn(ids)
        .stream().collect(Collectors.groupingBy(ItemEntity::getHoldingsRecordId,
          HashMap::new, Collectors.mapping(itemEntity -> itemEntity, Collectors.toList())));
      entityManager.clear();
    }
    var holdingsJsonArray = new JSONArray();
    for (var holdingsEntity : holdingsEntities) {
      var itemJsonArray = new JSONArray();
      var itemEntities = itemsByHoldingId.getOrDefault(holdingsEntity.getId(), new ArrayList<>());
      itemEntities.forEach(itemEntity -> {
        var itemJsonOpt = getAsJsonObject(itemEntity.getJsonb());
        if (itemJsonOpt.isPresent()) {
          itemJsonArray.add(itemJsonOpt.get());
        } else {
          log.error("addItemsToHolding:: error converting to json item by id {}", itemEntity.getId());
        }
      });
      var holdingJsonOpt = getAsJsonObject(holdingsEntity.getJsonb());
      if (holdingJsonOpt.isPresent()) {
        var holdingJson = holdingJsonOpt.get();
        holdingJson.put(ITEMS_KEY, itemJsonArray);
        holdingJson.put(INSTANCE_HRID_KEY, instanceHrid);
        holdingsJsonArray.add(holdingJson);
      } else {
        log.error("addItemsToHolding:: error converting to json holding by id {}", holdingsEntity.getId());
      }
    }
    if (jsonToUpdateWithHoldingsAndItems.containsKey(HOLDINGS_KEY)) {
      var existHoldingsJsonArray = (JSONArray)jsonToUpdateWithHoldingsAndItems.get(HOLDINGS_KEY);
      existHoldingsJsonArray.addAll(holdingsJsonArray);
    } else {
      jsonToUpdateWithHoldingsAndItems.put(HOLDINGS_KEY, holdingsJsonArray);
    }
  }

  public boolean isNeedUpdateWithHoldingsOrItems(MappingProfile mappingProfile) {
    var recordTypes = mappingProfile.getRecordTypes();
    return recordTypes.contains(RecordTypes.HOLDINGS) || recordTypes.contains(RecordTypes.ITEM);
  }
}
