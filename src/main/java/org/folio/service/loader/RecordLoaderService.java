package org.folio.service.loader;

import java.util.List;

/**
 * Record loader service. Service is responsible to retrieve various FOLIO records: SRS records, Inventory records.
 */
public interface RecordLoaderService {

  /**
   * Retrieves collection of underlying SRS records as a source of truth
   * Retrieves collection of Inventory records that do not have underlying SRS records
   * Returns collection of records by given Instance ids
   * @param uuids   collection of Inventory ids
   * @return      collection of records
   */
   SrsLoadResult loadMarcRecords(List<String> uuids);
}
