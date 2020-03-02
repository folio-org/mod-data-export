package org.folio.service.job;


import io.vertx.core.Future;
import org.folio.dao.JobExecutionDao;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobExecutionCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Implementation of the JobExecutionService, calls JobExecutionDao to access JobExecution metadata.
 *
 */
@Service
public class JobExecutionServiceImpl implements JobExecutionService {

  @Autowired
  private JobExecutionDao jobExecutionDao;

  @Override
  public Future<JobExecutionCollection> getJobExecutions(String query, int offset, int limit, String tenantId) {
    return jobExecutionDao.getJobExecutions(query, offset, limit, tenantId);
  }

  @Override
  public Future<JobExecution> saveJobExecution(JobExecution jobExecution, String tenantId) {
    return jobExecutionDao.saveJobExecution(jobExecution, tenantId);
  }
}
