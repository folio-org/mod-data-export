package org.folio.service.job;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Sets;
import org.folio.dao.impl.JobExecutionDaoImpl;
import org.folio.rest.jaxrs.model.ExportedFile;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.jaxrs.model.Progress;
import org.folio.service.profiles.jobprofile.JobProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class JobExecutionServiceUnitTest {
  private static final String JOB_EXECUTION_ID = UUID.randomUUID().toString();
  private static final String JOB_PROFILE_ID = UUID.randomUUID().toString();
  private static final String JOB_PROFILE_NAME = "Job profile";
  private static final String DEFAULT_JOB_PROFILE_NAME = "default";
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
  @Mock
  private JobProfileService jobProfileService;


  @Test
  void getById_shouldReturnFailedFuture_whenJobExecutionDoesNotExist(VertxTestContext context) {
    //given
    String errorMessage = String.format("Job execution not found with id %s", JOB_EXECUTION_ID);
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.empty()));
    //when
    Future<JobExecution> future = jobExecutionService.getById(JOB_EXECUTION_ID, TENANT_ID);
    //then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.failed());
        assertEquals(ar.cause().getMessage(), errorMessage);
        context.completeNow();
      });

    });
  }

  @Test
  void getById_shouldReturnSucceededFuture_withDefaultJobProfileName_whenJobProfileNotFound(VertxTestContext context) {
    //given
    JobExecution jobExecution = new JobExecution()
      .withId(JOB_EXECUTION_ID)
      .withJobProfileId(JOB_PROFILE_ID);
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.of(jobExecution)));
    when(jobProfileService.getById(JOB_PROFILE_ID, TENANT_ID)).thenReturn(Future.failedFuture(StringUtils.EMPTY));
    //when
    Future<JobExecution> future = jobExecutionService.getById(JOB_EXECUTION_ID, TENANT_ID);
    //then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.succeeded());
        JobExecution fetchedJobExecution = ar.result();
        assertEquals(DEFAULT_JOB_PROFILE_NAME, fetchedJobExecution.getJobProfileName());
        context.completeNow();
      });
    });
  }

  @Test
  void getById_shouldReturnSucceededFuture_withExistingJobProfileName_whenJobProfileNotFound(VertxTestContext context) {
    //given
    JobExecution jobExecution = new JobExecution()
      .withId(JOB_EXECUTION_ID)
      .withJobProfileId(JOB_PROFILE_ID)
      .withJobProfileName(JOB_PROFILE_NAME);
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.of(jobExecution)));
    when(jobProfileService.getById(JOB_PROFILE_ID, TENANT_ID)).thenReturn(Future.failedFuture(StringUtils.EMPTY));
    //when
    Future<JobExecution> future = jobExecutionService.getById(JOB_EXECUTION_ID, TENANT_ID);
    //then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.succeeded());
        JobExecution fetchedJobExecution = ar.result();
        assertEquals(JOB_PROFILE_NAME, fetchedJobExecution.getJobProfileName());
        context.completeNow();
      });
    });
  }

  @Test
  void incrementCurrentProgress_shouldIncrement(VertxTestContext context) {
    //given
    JobExecution job = new JobExecution().withProgress(new Progress().withExported(10).withFailed(3));
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.of(job)));
    when(jobExecutionDao.update(job, TENANT_ID)).thenReturn(Future.succeededFuture(job));
    //when
    Future<JobExecution> future = jobExecutionService.incrementCurrentProgress(JOB_EXECUTION_ID, 5, 1, TENANT_ID);
    //then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.succeeded());
        assertEquals(15, job.getProgress().getExported().intValue());
        assertEquals(4, job.getProgress().getFailed().intValue());
        context.completeNow();
      });
    });
  }

  @Test
  void incrementCurrentProgress_shouldReturnFailedFuture_whenProgressIsAbsent(VertxTestContext context) {
    //given
    String errorMessage = String.format("Unable to update progress of job execution with id %s", JOB_EXECUTION_ID);
    JobExecution jobExecution = new JobExecution();
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.of(jobExecution)));
    //when
    Future<JobExecution> future = jobExecutionService.incrementCurrentProgress(JOB_EXECUTION_ID, 0, 0, TENANT_ID);
    //then
    future.onComplete(ar -> {

      context.verify(() -> {
        assertTrue(ar.failed());
        assertEquals(ar.cause()
          .getMessage(), errorMessage);
        context.completeNow();
      });

    });
  }

  @Test
  void incrementCurrentProgress_shouldReturnFailedFuture_whenJobExecutionIsAbsent(VertxTestContext context) {
    //given
    String errorMessage = String.format("Job execution with id %s doesn't exist", JOB_EXECUTION_ID);
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.empty()));
    //when
    Future<JobExecution> future = jobExecutionService.incrementCurrentProgress(JOB_EXECUTION_ID, 0, 0, TENANT_ID);
    //then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.failed());
        assertEquals(ar.cause().getMessage(), errorMessage);
        context.completeNow();
      });

    });
  }

  @Test
  void shouldPrepareJobExecutionSuccessfully_whenJobExecutionStartDateIsNull(VertxTestContext context) {
    //given
    JobExecution jobExecution = new JobExecution()
      .withExportedFiles(Sets.newHashSet())
      .withJobProfileId(JOB_PROFILE_ID);
    FileDefinition fileDefinition = new FileDefinition()
      .withFileName(FILE_DEFINITION_FILE_NAME);
    JsonObject user = new JsonObject()
      .put(PERSONAL_KEY, new JsonObject()
        .put(FIRST_NAME_KEY, FIRST_NAME_VALUE)
        .put(LAST_NAME_KEY, LAST_NAME_VALUE));
    JobProfile jobProfile = new JobProfile()
      .withId(JOB_PROFILE_ID)
      .withName(JOB_PROFILE_NAME);
    when(jobProfileService.getById(JOB_PROFILE_ID, TENANT_ID)).thenReturn(Future.succeededFuture(jobProfile));
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.of(jobExecution)));
    when(jobExecutionDao.update(jobExecution, TENANT_ID)).thenReturn(Future.succeededFuture(jobExecution));

    //when
    Future<JobExecution> future = jobExecutionService.prepareJobForExport(JOB_EXECUTION_ID, fileDefinition, user, TOTAL_COUNT_LONG, TENANT_ID);

    //then
    future.onComplete(ar -> {
      context.verify(() -> {
        assertTrue(ar.succeeded());
        JobExecution updatedJobExecution = ar.result();
        ExportedFile exportedFile = getFirstExportedFile(updatedJobExecution);
        assertNotNull(exportedFile.getFileId());
        assertEquals(FILE_DEFINITION_FILE_NAME, exportedFile.getFileName());
        assertEquals(FIRST_NAME_VALUE, updatedJobExecution.getRunBy()
          .getFirstName());
        assertEquals(LAST_NAME_VALUE, updatedJobExecution.getRunBy()
          .getLastName());
        assertEquals(TOTAL_COUNT_STRING, updatedJobExecution.getProgress()
          .getTotal());
        assertEquals(JOB_PROFILE_NAME, jobExecution.getJobProfileName());
        context.completeNow();
      });
    });
  }

  private ExportedFile getFirstExportedFile(JobExecution updatedJobExecution) {
    return updatedJobExecution.getExportedFiles().iterator().next();
  }

}
