package org.folio.dao;

import java.util.Optional;

import org.folio.rest.jaxrs.model.Bucket;

import io.vertx.core.Future;

/**
 * Data access object for {@link Bucket}
 */
public interface BucketDao {

  /**
   * Searches for {@link Bucket} in the db
   *
   * @return future with {@link Bucket}
   */
  Future<Optional<Bucket>> getByTenantId(String tenantId);

  /**
   * Saves {@link Bucket}
   *
   * @param tenantBucket
   * @return future with id of saved {@link Bucket}
   */
  Future<String> save(String tenantId, Bucket tenantBucket);
}
