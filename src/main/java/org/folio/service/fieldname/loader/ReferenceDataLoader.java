package org.folio.service.fieldname.loader;

import io.vertx.core.json.JsonObject;
import org.folio.util.OkapiConnectionParams;

import java.util.Map;

/**
 * Record data loader. Loader is responsible to retrieve reference data from inventory.
 */
public interface ReferenceDataLoader {

  /**
   * Retrieves reference data from inventory
   *
   * @param okapiConnectionParams okapi headers and connection parameters
   * @return map with reference data
   */
  Map<String, JsonObject> load(OkapiConnectionParams okapiConnectionParams);

}
