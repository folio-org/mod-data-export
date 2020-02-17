package org.folio.service.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.folio.clients.SynchronousOkapiClient;
import org.springframework.stereotype.Service;

import io.vertx.core.json.JsonObject;

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
    return populateLoadResult(uuids, optionalRecords);
  }

  private SrsLoadResult populateLoadResult(List<String> uuids, Optional<JsonObject> optionalRecords) {
    SrsLoadResult srsLoadResult = new SrsLoadResult();
    optionalRecords.ifPresent(r -> {
      List list = r.getJsonArray("records").getList();
      List<String> marcRecords = new ArrayList<>();
      final List<String> setSingleInstanceIdentifiers = new ArrayList<>();
      for (Object o : list) {
        if (o instanceof JsonObject) {
          JsonObject record = (JsonObject) o;
          marcRecords.add(record.getString("content"));
          JsonObject externalIdsHolder = record.getJsonObject("externalIdsHolder");
          if (externalIdsHolder != null) {
            String instanceId = externalIdsHolder.getString("instanceId");
            if (uuids.contains(instanceId)) {
              setSingleInstanceIdentifiers.add(instanceId);
            }
          }
        }
      }
      srsLoadResult.setUnderlyingMarcRecords(marcRecords);
      srsLoadResult.setUnderlyingMarcRecords(setSingleInstanceIdentifiers);
    });
    return srsLoadResult;
  }


}
