package org.folio.service.job;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Sets;
import org.folio.HttpStatus;
import org.folio.dao.impl.JobExecutionDaoImpl;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.ExportedFile;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobExecutionCollection;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.jaxrs.model.JobProfileCollection;
import org.folio.rest.jaxrs.model.Progress;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.profiles.jobprofile.JobProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.folio.rest.jaxrs.model.JobExecution.Status.FAIL;
import static org.folio.rest.jaxrs.model.JobExecution.Status.IN_PROGRESS;
import static org.folio.rest.jaxrs.model.JobExecution.Status.NEW;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class JobExecutionServiceUnitTest {
  private static final String JOB_EXECUTION_ID = UUID.randomUUID().toString();
  private static final String JOB_PROFILE_ID = UUID.randomUUID().toString();
  private static final String SECOND_JOB_PROFILE_ID = UUID.randomUUID().toString();
  private static final String JOB_PROFILE_NAME = "Job profile";
  private static final String DEFAULT_JOB_PROFILE_NAME = "default";
  private static final String TENANT_ID = "diku";
  private static final String FILE_DEFINITION_FILE_NAME = "fileName";
  private static final String PERSONAL_KEY = "personal";
  private static final String FIRST_NAME_KEY = "firstName";
  private static final String FIRST_NAME_VALUE = "firstName";
  private static final String LAST_NAME_KEY = "lastName";
  private static final String LAST_NAME_VALUE = "lastName";
  private static final int TOTAL_COUNT = 2;

  @Spy
  @InjectMocks
  private JobExecutionServiceImpl jobExecutionService;

  @Mock
  private JobExecutionDaoImpl jobExecutionDao;
  @Mock
  private JobProfileService jobProfileService;
  @Mock
  private ExportStorageService exportStorageService;
  @Mock
  private ErrorLogService errorLogService;


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
  void getById_shouldReturnSucceededFuture_withUpdatedJobProfileName_whenJobProfileIsPresent(VertxTestContext context) {
    //given
    JobExecution jobExecution = new JobExecution()
      .withId(JOB_EXECUTION_ID)
      .withJobProfileId(JOB_PROFILE_ID)
      .withJobProfileName(StringUtils.EMPTY);
    JobProfile jobProfile = new JobProfile()
      .withId(JOB_PROFILE_ID)
      .withName(JOB_PROFILE_NAME);
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.of(jobExecution)));
    when(jobProfileService.getById(JOB_PROFILE_ID, TENANT_ID)).thenReturn(Future.succeededFuture(jobProfile));
    //when
    Future<JobExecution> future = jobExecutionService.getById(JOB_EXECUTION_ID, TENANT_ID);
    //then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      JobExecution fetchedJobExecution = ar.result();
      assertEquals(JOB_PROFILE_NAME, fetchedJobExecution.getJobProfileName());
      context.completeNow();
    }));
  }

  @Test
  void getByQuery_shouldReturnSucceededFuture_withUpdatedJobProfileName_whenJobProfilesIsPresent(VertxTestContext context) {
    //given
    JobExecution jobExecution = new JobExecution()
      .withId(JOB_EXECUTION_ID)
      .withJobProfileId(JOB_PROFILE_ID)
      .withJobProfileName(StringUtils.EMPTY);
    JobExecution secondJobExecution = new JobExecution()
      .withId(UUID.randomUUID().toString())
      .withJobProfileId(JOB_PROFILE_ID)
      .withJobProfileName("Job");
    JobExecution thirdJobExecution = new JobExecution()
      .withId(UUID.randomUUID().toString())
      .withJobProfileId(SECOND_JOB_PROFILE_ID)
      .withJobProfileName(StringUtils.EMPTY);
    JobProfile jobProfile = new JobProfile()
      .withId(JOB_PROFILE_ID)
      .withName(JOB_PROFILE_NAME);
    JobProfile secondJobProfile = new JobProfile()
      .withId(SECOND_JOB_PROFILE_ID)
      .withName(DEFAULT_JOB_PROFILE_NAME);
    JobProfile third = new JobProfile()
      .withId(UUID.randomUUID().toString())
      .withName(DEFAULT_JOB_PROFILE_NAME);
    String query = "id=" + jobExecution.getId();
    when(jobExecutionDao.get(query, 0, 10, TENANT_ID)).thenReturn(Future.succeededFuture(new JobExecutionCollection()
      .withJobExecutions(asList(jobExecution, secondJobExecution, thirdJobExecution))));
    when(jobProfileService.get(anyString(), eq(0), eq(10), eq(TENANT_ID))).thenReturn(Future.succeededFuture(new JobProfileCollection().withJobProfiles(asList(third, secondJobProfile, jobProfile, secondJobProfile))));
    //when
    Future<JobExecutionCollection> future = jobExecutionService.get(query, 0, 10, TENANT_ID);
    //then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      JobExecution fetchedJobExecution = ar.result().getJobExecutions().get(0);
      assertEquals(JOB_PROFILE_NAME, fetchedJobExecution.getJobProfileName());
      JobExecution fetchedSecondJobExecution = ar.result().getJobExecutions().get(1);
      assertEquals(JOB_PROFILE_NAME, fetchedSecondJobExecution.getJobProfileName());
      JobExecution fetchedThirdJobExecution = ar.result().getJobExecutions().get(2);
      assertEquals(DEFAULT_JOB_PROFILE_NAME, fetchedThirdJobExecution.getJobProfileName());
      context.completeNow();
    }));
  }

  @Test
  void getById_shouldReturnSucceededFuture_withAbsentJobProfileName_whenJobProfileNotFound(VertxTestContext context) {
    //given
    JobExecution jobExecution = new JobExecution()
      .withId(JOB_EXECUTION_ID)
      .withJobProfileId(JOB_PROFILE_ID);
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.of(jobExecution)));
    when(jobProfileService.getById(JOB_PROFILE_ID, TENANT_ID)).thenReturn(Future.failedFuture(StringUtils.EMPTY));
    //when
    Future<JobExecution> future = jobExecutionService.getById(JOB_EXECUTION_ID, TENANT_ID);
    //then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      JobExecution fetchedJobExecution = ar.result();
      assertEquals(StringUtils.EMPTY, fetchedJobExecution.getJobProfileName());
      context.completeNow();
    }));
  }

  @Test
  void getByQuery_shouldReturnSucceededFuture_withCorrectJobProfileName_whenJobProfilesNotFound(VertxTestContext context) {
    //given
    JobExecution jobExecution = new JobExecution()
      .withId(JOB_EXECUTION_ID)
      .withJobProfileId(JOB_PROFILE_ID);
    JobExecution secondJobExecution = new JobExecution()
      .withId(UUID.randomUUID().toString())
      .withJobProfileId(JOB_PROFILE_ID)
      .withJobProfileName(StringUtils.EMPTY);
    String query = "id=" + jobExecution.getId();
    when(jobExecutionDao.get(query, 0, 10, TENANT_ID)).thenReturn(Future.succeededFuture(new JobExecutionCollection()
      .withJobExecutions(asList(jobExecution, secondJobExecution))));
    when(jobProfileService.get(anyString(), eq(0), eq(10), eq(TENANT_ID))).thenReturn(Future.failedFuture(StringUtils.EMPTY));
    //when
    Future<JobExecutionCollection> future = jobExecutionService.get(query, 0, 10, TENANT_ID);
    //then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      JobExecution fetchedJobExecution = ar.result().getJobExecutions().get(0);
      assertEquals(StringUtils.EMPTY, fetchedJobExecution.getJobProfileName());
      JobExecution fetchedSecondJobExecution = ar.result().getJobExecutions().get(1);
      assertEquals(StringUtils.EMPTY, fetchedSecondJobExecution.getJobProfileName());
      context.completeNow();
    }));
  }

  @Test
  void getByQuery_shouldReturnSucceededFuture_withEmptyJobExecutionCollection_whenDaoReturnedEmptyCollection(VertxTestContext context) {
    //given
    JobExecution jobExecution = new JobExecution()
      .withId(JOB_EXECUTION_ID)
      .withJobProfileId(JOB_PROFILE_ID);
    JobExecution secondJobExecution = new JobExecution()
      .withId(UUID.randomUUID().toString())
      .withJobProfileId(JOB_PROFILE_ID)
      .withJobProfileName(StringUtils.EMPTY);
    String query = "id=" + jobExecution.getId();
    when(jobExecutionDao.get(query, 0, 10, TENANT_ID)).thenReturn(Future.succeededFuture(new JobExecutionCollection()));
    //when
    Future<JobExecutionCollection> future = jobExecutionService.get(query, 0, 10, TENANT_ID);
    //then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      assertTrue(CollectionUtils.isEmpty(ar.result().getJobExecutions()));
      Mockito.verify(jobProfileService, Mockito.never()).get(anyString(), anyInt(), anyInt(), anyString());
      context.completeNow();
    }));
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
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      JobExecution fetchedJobExecution = ar.result();
      assertEquals(JOB_PROFILE_NAME, fetchedJobExecution.getJobProfileName());
      context.completeNow();
    }));
  }

  @Test
  void getByQuery_shouldReturnSucceededFuture_withExistingJobProfileName_whenJobProfileNotFound(VertxTestContext context) {
    //given
    JobExecution jobExecution = new JobExecution()
      .withId(JOB_EXECUTION_ID)
      .withJobProfileId(JOB_PROFILE_ID)
      .withJobProfileName(JOB_PROFILE_NAME);
    String query = "id=" + jobExecution.getId();
    when(jobExecutionDao.get(query, 0, 10, TENANT_ID)).thenReturn(Future.succeededFuture(new JobExecutionCollection()
      .withJobExecutions(singletonList(jobExecution))));
    when(jobProfileService.get(anyString(), eq(0), eq(10), eq(TENANT_ID))).thenReturn(Future.failedFuture(StringUtils.EMPTY));
    //when
    Future<JobExecutionCollection> future = jobExecutionService.get(query, 0, 10, TENANT_ID);
    //then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      JobExecution fetchedJobExecution = ar.result().getJobExecutions().get(0);
      assertEquals(JOB_PROFILE_NAME, fetchedJobExecution.getJobProfileName());
      context.completeNow();
    }));
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
      .withId(JOB_EXECUTION_ID)
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
    when(jobExecutionDao.update(jobExecution, TENANT_ID)).thenReturn(Future.succeededFuture(jobExecution));

    //when
    Future<JobExecution> future = jobExecutionService.prepareJobForExport(jobExecution, fileDefinition, user, TOTAL_COUNT, true, TENANT_ID);

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
        assertEquals(TOTAL_COUNT, updatedJobExecution.getProgress()
          .getTotal().intValue());
        assertEquals(JOB_PROFILE_NAME, jobExecution.getJobProfileName());
        context.completeNow();
      });
    });
    verify(jobExecutionService).update(jobExecution, TENANT_ID);
  }

  @Test
  void expireJobExecutionsShouldSetStatusToFail(VertxTestContext context) {
    // given
    JobExecution jobExecution = new JobExecution()
      .withId(JOB_EXECUTION_ID)
      .withExportedFiles(Sets.newHashSet())
      .withJobProfileId(JOB_PROFILE_ID)
      .withStatus(IN_PROGRESS);
    JobExecution secondJobExecution = new JobExecution()
      .withId(UUID.randomUUID().toString())
      .withExportedFiles(Sets.newHashSet())
      .withJobProfileId(JOB_PROFILE_ID)
      .withStatus(NEW);
    when(jobExecutionDao.getExpiredEntries(any(Date.class), eq(TENANT_ID))).thenReturn(Future.succeededFuture(asList(jobExecution, secondJobExecution)));

    // when
    Future<Void> future = jobExecutionService.expireJobExecutions(TENANT_ID);

    future.onComplete(ar ->
      context.verify(() -> {
        assertTrue(ar.succeeded());
        assertNotNull(jobExecution.getCompletedDate());
        assertNotNull(secondJobExecution.getCompletedDate());
        verify(jobExecutionDao).update(eq(jobExecution.withStatus(FAIL)), eq(TENANT_ID));
        verify(jobExecutionDao).update(eq(secondJobExecution.withStatus(FAIL)), eq(TENANT_ID));
        context.completeNow();
      })
    );
  }

  @Test
  void deleteById_shouldReturnFailFuture_whenStatusIsInProgress(VertxTestContext context) {
    //given
    JobExecution jobExecution = new JobExecution()
      .withId(JOB_EXECUTION_ID)
      .withStatus(IN_PROGRESS);
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.of(jobExecution)));
    //when
    Future<Boolean> future = jobExecutionService.deleteById(JOB_EXECUTION_ID, TENANT_ID);
    //then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.failed());
      assertEquals(String.format("Fail to delete jobExecution with id %s, status is IN_PROGRESS", JOB_EXECUTION_ID), ar.cause().getMessage());
      ServiceException serviceException = (ServiceException) ar.cause();
      assertEquals(HttpStatus.HTTP_FORBIDDEN.toInt(), serviceException.getCode());
      context.completeNow();
    }));
  }

  @Test
  void deleteById_shouldReturnFalseFuture_whenJobExecutionNotFound(VertxTestContext context) {
    //given
    when(jobExecutionDao.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.empty()));
    //when
    Future<Boolean> future = jobExecutionService.deleteById(JOB_EXECUTION_ID, TENANT_ID);
    //then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      assertFalse(ar.result());
      context.completeNow();
    }));
  }

  private ExportedFile getFirstExportedFile(JobExecution updatedJobExecution) {
    return updatedJobExecution.getExportedFiles().iterator().next();
  }

}
