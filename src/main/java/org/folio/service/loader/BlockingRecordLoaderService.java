package org.folio.service.loader;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.clients.SynchronousOkapiClient;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Synchronous implementation of #RecordLoaderService that uses blocking http client.
 */
@Service
public class BlockingRecordLoaderService implements RecordLoaderService {

  private final SynchronousOkapiClient client;

  public BlockingRecordLoaderService(SynchronousOkapiClient client) {
    this.client = client;
  }

  @Override
  public SrsLoadResult loadMarcRecords(List<String> uuids) {
    Optional<JsonObject> optionalRecords = client.getByIds(uuids);
    SrsLoadResult srsLoadResult = new SrsLoadResult();
    optionalRecords.ifPresent(records -> populateLoadResult(uuids, records, srsLoadResult));
    return srsLoadResult;
  }

  private void populateLoadResult(List<String> queriedUuids, JsonObject foundRecords, SrsLoadResult loadResult) {
    JsonArray records = foundRecords.getJsonArray("records");
    Set<String> marcRecords = new HashSet<>();
    Set<String> setSingleInstanceIdentifiers = new HashSet<>(queriedUuids);
    for (Object o : records) {
      if (o instanceof JsonObject) {
        JsonObject record = (JsonObject) o;
        marcRecords.add(getRecordContent(record));
        JsonObject externalIdsHolder = record.getJsonObject("externalIdsHolder");
        if (externalIdsHolder != null) {
          String instanceId = externalIdsHolder.getString("instanceId");
          setSingleInstanceIdentifiers.remove(instanceId);
        }
      }
    }
    loadResult.setUnderlyingMarcRecords(marcRecords);
    loadResult.setSingleInstanceIdentifiers(setSingleInstanceIdentifiers);
  }

  private String getRecordContent(JsonObject record) {
    return record.getJsonObject("rawRecord").getString("content");
  }
}
