package org.folio.service.profiles.jobprofile;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.jaxrs.model.JobProfileCollection;
import org.folio.util.OkapiConnectionParams;

public interface JobProfileService {


  /**
   * Searches for {@link JobProfile} by query
   *
   * @param query    query for search
   * @param tenantId tenant id
   * @return future with optional {@link JobProfile}
   */
  Future<JobProfileCollection> get(String query, int offset, int limit, String tenantId);

  /**
   * Searches for {@link JobProfile} by id
   *
   * @param id       jobProfile id
   * @param tenantId tenant id
   * @return future with optional {@link JobProfile}
   */
  Future<JobProfile> getById(String id, String tenantId);

  /**
   * Gets default {@link JobProfile}
   *
   * @param tenantId tenant id
   * @return future with default {@link JobProfile}
   */
  Future<JobProfile> getDefault(String tenantId);

  /**
   * Saves {@link JobProfile} to database
   *
   * @param jobProfile {@link JobProfile} to save
   * @param params     okapi headers and connection parameters
   * @return future with saved {@link JobProfile}
   */
  Future<JobProfile> save(JobProfile jobProfile, OkapiConnectionParams params);

  /**
   * Updates {@link JobProfile}
   *
   * @param jobProfile {@link JobProfile} to update
   * @param params     okapi headers and connection parameters
   * @return future with {@link JobProfile}
   */
  Future<JobProfile> update(JobProfile jobProfile, OkapiConnectionParams params);

  /**
   * Deletes a {@link JobProfile} by id
   *
   * @param id       jobProfile id
   * @param tenantId tenant id
   * @return future with Boolean
   */
  Future<Boolean> deleteById(String id, String tenantId);

  /**
   * Searches for only used {@link JobProfile}
   *
   * @param offset starting index in a list of records
   * @param limit maximum number of records to return
   * @param tenantId tenant id
   * @return future with {@link JobProfileCollection}
   */
  Future<JobProfileCollection> getUsed(int offset, int limit, String tenantId);
}
