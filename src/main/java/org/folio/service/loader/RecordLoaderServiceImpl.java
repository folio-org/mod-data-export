package org.folio.service.loader;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.folio.clients.InventoryClient;
import org.folio.clients.SourceRecordStorageClient;
import org.folio.service.manager.export.strategy.AbstractExportStrategy;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of #RecordLoaderService that uses blocking http client.
 */
@Service
public class RecordLoaderServiceImpl implements RecordLoaderService {

  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  private static final String INSTANCES = "instances";
  private static final String HOLDINGS_RECORDS = "holdingsRecords";

  private SourceRecordStorageClient srsClient;
  private InventoryClient inventoryClient;

  public RecordLoaderServiceImpl(@Autowired SourceRecordStorageClient srsClient, @Autowired InventoryClient inventoryClient) {
    this.srsClient = srsClient;
    this.inventoryClient = inventoryClient;
  }

  @Override
  public SrsLoadResult loadMarcRecordsBlocking(List<String> uuids, AbstractExportStrategy.EntityType idType, String jobExecutionId, OkapiConnectionParams okapiConnectionParams) {
    Optional<JsonObject> optionalRecords = srsClient.getRecordsByIds(uuids, idType, jobExecutionId, okapiConnectionParams);
    SrsLoadResult srsLoadResult = new SrsLoadResult();
    if (optionalRecords.isPresent()) {
      populateLoadResultFromSRS(uuids, optionalRecords.get(), srsLoadResult, idType);
    } else {
      srsLoadResult.setIdsWithoutSrs(uuids);
    }
    return srsLoadResult;
  }

  @Override
  public LoadResult loadInventoryInstancesBlocking(Collection<String> instanceIds, String jobExecutionId, OkapiConnectionParams params, int partitionSize) {
    Optional<JsonObject> optionalRecords = inventoryClient.getInstancesByIds(new ArrayList<>(instanceIds), jobExecutionId, params, partitionSize);
    LoadResult loadResult = new LoadResult();
    loadResult.setEntityType(AbstractExportStrategy.EntityType.INSTANCE);
    if (optionalRecords.isPresent()) {
      populateLoadResultFromInventory(instanceIds, optionalRecords.get(), loadResult);
    } else {
      loadResult.setNotFoundEntitiesUUIDs(instanceIds);
    }
    return loadResult;
  }

  @Override
  public LoadResult getHoldingsById(List<String> holdingIds, String jobExecutionId, OkapiConnectionParams params) {
    Optional<JsonObject> optionalRecords = inventoryClient.getHoldingsByIds(holdingIds, jobExecutionId, params);
    LoadResult holdingsLoadResult = new LoadResult();
    holdingsLoadResult.setEntityType(AbstractExportStrategy.EntityType.HOLDING);
    if (optionalRecords.isPresent()) {
      populateLoadResultFromInventory(holdingIds, optionalRecords.get(), holdingsLoadResult);
    } else {
      holdingsLoadResult.setNotFoundEntitiesUUIDs(holdingIds);
    }
    return holdingsLoadResult;
  }


  private void populateLoadResultFromInventory(Collection<String> entityIds, JsonObject entities, LoadResult loadResult) {
    List<JsonObject> inventoryRecords = new ArrayList<>();
    Set<String> entitiesIdentifiersSet = new HashSet<>(entityIds);
    String jsonArrayKey = loadResult.getEntityType().equals(AbstractExportStrategy.EntityType.INSTANCE) ? INSTANCES : HOLDINGS_RECORDS;
    for (String entityId : entityIds) {
      for (Object entity : entities.getJsonArray(jsonArrayKey)) {
        JsonObject record = (JsonObject) entity;
        if (record.getValue("id").equals(entityId)) {
          entitiesIdentifiersSet.remove(entityId);
          inventoryRecords.add(record);
        }
      }
    }
    loadResult.setEntities(inventoryRecords);
    loadResult.setNotFoundEntitiesUUIDs(entitiesIdentifiersSet);
  }

  private void populateLoadResultFromSRS(List<String> uuids, JsonObject underlyingRecords, SrsLoadResult loadResult, AbstractExportStrategy.EntityType entityType) {
    JsonArray records = underlyingRecords.getJsonArray("sourceRecords");
    List<JsonObject> marcRecords = new ArrayList<>();
    Set<String> singleInstanceIdentifiersSet = new HashSet<>(uuids);
    for (Object o : records) {
      JsonObject record = (JsonObject) o;
      marcRecords.add(record);
      JsonObject externalIdsHolder = record.getJsonObject("externalIdsHolder");
      if (externalIdsHolder != null) {
        String key = entityType.equals(AbstractExportStrategy.EntityType.INSTANCE) ? "instanceId" : "holdingsId";
        String id = externalIdsHolder.getString(key);
        singleInstanceIdentifiersSet.remove(id);
      }
    }
    loadResult.setUnderlyingMarcRecords(marcRecords);
    loadResult.setIdsWithoutSrs(new ArrayList<>(singleInstanceIdentifiersSet));
  }

  /**
   * Method converts Json Response obtained from external services to a list of json Objects
   * @param field field to read from the JsonObject
   * @param jsonObject the response payload to convert
   * @return
   */
  private List<JsonObject> populateLoadResultFromResponse(String field, JsonObject jsonObject) {
    List<JsonObject> result = new ArrayList<>();
    JsonArray jsonArray = jsonObject.getJsonArray(field);
    LOGGER.debug("Populating result from external response {}", jsonArray);
    for (Object instance : jsonArray) {
      result.add(JsonObject.mapFrom(instance));
    }
    return result;
  }

  @Override
  public List<JsonObject> getHoldingsForInstance(String instanceId, String jobExecutionId, OkapiConnectionParams params) {
    Optional<JsonObject> optionalRecords = inventoryClient.getHoldingsByInstanceId(instanceId, jobExecutionId, params);
    return optionalRecords.map(holdings -> populateLoadResultFromResponse(HOLDINGS_RECORDS, holdings)).orElseGet(ArrayList::new);
  }

  @Override
  public List<JsonObject> getAllItemsForHolding(List<String> holdingIds, String jobExecutionId, OkapiConnectionParams params) {
    Optional<JsonObject> optionalRecords = inventoryClient.getItemsByHoldingIds(holdingIds, jobExecutionId, params);
    return optionalRecords.map(items -> populateLoadResultFromResponse("items", items)).orElseGet(ArrayList::new);
  }
}
