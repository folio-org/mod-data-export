package org.folio.service.job;


import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.folio.dao.JobExecutionDao;
import org.folio.rest.jaxrs.model.ExportedFile;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobExecutionCollection;
import org.folio.rest.jaxrs.model.Progress;
import org.folio.rest.jaxrs.model.RunBy;
import org.folio.service.profiles.jobprofile.JobProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

/**
 * Implementation of the JobExecutionService, calls JobExecutionDao to access JobExecution metadata.
 */
@Service
public class JobExecutionServiceImpl implements JobExecutionService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String DEFAULT = "default";

  @Autowired
  private JobExecutionDao jobExecutionDao;
  @Autowired
  private JobProfileService jobProfileService;

  @Override
  public Future<JobExecutionCollection> get(String query, int offset, int limit, String tenantId) {
    Promise<JobExecutionCollection> jobExecutionPromise = Promise.promise();
    jobExecutionDao.get(query, offset, limit, tenantId)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          JobExecutionCollection jobExecutionCollection = ar.result();
          jobExecutionCollection.getJobExecutions().forEach(jobExecution -> {
            if (StringUtils.isNotEmpty(jobExecution.getJobProfileId())) {
              jobProfileService.getById(jobExecution.getJobProfileId(), tenantId)
                .onSuccess(jobProfile -> jobExecution.setJobProfileName(jobProfile.getName()))
                .onFailure(async -> {
                if (StringUtils.isEmpty(jobExecution.getJobProfileName())) {
                  LOGGER.error("Failed to get Job Profile with id {} while get Job Executions by query with id {}, the default name will be used", jobExecution.getJobProfileId(), jobExecution.getId());
                  jobExecution.setJobProfileName(DEFAULT);
                } else {
                  LOGGER.error("Failed to get Job Profile with id {} while get Job Executions by query with id {}, the existing name will be used", jobExecution.getJobProfileId(), jobExecution.getId());
                }
              });
            } else {
              LOGGER.error("JobProfileId is not present in jobExecution with id {} while get JobExecution by query, the default name will be used", jobExecution.getId());
              jobExecution.setJobProfileName(DEFAULT);
            }
          });
          jobExecutionPromise.complete(jobExecutionCollection);
        } else {
          jobExecutionPromise.fail(ar.cause());
        }
      });
    return jobExecutionPromise.future();
  }

  @Override
  public Future<JobExecution> save(JobExecution jobExecution, String tenantId) {
    return jobExecutionDao.save(jobExecution, tenantId);
  }

  @Override
  public Future<JobExecution> update(final JobExecution jobExecution, final String tenantId) {
    return jobExecutionDao.update(jobExecution, tenantId);
  }

  @Override
  public Future<JobExecution> getById(final String jobExecutionId, final String tenantId) {
    Promise<JobExecution> jobExecutionPromise = Promise.promise();
    jobExecutionDao.getById(jobExecutionId, tenantId)
      .onComplete(optionalJobExecution -> {
        if (optionalJobExecution.succeeded() && optionalJobExecution.result().isPresent()) {
          JobExecution jobExecution = optionalJobExecution.result().get();
          if (StringUtils.isNotEmpty(jobExecution.getJobProfileId())) {
            jobProfileService.getById(jobExecution.getJobProfileId(), tenantId)
              .onSuccess(jobProfile -> jobExecution.setJobProfileName(jobProfile.getName())).onFailure(ar -> {
              if (StringUtils.isEmpty(jobExecution.getJobProfileName())) {
                LOGGER.error("Failed to get Job Profile with id {} while querying Job Execution with id {}, the default name will be used", jobExecution.getJobProfileId(), jobExecution.getId());
                jobExecution.setJobProfileName(DEFAULT);
              } else {
                LOGGER.error("Failed to get Job Profile with id {} while get Job Execution with id {}, the existing name will be used", jobExecution.getJobProfileId(), jobExecution.getId());
              }
            });
          } else {
            LOGGER.error("JobProfileId is not present in jobExecution with id {} while get JobExecution by id, the default name will be used", jobExecution.getId());
            jobExecution.setJobProfileName(DEFAULT);
          }
          jobExecutionPromise.complete(jobExecution);
        } else {
          String errorMessage = String.format("Job execution not found with id %s", jobExecutionId);
          LOGGER.error(errorMessage);
          jobExecutionPromise.fail(new NotFoundException(errorMessage));
        }
      });
    return jobExecutionPromise.future();
  }

  @Override
  public void updateJobStatusById(String id, JobExecution.Status status, String tenantId) {
    getById(id, tenantId).onSuccess(jobExecution -> {
      jobExecution.setStatus(status);
      jobExecution.setCompletedDate(new Date());
      update(jobExecution, tenantId);
    });
  }

  @Override
  public Future<JobExecution> prepareJobForExport(String id, FileDefinition fileExportDefinition, JsonObject user, long totalCount, String tenantId) {
    return getById(id, tenantId).compose(jobExecution -> {
      ExportedFile exportedFile = new ExportedFile()
        .withFileId(UUID.randomUUID().toString())
        .withFileName(fileExportDefinition.getFileName());
      Set<ExportedFile> exportedFiles = jobExecution.getExportedFiles();
      exportedFiles.add(exportedFile);
      jobExecution.setExportedFiles(exportedFiles);
      jobExecution.setStatus(JobExecution.Status.IN_PROGRESS);
      if (Objects.isNull(jobExecution.getStartedDate())) {
        jobExecution.setStartedDate(new Date());
      }
      JsonObject personal = user.getJsonObject("personal");
      jobExecution.setRunBy(new RunBy()
        .withFirstName(personal.getString("firstName"))
        .withLastName(personal.getString("lastName")));
      jobExecution.setProgress(new Progress().withTotal(String.valueOf(totalCount)));
      return update(jobExecution, tenantId);
    });
  }


  @Override
  public Future<JobExecution> incrementCurrentProgress(String jobExecutionId, int exported, int failed, String tenantId) {
    return jobExecutionDao.getById(jobExecutionId, tenantId)
      .compose(jobExecutionOptional -> {
        if (jobExecutionOptional.isPresent()) {
          JobExecution jobExecution = jobExecutionOptional.get();
          Progress progress = jobExecution.getProgress();
          if (nonNull(progress)) {
            progress.setExported(progress.getExported() + exported);
            progress.setFailed(progress.getFailed() + failed);
            return jobExecutionDao.update(jobExecution, tenantId);
          }
          return Future.failedFuture(format("Unable to update progress of job execution with id %s", jobExecutionId));
        }
        return Future.failedFuture(format("Job execution with id %s doesn't exist", jobExecutionId));
      });
  }
}
