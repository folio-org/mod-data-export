package org.folio.service.job;


import io.vertx.core.Future;

import java.util.Optional;

import org.folio.dao.JobExecutionDao;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobExecutionCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
