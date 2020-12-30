package org.folio.service.loader;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.folio.clients.InventoryClient;
import org.folio.clients.SourceRecordStorageClient;
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
  private SourceRecordStorageClient srsClient;
  private InventoryClient inventoryClient;

  public RecordLoaderServiceImpl(@Autowired SourceRecordStorageClient srsClient, @Autowired InventoryClient inventoryClient) {
    this.srsClient = srsClient;
    this.inventoryClient = inventoryClient;
  }

  @Override
  public SrsLoadResult loadMarcRecordsBlocking(List<String> uuids, String jobExecutionId, OkapiConnectionParams okapiConnectionParams) {
    Optional<JsonObject> optionalRecords = srsClient.getRecordsByInstanceIds(uuids, jobExecutionId, okapiConnectionParams);
    SrsLoadResult srsLoadResult = new SrsLoadResult();
    if (optionalRecords.isPresent()) {
      populateLoadResultFromSRS(uuids, optionalRecords.get(), srsLoadResult);
    } else {
      srsLoadResult.setInstanceIdsWithoutSrs(uuids);
    }
    return srsLoadResult;
  }

  @Override
  public InventoryLoadResult loadInventoryInstancesBlocking(Collection<String> instanceIds, String jobExecutionId, OkapiConnectionParams params, int partitionSize) {
    Optional<JsonObject> optionalRecords = inventoryClient.getInstancesByIds(new ArrayList<>(instanceIds), jobExecutionId, params, partitionSize);
    InventoryLoadResult inventoryLoadResult = new InventoryLoadResult();
    if (optionalRecords.isPresent()) {
      populateLoadResultFromInventory(instanceIds, optionalRecords.get(), inventoryLoadResult);
    } else {
      inventoryLoadResult.setNotFoundInstancesUUIDs(instanceIds);
    }
    return inventoryLoadResult;
  }

  private void populateLoadResultFromInventory(Collection<String> instanceIds, JsonObject instanceRecord, InventoryLoadResult inventoryLoadResult) {
    List<JsonObject> inventoryRecords = new ArrayList<>();
    Set<String> singleInstanceIdentifiersSet = new HashSet<>(instanceIds);
    for (String instanceId : instanceIds) {
      for (Object instance : instanceRecord.getJsonArray("instances")) {
        JsonObject record = (JsonObject) instance;
        if (record.getValue("id").equals(instanceId)) {
          singleInstanceIdentifiersSet.remove(instanceId);
          inventoryRecords.add(record);
        }
      }
    }
    inventoryLoadResult.setInstances(inventoryRecords);
    inventoryLoadResult.setNotFoundInstancesUUIDs(singleInstanceIdentifiersSet);
  }

  private void populateLoadResultFromSRS(List<String> uuids, JsonObject underlyingRecords, SrsLoadResult loadResult) {
    JsonArray records = underlyingRecords.getJsonArray("sourceRecords");
    List<JsonObject> marcRecords = new ArrayList<>();
    Set<String> singleInstanceIdentifiersSet = new HashSet<>(uuids);
    for (Object o : records) {
      JsonObject record = (JsonObject) o;
      marcRecords.add(record);
      JsonObject externalIdsHolder = record.getJsonObject("externalIdsHolder");
      if (externalIdsHolder != null) {
        String instanceId = externalIdsHolder.getString("instanceId");
        singleInstanceIdentifiersSet.remove(instanceId);
      }
    }
    loadResult.setUnderlyingMarcRecords(marcRecords);
    loadResult.setInstanceIdsWithoutSrs(new ArrayList<>(singleInstanceIdentifiersSet));
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
    return optionalRecords.map(holdings -> populateLoadResultFromResponse("holdingsRecords", holdings)).orElseGet(ArrayList::new);
  }

  @Override
  public List<JsonObject> getAllItemsForHolding(List<String> holdingIds, String jobExecutionId, OkapiConnectionParams params) {
    Optional<JsonObject> optionalRecords = inventoryClient.getItemsByHoldingIds(holdingIds, jobExecutionId, params);
    return optionalRecords.map(items -> populateLoadResultFromResponse("items", items)).orElseGet(ArrayList::new);
  }
}
