package org.folio.service.loader;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.clients.InventoryClient;
import org.folio.clients.SourceRecordStorageClient;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

  private SourceRecordStorageClient srsClient;
  private InventoryClient inventoryClient;

  public RecordLoaderServiceImpl(@Autowired SourceRecordStorageClient srsClient, @Autowired InventoryClient inventoryClient) {
    this.srsClient = srsClient;
    this.inventoryClient = inventoryClient;
  }

  @Override
  public SrsLoadResult loadMarcRecordsBlocking(List<String> uuids, OkapiConnectionParams okapiConnectionParams, int partitionSize) {
    Optional<JsonObject> optionalRecords = srsClient.getRecordsByIds(uuids, okapiConnectionParams, partitionSize);
    SrsLoadResult srsLoadResult = new SrsLoadResult();
    if (optionalRecords.isPresent()) {
      populateLoadResultFromSRS(uuids, optionalRecords.get(), srsLoadResult);
    } else {
      srsLoadResult.setInstanceIdsWithoutSrs(uuids);
    }
    return srsLoadResult;
  }

  @Override
  public List<JsonObject> loadInventoryInstancesBlocking(Collection<String> instanceIds, OkapiConnectionParams params, int partitionSize) {
    Optional<JsonObject> optionalRecords = inventoryClient.getInstancesByIds(new ArrayList<>(instanceIds), params, partitionSize);
    return optionalRecords.map(this::populateLoadResultFromInventory).orElseGet(ArrayList::new);
  }

  private void populateLoadResultFromSRS(List<String> uuids, JsonObject underlyingRecords, SrsLoadResult loadResult) {
    JsonArray records = underlyingRecords.getJsonArray("records");
    List<String> marcRecords = new ArrayList<>();
    Set<String> singleInstanceIdentifiersSet = new HashSet<>(uuids);
    for (Object o : records) {
      JsonObject record = (JsonObject) o;
      marcRecords.add(getRecordContent(record));
      JsonObject externalIdsHolder = record.getJsonObject("externalIdsHolder");
      if (externalIdsHolder != null) {
        String instanceId = externalIdsHolder.getString("instanceId");
        singleInstanceIdentifiersSet.remove(instanceId);
      }
    }
    loadResult.setUnderlyingMarcRecords(marcRecords);
    loadResult.setInstanceIdsWithoutSrs(new ArrayList<>(singleInstanceIdentifiersSet));
  }

  private List<JsonObject> populateLoadResultFromInventory(JsonObject instancesJson) {
    List<JsonObject> instancesResult = new ArrayList<>();
    JsonArray instances = instancesJson.getJsonArray("instances");
    for (Object instance : instances) {
      instancesResult.add(JsonObject.mapFrom(instance));
    }
    return instancesResult;
  }

  private String getRecordContent(JsonObject record) {
    return record.getJsonObject("parsedRecord").getJsonObject("content").encode();
  }
}
