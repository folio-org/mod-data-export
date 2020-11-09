package org.folio.service.job;


import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.rest.jaxrs.model.JobExecution.Status.FAIL;
import static org.folio.rest.jaxrs.model.JobExecution.Status.IN_PROGRESS;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;

import org.apache.commons.collections4.CollectionUtils;
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

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Implementation of the JobExecutionService, calls JobExecutionDao to access JobExecution metadata.
 */
@Service
public class JobExecutionServiceImpl implements JobExecutionService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
          if (CollectionUtils.isEmpty(jobExecutionCollection.getJobExecutions())) {
            jobExecutionPromise.complete(jobExecutionCollection);
          } else {
            jobProfileService.get(getAssociatedJobProfileIdsQuery(jobExecutionCollection), 0, limit, tenantId)
              .onSuccess(jobProfileCollection -> {
                LOGGER.info("Successfully fetched jobProfiles while querying job execution for tenant {}", tenantId);
                jobExecutionCollection.getJobExecutions().forEach(jobExecution ->
                  jobProfileCollection.getJobProfiles()
                    .stream()
                    .filter(jobProfile -> jobProfile.getId().equals(jobExecution.getJobProfileId()))
                    .forEach(jobProfile -> jobExecution.setJobProfileName(jobProfile.getName())));
                jobExecutionPromise.complete(jobExecutionCollection);
              })
              .onFailure(async -> {
                LOGGER.error("Failed to fetch job profiles while getting job executions by query for tenant {}. An empty jobProfileName will be used for those jobs that do not have jobProfileName", tenantId);
                jobExecutionCollection.getJobExecutions()
                  .forEach(JobExecutionServiceImpl::populateEmptyJobProfileName);
                jobExecutionPromise.complete(jobExecutionCollection);
              });
          }
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
    return jobExecutionDao.getById(jobExecutionId, tenantId)
      .compose(optionalJobExecution -> {
        if (optionalJobExecution.isPresent()) {
          return isNotEmpty(optionalJobExecution.get().getJobProfileId())
            ? populateJobProfileNameIfNecessary(optionalJobExecution.get(), tenantId)
            : succeededFuture(optionalJobExecution.get());
        } else {
          String errorMessage = String.format("Job execution not found with id %s", jobExecutionId);
          LOGGER.error(errorMessage);
          return failedFuture(new NotFoundException(errorMessage));
        }
      });
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
  public Future<JobExecution> prepareJobForExport(String id, FileDefinition fileExportDefinition, JsonObject user, int totalCount, boolean withProgress, String tenantId) {
    return getById(id, tenantId).compose(jobExecution -> {
      prepareJobExecution(jobExecution, fileExportDefinition, IN_PROGRESS, user, totalCount, withProgress);
      return update(jobExecution, tenantId);
    });
  }

  @Override
  public void prepareAndSaveJobForFailedExport(JobExecution jobExecution, FileDefinition fileExportDefinition, JsonObject user, int totalCount, boolean withProgress, String tenantId) {
    prepareJobExecution(jobExecution, fileExportDefinition, FAIL, user, totalCount, withProgress);
    jobExecution.setCompletedDate(new Date());
    update(jobExecution, tenantId);
  }

  private void prepareJobExecution(JobExecution jobExecution, FileDefinition fileExportDefinition, JobExecution.Status status, JsonObject user, int totalCount, boolean withProgress) {
    ExportedFile exportedFile = new ExportedFile()
      .withFileId(UUID.randomUUID().toString())
      .withFileName(fileExportDefinition.getFileName());
    Set<ExportedFile> exportedFiles = jobExecution.getExportedFiles();
    exportedFiles.add(exportedFile);
    jobExecution.setExportedFiles(exportedFiles);
    jobExecution.setStatus(status);
    if (Objects.isNull(jobExecution.getStartedDate())) {
      jobExecution.setStartedDate(new Date());
      jobExecution.setLastUpdatedDate(new Date());
    }
    JsonObject personal = user.getJsonObject("personal");
    jobExecution.setRunBy(new RunBy()
      .withFirstName(personal.getString("firstName"))
      .withLastName(personal.getString("lastName")));
    if (withProgress) {
      jobExecution.setProgress(new Progress().withTotal(totalCount));
    }
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
            jobExecution.setLastUpdatedDate(new Date());
            return jobExecutionDao.update(jobExecution, tenantId);
          }
          return failedFuture(format("Unable to update progress of job execution with id %s", jobExecutionId));
        }
        return failedFuture(format("Job execution with id %s doesn't exist", jobExecutionId));
      });
  }

  @Override
  public Future<Void> expireJobExecutions(String tenantId) {
    Promise<Void> jobExecutionPromise = Promise.promise();
    //expire entries that have last updated date greater then 1 hr
    jobExecutionDao.getExpiredEntries(new Date(new Date().getTime() - 3600_000), tenantId)
      .onComplete(asyncResult -> {
        if (asyncResult.succeeded()) {
          List<JobExecution> jobExecutionList = asyncResult.result();
          jobExecutionList.forEach(jobExe -> {
            jobExe.setStatus(FAIL);
            jobExecutionDao.update(jobExe, tenantId);
          });
          jobExecutionPromise.complete();
        } else {
          String errorMessage = String.format("Fail to fetch expired job executions, cause %s", asyncResult.cause());
          LOGGER.error(errorMessage);
          jobExecutionPromise.fail(errorMessage);
        }

      });
    return jobExecutionPromise.future();
  }

  private Future<JobExecution> populateJobProfileNameIfNecessary(JobExecution jobExecution, String tenantId) {
    Promise<JobExecution> jobExecutionPromise = Promise.promise();
    jobProfileService.getById(jobExecution.getJobProfileId(), tenantId)
      .onSuccess(jobProfile -> {
        jobExecution.setJobProfileName(jobProfile.getName());
        jobExecutionPromise.complete(jobExecution);
      })
      .onFailure(ar -> {
        LOGGER.error("Failed to fetch job profiles while getting job executions by id for tenant {}. " +
          "An empty jobProfileName will be used for jobExecution with id {}", tenantId, jobExecution.getId());
        populateEmptyJobProfileName(jobExecution);
        jobExecutionPromise.complete(jobExecution);
      });
    return jobExecutionPromise.future();
  }

  private static void populateEmptyJobProfileName(JobExecution jobExecution) {
    if (StringUtils.isEmpty(jobExecution.getJobProfileName())) {
      jobExecution.setJobProfileName(StringUtils.EMPTY);
    }
  }

  private String getAssociatedJobProfileIdsQuery(JobExecutionCollection jobExecutionCollection) {
    return String.format("(%s)", jobExecutionCollection
      .getJobExecutions()
      .stream()
      .filter(jobExecution -> isNotEmpty(jobExecution.getJobProfileId()))
      .map(jobExecution -> "id==" + jobExecution.getJobProfileId())
      .collect(Collectors.joining(" or ")));
  }

  @Override
  public Future<Boolean> deleteById(String id, String tenantId) {
    return jobExecutionDao.deleteById(id, tenantId);

  }

}
