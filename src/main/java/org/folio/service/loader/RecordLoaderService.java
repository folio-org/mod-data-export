package org.folio.service.loader;

import io.vertx.core.json.JsonObject;
import org.folio.util.OkapiConnectionParams;

import java.util.List;

/**
 * Record loader service. Service is responsible to retrieve various FOLIO records: SRS records, Inventory records.
 */
public interface RecordLoaderService {

  /**
   * Retrieves collection of underlying SRS records as a source of truth
   * Returns collection of records by given Instance ids
   * @param uuids   collection of Inventory ids
   * @param params
   * @return      collection of records
   */
   MarcLoadResult loadMarcByInstanceIds(List<String> uuids, OkapiConnectionParams params);
  /**
   * Retrieves collection of Inventory records that do not have underlying SRS records
   */
  List<JsonObject> loadInstancesByIds(List<String> instanceIds);
}
