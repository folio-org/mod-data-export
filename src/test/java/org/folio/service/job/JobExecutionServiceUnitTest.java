package org.folio.service.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Optional;
import java.util.UUID;
import org.folio.dao.impl.JobExecutionDaoImpl;
import org.folio.rest.jaxrs.model.JobExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class JobExecutionServiceUnitTest {
  private static final String JOB_EXECUTION_ID = UUID.randomUUID().toString();
  private static final String TENANT_ID = "diku";

  @Spy
  @InjectMocks
  JobExecutionServiceImpl jobExecutionService;

  @Mock
  JobExecutionDaoImpl jobExecutionDao;


  @Test
  public void getById_shouldReturnFailedFuture_whenJobExecutionDoesNotExist(VertxTestContext context) {
    //given
    String errorMessage = String.format("Job execution not found with id %s", JOB_EXECUTION_ID);
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, "diku")).thenReturn(Future.succeededFuture(Optional.empty()));
    //when
    Future<JobExecution> future = jobExecutionService.getById(JOB_EXECUTION_ID, TENANT_ID);
    //then
    future.setHandler(ar -> {
      context.verify(() -> {
        assertTrue(ar.failed());
        assertEquals(ar.cause().getMessage(), errorMessage);
        context.completeNow();
      });

    });
  }

  @Test
  public void incrementCurrentProgress_shouldReturnFailedFuture_whenProgressIsAbsent(VertxTestContext context) {
    //given
    String errorMessage = String.format("Unable to update progress of job execution with id %s", JOB_EXECUTION_ID);
    JobExecution jobExecution = new JobExecution();
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, "diku")).thenReturn(Future.succeededFuture(Optional.of(jobExecution)));
    //when
    Future<JobExecution> future = jobExecutionService.incrementCurrentProgress(JOB_EXECUTION_ID, 0, TENANT_ID);
    //then
    future.setHandler(ar -> {

      context.verify(() -> {
        assertTrue(ar.failed());
        assertEquals(ar.cause()
          .getMessage(), errorMessage);
        context.completeNow();
      });

    });
  }

  @Test
  public void incrementCurrentProgress_shouldReturnFailedFuture_whenJobExecutionIsAbsent(VertxTestContext context) {
    //given
    String errorMessage = String.format("Job execution with id %s doesn't exist", JOB_EXECUTION_ID);
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, "diku")).thenReturn(Future.succeededFuture(Optional.empty()));
    //when
    Future<JobExecution> future = jobExecutionService.incrementCurrentProgress(JOB_EXECUTION_ID, 0, TENANT_ID);
    //then
    future.setHandler(ar -> {
      context.verify(() -> {
        assertTrue(ar.failed());
        assertEquals(ar.cause().getMessage(), errorMessage);
        context.completeNow();
      });

    });
  }

}
