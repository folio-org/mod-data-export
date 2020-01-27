package org.folio.service.loader;

import io.vertx.core.Future;

import java.util.Collections;
import java.util.List;

/**
 * Record loader service. Service is responsible to retrieve various FOLIO records: SRS records, Inventory records.
 */
public interface RecordLoaderService {

  /**
   * Returns a collection of records by given Instance ids
   * @param ids   collection of Inventory ids
   * @return      collection of records
   */
  default Future<List<String>> loadRecordsByInstanceIds(List<String> ids) {
    // Retrieves collection of underlying SRS records as a source of truth
    // Retrieves collection of Inventory records that do not have underlying SRS records
    return Future.succeededFuture(Collections.emptyList());
  }
}
