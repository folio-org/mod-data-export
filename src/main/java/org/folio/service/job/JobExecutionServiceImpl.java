package org.folio.service.job;


import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.dao.JobExecutionDao;
import org.folio.rest.jaxrs.model.ExportedFile;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobExecutionCollection;
import org.folio.rest.jaxrs.model.Progress;
import org.folio.rest.jaxrs.model.RunBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static java.util.Objects.nonNull;

/**
 * Implementation of the JobExecutionService, calls JobExecutionDao to access JobExecution metadata.
 */
@Service
public class JobExecutionServiceImpl implements JobExecutionService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private JobExecutionDao jobExecutionDao;

  @Override
  public Future<JobExecutionCollection> get(String query, int offset, int limit, String tenantId) {
    return jobExecutionDao.get(query, offset, limit, tenantId);
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
          return succeededFuture(optionalJobExecution.get());
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
      jobExecution.setProgress(new Progress()
        .withTotal(String.valueOf(totalCount)));
      return update(jobExecution, tenantId);
    });
  }


  @Override
  public Future<JobExecution> incrementCurrentProgress(final String jobExecutionId, final int delta, final String tenantId) {
    return jobExecutionDao.getById(jobExecutionId, tenantId)
      .compose(jobExecutionOptional -> {
        if (jobExecutionOptional.isPresent()) {
          JobExecution jobExecution = jobExecutionOptional.get();
          Progress progress = jobExecution.getProgress();
          if (nonNull(progress)) {
            int current = nonNull(progress.getCurrent()) ? progress.getCurrent() : 0;
            int incrementedCurrent = current + delta;
            progress.setCurrent(incrementedCurrent);
            return jobExecutionDao.update(jobExecution, tenantId);
          }
          return Future.failedFuture(format("Unable to update progress of job execution with id %s", jobExecutionId));
        }
        return Future.failedFuture(format("Job execution with id %s doesn't exist", jobExecutionId));
      });
  }
}
