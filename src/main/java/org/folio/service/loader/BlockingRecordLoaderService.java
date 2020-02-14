package org.folio.service.loader;

import java.util.ArrayList;
import java.util.Collection;
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
    SrsLoadResult srsLoadResult = new SrsLoadResult();
    Collection<String> marcRecords = new ArrayList<>();
    Collection<String> notFoundIds = new ArrayList<>();
    uuids.forEach(uuid -> {
      Optional<JsonObject> optionalRecord = client.getById(uuid);
      if (optionalRecord.isPresent()) {
        marcRecords.add(getRecordContent(optionalRecord.get()));
      } else {
        notFoundIds.add(uuid);
      }
    });
    srsLoadResult.setUnderlyingMarcRecords(marcRecords);
    srsLoadResult.setSingleInstanceIdentifiers(notFoundIds);
    return srsLoadResult;
  }

  private String getRecordContent(JsonObject record) {
    return record.getJsonObject("rawRecord").getString("content");
  }
}
