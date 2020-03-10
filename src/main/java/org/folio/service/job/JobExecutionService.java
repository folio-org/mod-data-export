package org.folio.service.job;


import io.vertx.core.Future;
import java.util.Optional;
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
  Future<JobExecutionCollection> get(String query, int offset, int limit, String tenantId);

  /**
   * Saves {@link JobExecution}
   * @param jobExecution  jobExecution to save
   * @param tenantId tenant id
   * @return future with JobExecutionCollection
   */
  Future<JobExecution> save(JobExecution jobExecution, String tenantId);

  /**
   *
   * @param jobExecution
   * @param tenantId
   * @return
   */
  Future<Optional<JobExecution>> getById(String jobId, String tenantId);
}
