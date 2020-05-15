package org.folio.service.job;

import com.google.common.collect.Sets;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.dao.impl.JobExecutionDaoImpl;
import org.folio.rest.jaxrs.model.ExportedFile;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.Progress;
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
  private static final String FILE_DEFINITION_FILE_NAME = "fileName";
  private static final String PERSONAL_KEY = "personal";
  private static final String FIRST_NAME_KEY = "firstName";
  private static final String FIRST_NAME_VALUE = "firstName";
  private static final String LAST_NAME_KEY = "lastName";
  private static final String LAST_NAME_VALUE = "lastName";
  private static final long TOTAL_COUNT_LONG = 2L;
  private static final String TOTAL_COUNT_STRING = "2";

  @Spy
  @InjectMocks
  private JobExecutionServiceImpl jobExecutionService;

  @Mock
  private JobExecutionDaoImpl jobExecutionDao;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void shouldIncrementCurrentProgress(TestContext context) {
    //given
    Async async = context.async();
    JobExecution job = new JobExecution().withProgress(new Progress().withExported(10).withFailed(3));
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.of(job)));
    when(jobExecutionDao.update(job, TENANT_ID)).thenReturn(Future.succeededFuture(job));
    //when
    Future<JobExecution> future = jobExecutionService.incrementCurrentProgress(JOB_EXECUTION_ID, 5, 1, TENANT_ID);
    //then
    future.setHandler(ar -> {
      context.assertTrue(ar.succeeded());
      context.assertEquals(15, job.getProgress().getExported());
      context.assertEquals(4, job.getProgress().getFailed());
      async.complete();
    });
  }

  @Test
  public void getById_shouldReturnFailedFuture_whenJobExecutionDoesNotExist(TestContext context) {
    //given
    Async async = context.async();
    String errorMessage = String.format("Job execution not found with id %s", JOB_EXECUTION_ID);
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.empty()));
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
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.of(jobExecution)));
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
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.empty()));
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
  public void shouldPrepareJobExecutionSuccessfully_whenJobExecutionStartDateIsNull(TestContext context) {
    //given
    Async async = context.async();
    JobExecution jobExecution = new JobExecution()
      .withExportedFiles(Sets.newHashSet());
    FileDefinition fileDefinition = new FileDefinition()
      .withFileName(FILE_DEFINITION_FILE_NAME);
    JsonObject user = new JsonObject()
      .put(PERSONAL_KEY, new JsonObject()
        .put(FIRST_NAME_KEY, FIRST_NAME_VALUE)
        .put(LAST_NAME_KEY, LAST_NAME_VALUE));
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.of(jobExecution)));
    when(jobExecutionDao.update(jobExecution, TENANT_ID)).thenReturn(Future.succeededFuture(jobExecution));

    //when
    Future<JobExecution> future = jobExecutionService.prepareJobForExport(JOB_EXECUTION_ID, fileDefinition, user, TOTAL_COUNT_LONG, TENANT_ID);

    //then
    future.setHandler(ar -> {
      context.assertTrue(ar.succeeded());
      JobExecution updatedJobExecution = ar.result();
      ExportedFile exportedFile = getFirstExportedFile(updatedJobExecution);
      context.assertNotNull(exportedFile.getFileId());
      context.assertEquals(FILE_DEFINITION_FILE_NAME, exportedFile.getFileName());
      context.assertEquals(FIRST_NAME_VALUE, updatedJobExecution.getRunBy().getFirstName());
      context.assertEquals(LAST_NAME_VALUE, updatedJobExecution.getRunBy().getLastName());
      context.assertEquals(TOTAL_COUNT_STRING, updatedJobExecution.getProgress().getTotal());
      async.complete();
    });
  }

  private ExportedFile getFirstExportedFile(JobExecution updatedJobExecution) {
    return updatedJobExecution.getExportedFiles().iterator().next();
  }
}
