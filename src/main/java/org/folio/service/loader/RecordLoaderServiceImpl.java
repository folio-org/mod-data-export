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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of #RecordLoaderService that uses blocking http client.
 */
@Service
public class RecordLoaderServiceImpl implements RecordLoaderService {

  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  public static final String CONSORTIUM_MARC_INSTANCE_SOURCE = "CONSORTIUM-MARC";
  private static final String ID_FIELD = "id";
  private static final String SOURCE_RECORDS_FIELD = "sourceRecords";
  private static final String TOTAL_RECORDS_FIELD = "totalRecords";
  private static final String INSTANCES = "instances";
  private static final String HOLDINGS_RECORDS = "holdingsRecords";

  private static final Map<AbstractExportStrategy.EntityType, String> entityIdMap = Map.of(
    AbstractExportStrategy.EntityType.INSTANCE, "instanceId",
    AbstractExportStrategy.EntityType.HOLDING, "holdingsId",
    AbstractExportStrategy.EntityType.AUTHORITY, "authorityId"
  );

  private final SourceRecordStorageClient srsClient;
  private final InventoryClient inventoryClient;

  public RecordLoaderServiceImpl(@Autowired SourceRecordStorageClient srsClient, @Autowired InventoryClient inventoryClient) {
    this.srsClient = srsClient;
    this.inventoryClient = inventoryClient;
  }

  @Override
  public SrsLoadResult loadMarcRecordsBlocking(List<String> uuids, AbstractExportStrategy.EntityType idType, String jobExecutionId, OkapiConnectionParams okapiConnectionParams) {
    Optional<JsonObject> optionalRecords;
    if (AbstractExportStrategy.EntityType.INSTANCE == idType) {
      optionalRecords = getMarcRecordsForInstancesByIds(uuids, jobExecutionId, okapiConnectionParams);
    } else {
      optionalRecords = srsClient.getRecordsByIds(uuids, idType, jobExecutionId, okapiConnectionParams);
    }
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
    Optional<JsonObject> optionalRecords = inventoryClient.getInstancesWithPrecedingSucceedingTitlesByIds(new ArrayList<>(instanceIds), jobExecutionId, params, partitionSize);
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
  public LoadResult getHoldingsById(List<String> holdingIds, String jobExecutionId, OkapiConnectionParams params, int partitionSize) {
    Optional<JsonObject> optionalRecords = inventoryClient.getHoldingsByIds(holdingIds, jobExecutionId, params, partitionSize);
    LoadResult holdingsLoadResult = new LoadResult();
    holdingsLoadResult.setEntityType(AbstractExportStrategy.EntityType.HOLDING);
    if (optionalRecords.isPresent()) {
      populateLoadResultFromInventory(holdingIds, optionalRecords.get(), holdingsLoadResult);
    } else {
      holdingsLoadResult.setNotFoundEntitiesUUIDs(holdingIds);
    }
    return holdingsLoadResult;
  }

  private Optional<JsonObject> getMarcRecordsForInstancesByIds(List<String> uuids, String jobExecutionId, OkapiConnectionParams okapiConnectionParams) {
    var consortiumUUIDs = new ArrayList<String>();
    Optional<JsonObject> response = inventoryClient.getInstancesByIds(uuids, jobExecutionId, okapiConnectionParams, CONSORTIUM_MARC_INSTANCE_SOURCE);
    if (response.isPresent()) {
      var inventoryRecords = response.get().getJsonArray(INSTANCES);
      inventoryRecords.forEach(obj -> {
        if (obj instanceof JsonObject) {
          var jsonObject = (JsonObject) obj;
          consortiumUUIDs.add(jsonObject.getString(ID_FIELD));
        }});
    }
    Optional<JsonObject> optionalRecords = Optional.empty();
    uuids = uuids.stream().filter(uuid -> !consortiumUUIDs.contains(uuid)).collect(Collectors.toList());
    if (!uuids.isEmpty()) optionalRecords = srsClient.getRecordsByIds(uuids, AbstractExportStrategy.EntityType.INSTANCE, jobExecutionId, okapiConnectionParams);
    if (!consortiumUUIDs.isEmpty()) {
      Optional<JsonObject> optionalConsortiumRecords = srsClient.getRecordsByIds(consortiumUUIDs, AbstractExportStrategy.EntityType.INSTANCE, jobExecutionId, okapiConnectionParams, true);
      if (optionalConsortiumRecords.isPresent()) {
        if (optionalRecords.isEmpty()) {
          optionalRecords = optionalConsortiumRecords;
        } else {
          var consortiumSourceRecords = optionalConsortiumRecords.get().getJsonArray(SOURCE_RECORDS_FIELD);
          var records = optionalRecords.get();
          records.getJsonArray(SOURCE_RECORDS_FIELD).addAll(consortiumSourceRecords);
          var totalSize = records.getInteger(TOTAL_RECORDS_FIELD);
          records.put(TOTAL_RECORDS_FIELD, totalSize);
        }
      }
    }
    return optionalRecords;
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
    JsonArray records = underlyingRecords.getJsonArray(SOURCE_RECORDS_FIELD);
    List<JsonObject> marcRecords = new ArrayList<>();
    Set<String> singleRecordIdentifiersSet = new HashSet<>(uuids);
    for (Object o : records) {
      JsonObject record = (JsonObject) o;
      marcRecords.add(record);
      JsonObject externalIdsHolder = record.getJsonObject("externalIdsHolder");
      if (externalIdsHolder != null) {
        String key = entityIdMap.get(entityType);
        String id = externalIdsHolder.getString(key);
        singleRecordIdentifiersSet.remove(id);
      }
    }
    loadResult.setUnderlyingMarcRecords(marcRecords);
    loadResult.setIdsWithoutSrs(new ArrayList<>(singleRecordIdentifiersSet));
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
