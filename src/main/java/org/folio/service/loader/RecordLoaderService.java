package org.folio.service.loader;

import io.vertx.core.json.JsonObject;
import org.folio.util.OkapiConnectionParams;

import java.util.Collection;
import java.util.List;

/**
 * Record loader service. Service is responsible to retrieve various FOLIO records: SRS records, Inventory records.
 */
public interface RecordLoaderService {

  /**
   * Retrieves SRS records using blocking http client
   *
   * @param instanceIds inventory instances identifiers
   * @return @see MarcLoadResult
   */
  SrsLoadResult loadMarcRecordsBlocking(List<String> instanceIds, OkapiConnectionParams okapiConnectionParams, int partitionSize);

  /**
   * Retrieves Inventory instances using blocking http client
   *
   * @param instanceIds inventory instances identifiers
   * @return collection of json objects
   */
  List<JsonObject> loadInventoryInstancesBlocking(Collection<String> instanceIds, OkapiConnectionParams okapiConnectionParams, int partitionSize);

  /**
   * Retrieve all the holdings for a given instance UUID
   *
   * @param instanceId
   * @param params
   */
  List<JsonObject>  getHoldingsForInstance(String instanceId, OkapiConnectionParams params);

  /**
   * Retrieve all Items for the list of holding UUIDs
   * @param holdingIds
   * @param params
   * @return
   */
  List<JsonObject> getAllItemsForHolding(List<String> holdingIds, OkapiConnectionParams params);
}
