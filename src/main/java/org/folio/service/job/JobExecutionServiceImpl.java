package org.folio.service.job;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.dao.JobExecutionDao;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.ExportedFile;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobExecutionCollection;
import org.folio.rest.jaxrs.model.Progress;
import org.folio.rest.jaxrs.model.RunBy;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.profiles.jobprofile.JobProfileService;
import org.folio.util.ErrorCode;
import org.folio.util.HelperUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.rest.jaxrs.model.JobExecution.Status.FAIL;
import static org.folio.rest.jaxrs.model.JobExecution.Status.IN_PROGRESS;

/**
 * Implementation of the JobExecutionService, calls JobExecutionDao to access JobExecution metadata.
 */
@Service
public class JobExecutionServiceImpl implements JobExecutionService {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private JobExecutionDao jobExecutionDao;
  @Autowired
  private JobProfileService jobProfileService;
  @Autowired
  private ExportStorageService exportStorageService;
  @Autowired
  private ErrorLogService errorLogService;

  @Override
  public Future<JobExecutionCollection> get(String query, int offset, int limit, String tenantId) {
    Promise<JobExecutionCollection> jobExecutionPromise = Promise.promise();
    jobExecutionDao.get(query, offset, limit, tenantId)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          jobExecutionPromise.complete(ar.result());
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
  public void updateJobStatusById(String id, JobExecution jobExecution, JobExecution.Status status, String tenantId) {
    if (isNull(jobExecution)) {
      getById(id, tenantId)
        .onSuccess(execution -> update(updateJobStatus(execution, status), tenantId));
    } else {
      populateJobProfileNameIfNecessary(jobExecution, tenantId)
        .onSuccess(execution -> update(updateJobStatus(jobExecution, status), tenantId));
    }
  }

  private JobExecution updateJobStatus(JobExecution jobExecution, JobExecution.Status status) {
    return jobExecution.withStatus(status).withCompletedDate(new Date());
  }

  @Override
  public Future<JobExecution> prepareJobForExport(JobExecution jobExecution, FileDefinition fileExportDefinition, JsonObject user, int totalCount, boolean withProgress, String tenantId) {
    return populateJobProfileNameIfNecessary(jobExecution, tenantId).compose(execution -> {
      prepareJobExecution(execution, fileExportDefinition, IN_PROGRESS, user, totalCount, withProgress);
      return update(execution, tenantId);
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
            //reset progress to skip already exported/failed records
            if (jobExe.getProgress() != null) {
              jobExe.getProgress().withExported(0).withTotal(0).withFailed(0);
            }
            jobExe.setCompletedDate(new Date());
            updateErrorLogIfJobIsExpired(jobExe.getId(), tenantId);
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

  private void updateErrorLogIfJobIsExpired(String jobExecutionId, String tenantId) {
    Future<List<ErrorLog>> listFuture = errorLogService.getByQuery(HelperUtils.getErrorLogCriterionByJobExecutionId(jobExecutionId), tenantId);
    if (listFuture != null) {
      listFuture.onSuccess(errorLogs -> {
        if (!errorLogs.isEmpty()) {
          ErrorLog errorLog = errorLogs.get(0)
            .withErrorMessageCode(ErrorCode.ERROR_JOB_IS_EXPIRED.getCode())
            .withErrorMessageValues(List.of(ErrorCode.ERROR_JOB_IS_EXPIRED.getDescription()));
          errorLogService.update(errorLog, tenantId);
          //remove all the rest logs to have only 1 reason: job is expired
          errorLogs.subList(1, errorLogs.size()).forEach(redundantLog -> {
            errorLogService.deleteById(redundantLog.getId(), tenantId);
          });
        }
      });
    }
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

  @Override
  public Future<Boolean> deleteById(String id, String tenantId) {
    Promise<Boolean> promise = Promise.promise();
    jobExecutionDao.getById(id, tenantId)
      .onSuccess(jobExecution -> {
        if (jobExecution.isPresent() && !IN_PROGRESS.equals(jobExecution.get().getStatus())) {
          jobExecutionDao.deleteById(id, tenantId)
            .onSuccess(result -> {
              exportStorageService.removeFilesRelatedToJobExecution(jobExecution.get(), tenantId);
              promise.complete(result);
            })
            .onFailure(ar -> promise.fail(ar.getCause()));
        } else {
          jobExecution.ifPresentOrElse(
            job -> promise.fail(new ServiceException(HttpStatus.HTTP_FORBIDDEN, String.format("Fail to delete jobExecution with id %s, status is IN_PROGRESS", job.getId()))),
            () -> promise.complete(false));
        }
      }).onFailure(ar -> promise.fail(ar.getCause()));
    return promise.future();
  }

}
