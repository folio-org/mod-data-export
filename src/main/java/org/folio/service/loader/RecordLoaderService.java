package org.folio.service.loader;

import java.util.List;

/**
 * Record loader service. Service is responsible to retrieve various FOLIO records: SRS records, Inventory records.
 */
public interface RecordLoaderService {

  /**
   * Retrieves marc records from SRS
   *
   * @param instanceIds inventory instances identifiers
   * @return @see MarcLoadResult
   */
  SrsLoadResult loadMarcRecords(List<String> uuids);


  /**
   * Retrieves Inventory instances
   *
   * @param instanceIds inventory instances identifiers
   * @return collection of json objects
   */
  List<JsonObject> loadInventoryInstances(List<String> instanceIds);
}
