package org.folio.dao;

import io.vertx.core.Future;
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
  Future<JobExecutionCollection> getJobExecutions(String query, int offset, int limit, String tenantId);

  /**
   * Saves {@link JobExecution}
   *
   * @param jobExecution jobExecution to save
   * @param tenantId     tenant id
   * @return future with id of saved {@link JobExecution}
   */
  Future<JobExecution> saveJobExecution(JobExecution jobExecution, String tenantId);
}
