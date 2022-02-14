package org.folio.service.loader;

import io.vertx.core.json.JsonObject;

import org.folio.service.manager.export.strategy.AbstractExportStrategy;
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
   * @param instanceIds           inventory instances identifiers
   * @param idType                type of uuids
   * @param jobExecutionId        job execution id
   * @param okapiConnectionParams okapi headers and connection parameters
   * @return @see MarcLoadResult
   */
  SrsLoadResult loadMarcRecordsBlocking(List<String> instanceIds, AbstractExportStrategy.EntityType idType, String jobExecutionId, OkapiConnectionParams okapiConnectionParams);

  /**
   * Retrieves Inventory instances using blocking http client
   *
   * @param instanceIds           inventory instances identifiers
   * @param jobExecutionId        job execution id
   * @param okapiConnectionParams okapi headers and connection parameters
   * @param partitionSize         inventory query limit
   * @return {@link LoadResult}
   */
  LoadResult loadInventoryInstancesBlocking(Collection<String> instanceIds, String jobExecutionId, OkapiConnectionParams okapiConnectionParams, int partitionSize);

  /**
   * Retrieve all the holdings for a given instance UUID
   *
   * @param instanceId     instance id
   * @param jobExecutionId job execution id
   * @param params         okapi headers and connection parameters
   */
  List<JsonObject>  getHoldingsForInstance(String instanceId, String jobExecutionId, OkapiConnectionParams params);

  /**
   * Retrieve all holdings by the given list of UUIDs
   *
   * @param holdingIds     holding ids
   * @param jobExecutionId job execution id
   * @param params         okapi headers and connection parameters
   * @param partitionSize  partition size
   */
  LoadResult getHoldingsById(List<String> holdingIds, String jobExecutionId, OkapiConnectionParams params, int partitionSize);

  /**
   * Retrieve all Items for the list of holding UUIDs
   *
   * @param holdingIds     holding id`s
   * @param jobExecutionId job execution id
   * @param params         okapi headers and connection parameters
   * @return collection of json objects
   */
  List<JsonObject> getAllItemsForHolding(List<String> holdingIds, String jobExecutionId, OkapiConnectionParams params);
}
