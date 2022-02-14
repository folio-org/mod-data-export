package org.folio.service.mapping.converter;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.service.loader.LoadResult;
import org.folio.service.loader.RecordLoaderService;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.folio.service.manager.export.ExportManagerImpl.INVENTORY_LOAD_PARTITION_SIZE;

public class RecordConverter {

  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup()
    .lookupClass());

  private static final int SINGLE_HOLDING_POSITION = 0;
  private static final int PARTITION_SIZE = 1;

  @Autowired
  private RecordLoaderService recordLoaderService;

  protected boolean isTransformationRequired(MappingProfile mappingProfile) {
    List<RecordType> recordTypes = mappingProfile.getRecordTypes();
    return recordTypes.contains(RecordType.HOLDINGS) || recordTypes.contains(RecordType.ITEM);
  }

  /**
   * Fetches holdings if Transformations specify Record type "HOLDINGS", and
   * also appends items to it if recordtype contains "ITEM"
   */
  protected void fetchHoldingsAndItems(MappingProfile mappingProfile, OkapiConnectionParams params, String recordId,
                                       String recordHrId, RecordType recordType, JsonObject appendHoldingsItems, String jobExecutionId) {
    if (isTransformationRequired(mappingProfile)) {
      LOGGER.debug("Fetching holdings/items for instance");
      List<JsonObject> holdings;
      if (recordType.equals(RecordType.INSTANCE)) {
        holdings = recordLoaderService.getHoldingsForInstance(recordId, jobExecutionId, params);
      } else {
        LoadResult holding = recordLoaderService.getHoldingsById(Collections.singletonList(recordId), jobExecutionId, params, PARTITION_SIZE);
        holdings = holding.getEntities();
      }
      if (mappingProfile.getRecordTypes().contains(RecordType.ITEM) && CollectionUtils.isNotEmpty(holdings)) {
        List<String> holdingIds = holdings.stream().map(holding -> holding.getString("id")).collect(Collectors.toList());
        List<JsonObject> items = recordLoaderService.getAllItemsForHolding(holdingIds, jobExecutionId, params);
        for (JsonObject holding : holdings) {
          String holdingId = holding.getString("id");
          List<JsonObject> currentItems = items.stream()
            .filter(item -> holdingId.equals(item.getString("holdingsRecordId")))
            .collect(Collectors.toList());
          holding.put("items", currentItems);
        }
      }
      if (recordType.equals(RecordType.INSTANCE)) {
        holdings.forEach(holding -> holding.put("instanceHrId", recordHrId));
      }
      appendHoldingsItems.put("holdings", new JsonArray(holdings));
    }
  }

  protected LoadResult fetchInstancesForHoldings(List<JsonObject> holdings, String jobExecutionId, OkapiConnectionParams params) {
    List<String> instanceIds = getInstanceIdsFromHoldings(holdings);
    return recordLoaderService.loadInventoryInstancesBlocking(instanceIds, jobExecutionId, params, INVENTORY_LOAD_PARTITION_SIZE);
  }

  private List<String> getInstanceIdsFromHoldings(List<JsonObject> holdings) {
    return holdings.stream()
      .map(JsonObject.class::cast)
      .map(json -> json.getString("instanceId"))
      .collect(Collectors.toList());
  }

  /**
   * Fetches items if Transformations specify Record type "ITEM"
   */
  protected void fetchItemsIfRequired(MappingProfile mappingProfile, OkapiConnectionParams params, String holdingUUID, JsonObject appendHoldingsItems, String jobExecutionId) {
    LOGGER.debug("Fetching items for holdings MFHD export");
    if (mappingProfile.getRecordTypes().contains(RecordType.ITEM)) {
      List<JsonObject> items = recordLoaderService.getAllItemsForHolding(Collections.singletonList(holdingUUID), jobExecutionId, params);
      JsonArray holdings = appendHoldingsItems.getJsonArray("holdings");
      if (!holdings.isEmpty()) {
        holdings.getJsonObject(SINGLE_HOLDING_POSITION).put("items", items);
      }
    }
  }

}
