package org.folio.service.job;


import io.vertx.core.Future;

import java.util.Optional;

import org.folio.dao.JobExecutionDao;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobExecutionCollection;
import org.folio.rest.jaxrs.model.Progress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

/**
 * Implementation of the JobExecutionService, calls JobExecutionDao to access JobExecution metadata.
 */
@Service
public class JobExecutionServiceImpl implements JobExecutionService {
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
  public Future<Optional<JobExecution>> getById(final String jobExecutionId, final String tenantId) {
    return jobExecutionDao.getById(jobExecutionId, tenantId);
  }

  @Override
  public Future<JobExecution> incrementCurrentProgress(final String jobExecutionId, final int delta, final String tenantId ) {
        return jobExecutionDao.getById(jobExecutionId, tenantId)
          .compose(jobExecutionOptional ->{
            if(jobExecutionOptional.isPresent()) {
              JobExecution jobExecution = jobExecutionOptional.get();
              Progress progress = jobExecution.getProgress();
              if(nonNull(progress)) {
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
