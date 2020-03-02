package org.folio.service.job;


import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobExecutionCollection;

/**
 * JobExecution Service interface, contains logic for accessing jobs.
 */
public interface JobExecutionService {

  /**
   * Returns JobExecutionCollection by the input query
   * @param query  query string to filter entities
   * @param offset starting index in a list of results
   * @param limit  maximum number of results to return
   * @return future with JobExecutionCollection
   */
  Future<JobExecutionCollection> getJobExecutions(String query, int offset, int limit, String tenantId);

  /**
   * Saves {@link JobExecution}
   * @param jobExecution  jobExecution to save
   * @param tenantId tenant id
   * @return future with JobExecutionCollection
   */
  Future<JobExecution> saveJobExecution(JobExecution jobExecution, String tenantId);
}
