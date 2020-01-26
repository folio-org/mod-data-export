package org.folio.service.loader;

import io.vertx.core.Future;

import java.util.Collections;
import java.util.List;

public interface RecordLoaderService {

  default Future<List<String>> loadRecordsByInstanceIds(List<String> uuids) {
    // Retrieves collection of underlying SRS records as a source of truth
    // Retrieves collection of Inventory records for those UUIDs that do not have underlying SRS
    return Future.succeededFuture(Collections.emptyList());
  }
}
