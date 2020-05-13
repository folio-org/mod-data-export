package org.folio.dao;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.MappingProfileCollection;

import java.util.Optional;

public interface MappingProfileDao {

  /**
   * Returns {@link MappingProfileCollection} by the input query in the DB
   *
   * @param query  query string to filter entities
   * @param offset starting index in a list of results
   * @param limit  maximum number of results to return
   * @return future with {@link MappingProfileCollection}
   */
  Future<MappingProfileCollection> get(String query, int offset, int limit, String tenantId);

  /**
   * Saves {@link MappingProfile} to database
   *
   * @param mappingProfile {@link MappingProfile} to save
   * @param tenantId       tenant id
   * @return future with saved {@link MappingProfile}
   */
  Future<MappingProfile> save(MappingProfile mappingProfile, String tenantId);

  /**
   * Updates {@link MappingProfile}
   *
   * @param mappingProfile {@link MappingProfile} to update
   * @param tenantId       tenant id
   * @return future with {@link MappingProfile}
   */
  Future<MappingProfile> update(MappingProfile mappingProfile, String tenantId);

  /**
   * Delete {@link MappingProfile} by id
   *
   * @param mappingProfileId job id
   * @param tenantId         tenant id
   * @return future with {@link MappingProfile}
   */
  Future<Boolean> delete(String mappingProfileId, String tenantId);

  /**
   * Gets {@link MappingProfile}
   *
   * @param mappingProfileId mappingProfile id
   * @param tenantId         tenant id
   * @return future with {@link MappingProfile}
   */
  Future<Optional<MappingProfile>> getById(String mappingProfileId, String tenantId);
}
