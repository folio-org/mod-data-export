package org.folio.dao;

import io.vertx.core.Future;
import java.util.Optional;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobExecutionCollection;

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
   *
   * @param jobId
   * @param tenantId
   * @return
   */
  Future<Optional<JobExecution>> getById(String jobId, String tenantId);
}
