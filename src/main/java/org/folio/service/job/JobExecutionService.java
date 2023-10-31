package org.folio.service.job;


import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobExecutionCollection;
import org.folio.rest.jaxrs.model.Progress;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

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
  void updateJobStatusById(String id, JobExecution jobExecution, JobExecution.Status status, String tenantId);

  /**
   * Updates {@link JobExecution} status with IN-PROGRESS && updates exported files && updates started date
   *
   * @param id                   job execution id
   * @param fileExportDefinition definition of the file to export
   * @param user                 user represented in json object
   * @param withProgress         condition to add progress
   * @param tenantId             tenant id
   */
  Future<JobExecution> prepareJobForExport(JobExecution jobExecution, FileDefinition fileExportDefinition, JsonObject user, int totalCount, boolean withProgress, String tenantId);

  /**
   * Updates {@link JobExecution} status with specified status && updates exported files && updates started and completed dates
   *
   * @param jobExecution         job execution
   * @param fileExportDefinition definition of the file to export
   * @param user                 user represented in json object
   * @param withProgress         condition to add progress
   * @param tenantId             tenant id
   */
  void prepareAndSaveJobForFailedExport(JobExecution jobExecution, FileDefinition fileExportDefinition, JsonObject user, int totalCount, boolean withProgress, String tenantId);

  /**
   * Increment current value in {@link Progress} of {@link JobExecution}
   *
   * @param jobExecutionId id of a job
   * @param exported       exported records number
   * @param failed         number of records failed on export
   * @param duplicatedSrs  number of records with duplicated SRS
   * @param tenantId       tenant id
   * @return future
   */
  Future<JobExecution> incrementCurrentProgress(String jobExecutionId, final int exported, final int failed, final int duplicatedSrs, final int invalidUUIDs, String tenantId);

  /**
   * Update status of expired job executions to fail
   *
   * @param tenantId tenant id
   * @return void future
   */
  Future<Void> expireJobExecutions(String tenantId);

  /**
   * Deletes a {@link JobExecution} by id
   *
   * @param id       JobExecution id
   * @param tenantId tenant id
   * @return future with Boolean
   */
  Future<Boolean> deleteById(String id, String tenantId);

}
