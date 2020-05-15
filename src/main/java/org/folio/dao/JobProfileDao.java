package org.folio.dao;

import io.vertx.core.Future;

import java.util.Optional;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.jaxrs.model.JobProfileCollection;

/**
 * Data access object for {@link JobProfile}
 */
public interface JobProfileDao {

  /**
   * Searches for {@link JobProfile} in the db
   *
   * @param query  query string to filter JobProfiles based on matching criteria in fields
   * @param offset starting index in a list of results
   * @param limit  maximum number of results to return
   * @return future with {@link JobProfileCollection}
   */
  Future<JobProfileCollection> get(String query, int offset, int limit, String tenantId);

  /**
   * Saves {@link JobExecution}
   *
   * @param jobProfile jobExecution to save
   * @param tenantId     tenant id
   * @return future with id of saved {@link JobProfile}
   */
  Future<JobProfile> save(JobProfile jobProfile, String tenantId);

  /**
   * Updates {@link JobProfile}
   *
   * @param jobProfile job to update
   * @param tenantId     tenant id
   * @return future
   */
  Future<JobProfile> update(JobProfile jobProfile, String tenantId);

  /**
   * Gets {@link JobProfile}
   *
   * @param jobProfileId job id
   * @param tenantId       tenant id
   * @return future
   */
  Future<Optional<JobProfile>> getById(String jobProfileId, String tenantId);

  /**
   * Deletes {@link JobProfile} from database
   *
   * @param id       id of {@link JobProfile} to delete
   * @param tenantId tenant id
   * @return future with true is succeeded
   */
  Future<Boolean> deleteById(String id, String tenantId);
}
