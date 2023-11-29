package org.folio.dao;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobExecutionCollection;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Data access object for {@link JobExecution}
 */
public interface JobExecutionDao {

  /**
   * Searches for {@link JobExecution} in the db
   *
   * @param query  query string to filter jobExecutions based on matching criteria in fields
   * @param offset starting index in a list of results
   * @param limit  maximum number of results to return
   * @return future with {@link JobExecutionCollection}
   */
  Future<JobExecutionCollection> get(String query, int offset, int limit, String tenantId);

  /**
   * Saves {@link JobExecution}
   *
   * @param jobExecution jobExecution to save
   * @param tenantId     tenant id
   * @return future with id of saved {@link JobExecution}
   */
  Future<JobExecution> save(JobExecution jobExecution, String tenantId);

  /**
   * Updates {@link JobExecution}
   *
   * @param jobExecution job to update
   * @param tenantId     tenant id
   * @return future
   */
  Future<JobExecution> update(JobExecution jobExecution, String tenantId);

  /**
   * Gets {@link JobExecution}
   *
   * @param jobExecutionId job id
   * @param tenantId       tenant id
   * @return future
   */
  Future<Optional<JobExecution>> getById(String jobExecutionId, String tenantId);

  /**
   * Deletes {@link JobExecution} from database
   *
   * @param id       id of {@link JobExecution} to delete
   * @param tenantId tenant id
   * @return future with true is succeeded
   */
  Future<Boolean> deleteById(String id, String tenantId);

  /**
   * Fetch list of {@link JobExecution} from database
   *
   * @param lastUpdateDate last updated date {@link Date}
   * @param tenantId       tenant id
   * @return future with list of expire {@link JobExecution}
   */
  Future<List<JobExecution>> getExpiredEntries(Date lastUpdateDate, String tenantId);

  /**
   * Fetch list of failed {@link JobExecution} without completed date from database
   *
   * @param tenantId       tenant id
   * @return future with list of expire {@link JobExecution}
   */
  Future<List<JobExecution>> getFailedEntriesWithoutCompletedDate(String tenantId);

}
