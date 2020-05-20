package org.folio.service.profiles.jobprofile;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.jaxrs.model.JobProfileCollection;

public interface JobProfileService {


  /**
   * Searches for {@link JobProfile} by id
   *
   * @param id       jobProfile id
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
   * Saves {@link JobProfile} to database
   *
   * @param jobProfile {@link JobProfile} to save
   * @param tenantId       tenant id
   * @return future with saved {@link JobProfile}
   */
  Future<JobProfile> save(JobProfile jobProfile, String tenantId);

  /**
   * Updates {@link JobProfile}
   *
   * @param jobProfile {@link JobProfile} to update
   * @param tenantId       tenant id
   * @return future with {@link JobProfile}
   */
  Future<JobProfile> update(JobProfile jobProfile, String tenantId);

  /**
   * Deletes a {@link JobProfile} by id
   *
   * @param id       jobProfile id
   * @param tenantId tenant id
   * @return future with Boolean
   */
  Future<Boolean> deleteById(String id, String tenantId);

}
