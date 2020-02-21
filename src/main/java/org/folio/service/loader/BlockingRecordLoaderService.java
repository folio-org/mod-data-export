package org.folio.service.loader;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.clients.impl.SourceRecordStorageClient;
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
 * Synchronous implementation of #RecordLoaderService that uses blocking http client.
 */
@Service
public class BlockingRecordLoaderService implements RecordLoaderService {

  @Autowired
  private SourceRecordStorageClient client;

  @Override
  public SrsLoadResult loadMarcRecords(List<String> uuids, OkapiConnectionParams okapiConnectionParams) {
    Optional<JsonObject> optionalRecords = client.getByIds(uuids, okapiConnectionParams);
    SrsLoadResult srsLoadResult = new SrsLoadResult();
    optionalRecords.ifPresent(records -> populateLoadResult(uuids, records, srsLoadResult));
    return srsLoadResult;
  }

  @Override
  public List<JsonObject> loadInventoryInstances(Collection<String> instanceIds, OkapiConnectionParams params) {
    return new ArrayList<>();
  }

  private void populateLoadResult(List<String> uuids, JsonObject underlyingRecords, SrsLoadResult loadResult) {
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

  private String getRecordContent(JsonObject record) {
    return record.getJsonObject("rawRecord").getString("content");
  }
}
