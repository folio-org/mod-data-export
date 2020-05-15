package org.folio.service.job;

import io.vertx.core.Future;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.dao.impl.JobExecutionDaoImpl;
import org.folio.rest.jaxrs.model.JobExecution;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
public class JobExecutionServiceUnitTest {

  private static final String JOB_EXECUTION_ID = UUID.randomUUID().toString();
  private static final String TENANT_ID = "diku";

  @Spy
  @InjectMocks
  JobExecutionServiceImpl jobExecutionService;

  @Mock
  JobExecutionDaoImpl jobExecutionDao;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void getById_shouldReturnFailedFuture_whenJobExecutionDoesNotExist(TestContext context) {
    //given
    Async async = context.async();
    String errorMessage = String.format("Job execution not found with id %s", JOB_EXECUTION_ID);
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, "diku")).thenReturn(Future.succeededFuture(Optional.empty()));
    //when
    Future<JobExecution> future = jobExecutionService.getById(JOB_EXECUTION_ID, TENANT_ID);
    //then
    future.setHandler(ar -> {
      context.assertTrue(ar.failed());
      Assert.assertEquals(ar.cause().getMessage(), errorMessage);
      async.complete();
    });
  }

  @Test
  public void incrementCurrentProgress_shouldReturnFailedFuture_whenProgressIsAbsent(TestContext context) {
    //given
    Async async = context.async();
    String errorMessage = String.format("Unable to update progress of job execution with id %s", JOB_EXECUTION_ID);
    JobExecution jobExecution = new JobExecution();
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, "diku")).thenReturn(Future.succeededFuture(Optional.of(jobExecution)));
    //when
    Future<JobExecution> future = jobExecutionService.incrementCurrentProgress(JOB_EXECUTION_ID, 0, 0, TENANT_ID);
    //then
    future.setHandler(ar -> {
      context.assertTrue(ar.failed());
      Assert.assertEquals(ar.cause().getMessage(), errorMessage);
      async.complete();
    });
  }

  @Test
  public void incrementCurrentProgress_shouldReturnFailedFuture_whenJobExecutionIsAbsent(TestContext context) {
    //given
    Async async = context.async();
    String errorMessage = String.format("Job execution with id %s doesn't exist", JOB_EXECUTION_ID);
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, "diku")).thenReturn(Future.succeededFuture(Optional.empty()));
    //when
    Future<JobExecution> future = jobExecutionService.incrementCurrentProgress(JOB_EXECUTION_ID, 0, 0, TENANT_ID);
    //then
    future.setHandler(ar -> {
      context.assertTrue(ar.failed());
      Assert.assertEquals(ar.cause().getMessage(), errorMessage);
      async.complete();
    });
  }

}
