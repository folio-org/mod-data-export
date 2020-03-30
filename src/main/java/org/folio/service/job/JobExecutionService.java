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
   *
   * @param query  query string to filter entities
   * @param offset starting index in a list of results
   * @param limit  maximum number of results to return
   * @return future with JobExecutionCollection
   */
  Future<JobExecutionCollection> get(String query, int offset, int limit, String tenantId);

  /**
   * Saves {@link JobExecution}
   *
   * @param jobExecution jobExecution to save
   * @param tenantId     tenant id
   * @return future with JobExecutionCollection
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

  Future<JobExecution> incrementCurrentProgress(String jobExecutionId, int delta, String tenantId);
}
