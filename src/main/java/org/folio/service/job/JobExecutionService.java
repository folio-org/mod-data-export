package org.folio.service.job;


import io.vertx.core.Future;

import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobExecutionCollection;
import org.folio.rest.jaxrs.model.Progress;

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
   * @return future with JobExecution
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
  Future<JobExecution> getById(String jobExecutionId, String tenantId);

  /**
   * Updates status of {@link JobExecution} with id
   *
   * @param id       job execution id
   * @param status   status to update
   * @param tenantId tenant id
   */
  void updateJobStatusById(String id, JobExecution.Status status, String tenantId);

  /**
   * Updates {@link JobExecution} status with IN-PROGRESS && updates exported files && updates started date
   *
   * @param id                   job execution id
   * @param fileExportDefinition definition of the file to export
   * @param user                 user represented in json object
   * @param tenantId             tenant id
   */
  void prepareJobForExport(String id, FileDefinition fileExportDefinition, JsonObject user, String tenantId);

  /**
   * Increment current value in {@link Progress} of {@link JobExecution}
   *
   * @param jobExecutionId id of a job
   * @param exported exported records number
   * @param failed   number of records failed on export
   * @param tenantId       tenant id
   * @return future
   */
  Future<JobExecution> incrementCurrentProgress(String jobExecutionId, final int exported, final int failed, String tenantId);
}
