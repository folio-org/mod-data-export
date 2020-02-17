package org.folio.service.loader;

import io.vertx.core.json.JsonObject;
import org.folio.util.OkapiConnectionParams;

import java.util.List;

/**
 * Record loader service. Service is responsible to retrieve various FOLIO records: SRS records, Inventory records.
 */
public interface RecordLoaderService {

  /**
   * Retrieves collection of underlying SRS records
   * @return      collection of records
   */
   MarcLoadResult loadSrsMarcRecords(List<String> instanceIds);
  /**
   * Retrieves collection of Inventory records
   */
  List<JsonObject> loadInventoryInstances(List<String> instanceIds);
}
