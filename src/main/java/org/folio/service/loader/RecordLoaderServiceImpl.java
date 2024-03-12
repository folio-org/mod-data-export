package org.folio.service.loader;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.folio.service.manager.export.strategy.AbstractExportStrategy;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.apache.commons.collections4.ListUtils.union;

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
  private static final String AUTHORITIES = "authorities";
  private static final String HOLDINGS_RECORDS = "holdingsRecords";

  private static final Map<AbstractExportStrategy.EntityType, String> entityIdMap = Map.of(
    AbstractExportStrategy.EntityType.INSTANCE, "instanceId",
    AbstractExportStrategy.EntityType.HOLDING, "holdingsId",
    AbstractExportStrategy.EntityType.AUTHORITY, "authorityId"
  );

  private final SourceRecordStorageClient srsClient;
  private final InventoryClient inventoryClient;
  private final AuthorityClient authorityClient;

  public RecordLoaderServiceImpl(@Autowired SourceRecordStorageClient srsClient, @Autowired InventoryClient inventoryClient, @Autowired AuthorityClient authorityClient) {
    this.srsClient = srsClient;
    this.inventoryClient = inventoryClient;
    this.authorityClient = authorityClient;
  }

  @Override
  public SrsLoadResult loadMarcRecordsBlocking(List<String> uuids, AbstractExportStrategy.EntityType idType, String jobExecutionId, OkapiConnectionParams okapiConnectionParams) {
    Optional<JsonObject> optionalRecords;
    if (AbstractExportStrategy.EntityType.INSTANCE == idType ) {
      optionalRecords = getMarcRecordsForInstancesByIds(uuids, jobExecutionId, okapiConnectionParams);
    } else if (AbstractExportStrategy.EntityType.AUTHORITY == idType) {
      optionalRecords = getMarcRecordsForAuthoritiesByIds(uuids, jobExecutionId, okapiConnectionParams);
    } else {
      optionalRecords = srsClient.getRecordsByIdsFromLocalTenant(uuids, idType, jobExecutionId, okapiConnectionParams);
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

    var instances
      = inventoryClient.getByIds(uuids, jobExecutionId, okapiConnectionParams)
      .map(entries -> entries.getJsonArray(INSTANCES).stream()
        .filter(JsonObject.class::isInstance)
        .map(JsonObject.class::cast)
        .toList())
      .orElse(Collections.emptyList());

    var localInstanceLocalSrsUUIDs = instances.stream().filter(instance -> !CONSORTIUM_MARC_INSTANCE_SOURCE.equals(instance.getString("source"))).map(json -> json.getString(ID_FIELD)).toList();

    Optional<JsonObject> localSrsRecords = !localInstanceLocalSrsUUIDs.isEmpty()
      ? srsClient.getRecordsByIdsFromLocalTenant(localInstanceLocalSrsUUIDs, AbstractExportStrategy.EntityType.INSTANCE, jobExecutionId, okapiConnectionParams)
      : Optional.empty();

    var localInstanceCentralSrsUUIDs = instances.stream().filter(instance -> CONSORTIUM_MARC_INSTANCE_SOURCE.equals(instance.getString("source"))).map(json -> json.getString(ID_FIELD)).toList();

    var centralInstanceCentralSrsUUIDs = uuids.stream().filter(uuid -> !localInstanceCentralSrsUUIDs.contains(uuid) && !localInstanceLocalSrsUUIDs.contains(uuid)).toList();
    var centralSrsUUIDs =  union(localInstanceCentralSrsUUIDs, centralInstanceCentralSrsUUIDs);

    if (!centralSrsUUIDs.isEmpty()) {
      Optional<JsonObject> centralSrsRecords = srsClient.getRecordsByIdsFromCentralTenant(centralSrsUUIDs, AbstractExportStrategy.EntityType.INSTANCE, jobExecutionId, okapiConnectionParams);
      if (centralSrsRecords.isPresent()) {
        if (localSrsRecords.isEmpty()) {
          localSrsRecords = centralSrsRecords;
        } else {
          var centralTenantRecordsArray = centralSrsRecords.get().getJsonArray(SOURCE_RECORDS_FIELD);
          var records = localSrsRecords.get();
          records.getJsonArray(SOURCE_RECORDS_FIELD).addAll(centralTenantRecordsArray);
          var totalSize = records.getInteger(TOTAL_RECORDS_FIELD);
          records.put(TOTAL_RECORDS_FIELD, totalSize);
        }
      }
    }
    return localSrsRecords;
  }

  private Optional<JsonObject> getMarcRecordsForAuthoritiesByIds(List<String> uuids, String jobExecutionId, OkapiConnectionParams okapiConnectionParams) {

    var centralTenantUUIDs = authorityClient.getByIds(uuids, jobExecutionId, okapiConnectionParams, CONSORTIUM_MARC_INSTANCE_SOURCE)
      .map(entries -> entries.getJsonArray(AUTHORITIES).stream()
        .filter(JsonObject.class::isInstance)
        .map(JsonObject.class::cast)
        .map(json -> json.getString(ID_FIELD))
        .toList())
      .orElse(Collections.emptyList());

    Optional<JsonObject> localTenantRecords = Optional.empty();

    var localTenantUUIDs = uuids.stream()
      .filter(uuid -> !centralTenantUUIDs.contains(uuid))
      .toList();

    if (!localTenantUUIDs.isEmpty()) {
      localTenantRecords = srsClient.getRecordsByIdsFromLocalTenant(localTenantUUIDs, AbstractExportStrategy.EntityType.AUTHORITY, jobExecutionId, okapiConnectionParams);
    }

    if (!centralTenantUUIDs.isEmpty()) {
      Optional<JsonObject> centralTenantRecords = srsClient.getRecordsByIdsFromCentralTenant(centralTenantUUIDs, AbstractExportStrategy.EntityType.AUTHORITY, jobExecutionId, okapiConnectionParams);
      if (centralTenantRecords.isPresent()) {
        if (localTenantRecords.isEmpty()) {
          localTenantRecords = centralTenantRecords;
        } else {
          var centralTenantRecordsArray = centralTenantRecords.get().getJsonArray(SOURCE_RECORDS_FIELD);
          var records = localTenantRecords.get();
          records.getJsonArray(SOURCE_RECORDS_FIELD).addAll(centralTenantRecordsArray);
          var totalSize = records.getInteger(TOTAL_RECORDS_FIELD);
          records.put(TOTAL_RECORDS_FIELD, totalSize);
        }
      }
    }
    return localTenantRecords;
  }

  private void populateLoadResultFromInventory(Collection<String> entityIds, JsonObject entities, LoadResult loadResult) {
    List<JsonObject> inventoryRecords = new ArrayList<>();
    Set<String> entitiesIdentifiersSet = new HashSet<>(entityIds);
    String jsonArrayKey = loadResult.getEntityType().equals(AbstractExportStrategy.EntityType.INSTANCE) ? INSTANCES : HOLDINGS_RECORDS;
    for (String entityId : entityIds) {
      for (Object entity : entities.getJsonArray(jsonArrayKey)) {
        JsonObject e = (JsonObject) entity;
        if (e.getValue(ID_FIELD).equals(entityId)) {
          entitiesIdentifiersSet.remove(entityId);
          inventoryRecords.add(e);
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
    for (Object object : records) {
      JsonObject e = (JsonObject) object;
      marcRecords.add(e);
      JsonObject externalIdsHolder = e.getJsonObject("externalIdsHolder");
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
