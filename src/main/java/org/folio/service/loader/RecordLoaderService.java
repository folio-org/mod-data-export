package org.folio.service.loader;

import io.vertx.core.json.JsonObject;

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
  MarcLoadResult loadSrsMarcRecords(List<String> instanceIds);

  /**
   * Retrieves Inventory instances
   *
   * @param instanceIds inventory instances identifiers
   * @return collection of json objects
   */
  List<JsonObject> loadInventoryInstances(List<String> instanceIds);
}
