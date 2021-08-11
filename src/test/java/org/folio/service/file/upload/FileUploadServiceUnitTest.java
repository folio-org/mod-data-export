package org.folio.service.file.upload;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.clients.InventoryClient;
import org.folio.clients.UsersClient;
import org.folio.rest.impl.RestVerticleTestBase;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.QuickExportRequest;
import org.folio.service.file.definition.FileDefinitionService;
import org.folio.service.file.storage.FileStorage;
import org.folio.service.job.JobExecutionService;
import org.folio.service.logs.ErrorLogService;
import org.folio.util.OkapiConnectionParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.util.Optional.of;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.impl.MockServer.BASE_MOCK_DATA_PATH;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.COMPLETED;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.ERROR;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.IN_PROGRESS;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.NEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class FileUploadServiceUnitTest {
  private static final String USERS_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "user/get_user_response.json";
  private static final String INSTANCE_BULK_IDS_ALL_VALID_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_valid_instance_bulk_ids_response.json";
  private static final String JOB_EXECUTION_ID = "jobExecutionId";
  private static final String TENANT_ID = "diku";
  @Spy
  @InjectMocks
  FileUploadServiceImpl fileUploadService;
  @Mock
  FileStorage fileStorage;
  @Mock
  JobExecutionService jobExecutionService;
  @Mock
  FileDefinitionService fileDefinitionService;
  @Mock
  ErrorLogService errorLogService;
  @Mock
  UsersClient usersClient;
  @Mock
  InventoryClient inventoryClient;
  private OkapiConnectionParams params;
  private FileDefinition fileDefinition;
  private JsonObject user;

  @BeforeEach
  void before() throws IOException {
    fileDefinition = new FileDefinition()
      .withJobExecutionId(JOB_EXECUTION_ID)
      .withId(UUID.randomUUID().toString())
      .withStatus(NEW);
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    params = new OkapiConnectionParams(headers);
    user = new JsonObject(RestVerticleTestBase.getMockData(USERS_RECORDS_MOCK_DATA_PATH));
  }

  @Test
  void shouldFail_whenGetJobExecutionFail_uploadFileDependsOnTypeFor_emptyResponseInventory_cqlQuickExport(VertxTestContext context) {
    // given
    QuickExportRequest quickExportRequest = buildQuickCqlExportRequest("test");
    when(jobExecutionService.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(Future.failedFuture(new RuntimeException()));
    when(fileDefinitionService.update(any(FileDefinition.class), anyString())).thenReturn(succeededFuture(fileDefinition));

    // when
    Future<FileDefinition> fileDefinitionFuture = fileUploadService.uploadFileDependsOnTypeForQuickExport(quickExportRequest, fileDefinition, params);

    // then
    fileDefinitionFuture.onComplete(ar ->
      context.verify(() -> {
        assertTrue(ar.failed());
        assertEquals(ERROR, fileDefinition.getStatus());
        context.completeNow();
      }));
  }

  @Test
  void shouldFail_uploadFileDependsOnType_whenGetStartUploadingFail_emptyResponseInventory_cqlQuickExport(VertxTestContext context) {
    // given
    QuickExportRequest quickExportRequest = buildQuickCqlExportRequest("test");
    when(jobExecutionService.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(succeededFuture(new JobExecution().withId(JOB_EXECUTION_ID)));
    when(fileDefinitionService.update(any(FileDefinition.class), anyString())).thenReturn(failedFuture(new RuntimeException()))
      .thenReturn(failedFuture(new RuntimeException()));

    // when
    Future<FileDefinition> fileDefinitionFuture = fileUploadService.uploadFileDependsOnTypeForQuickExport(quickExportRequest, fileDefinition, params);

    // then
    fileDefinitionFuture.onComplete(ar ->
      context.verify(() -> {
        assertTrue(ar.failed());
        verify(fileDefinitionService, times(2)).update(eq(fileDefinition), eq(TENANT_ID));
        context.completeNow();
      }));
  }


  @Test
  void shouldSucceed_uploadFileDependsOnType_whenEmptyResponseInventory_cqlQuickExport(VertxTestContext context) {
    // given
    FileDefinition inProgressDef = new FileDefinition()
      .withId(fileDefinition.getId())
      .withStatus(IN_PROGRESS)
      .withJobExecutionId(JOB_EXECUTION_ID);
    FileDefinition completedDef = new FileDefinition()
      .withId(fileDefinition.getId())
      .withStatus(COMPLETED)
      .withJobExecutionId(JOB_EXECUTION_ID);
    QuickExportRequest quickExportRequest = buildQuickCqlExportRequest("test");
    when(jobExecutionService.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(succeededFuture());
    when(fileDefinitionService.update(any(FileDefinition.class), anyString())).thenReturn(succeededFuture(inProgressDef))
      .thenReturn(succeededFuture(completedDef));
    when(inventoryClient.getInstancesBulkUUIDsAsync(eq("test"), any(OkapiConnectionParams.class))).thenReturn(Future.succeededFuture(Optional.empty()));

    // when
    Future<FileDefinition> fileDefinitionFuture = fileUploadService.uploadFileDependsOnTypeForQuickExport(quickExportRequest, fileDefinition, params);

    // then
    fileDefinitionFuture.onComplete(ar ->
      context.verify(() -> {
        assertTrue(ar.succeeded());
        FileDefinition fileDefinitionResult = ar.result();
        verify(fileDefinitionService, times(2)).update(any(FileDefinition.class), eq(TENANT_ID));
        verify(fileDefinitionService).update(fileDefinition.withStatus(IN_PROGRESS), TENANT_ID);
        assertEquals(COMPLETED, fileDefinitionResult.getStatus());
        context.completeNow();
      }));
  }

  @Test
  void shouldSucceed_uploadFileDependsOnType_cqlQuickExport(VertxTestContext context) throws IOException {
    // given
    FileDefinition inProgressDef = new FileDefinition()
      .withId(fileDefinition.getId())
      .withStatus(IN_PROGRESS)
      .withJobExecutionId(JOB_EXECUTION_ID);
    FileDefinition completedDef = new FileDefinition()
      .withId(fileDefinition.getId())
      .withStatus(COMPLETED)
      .withJobExecutionId(JOB_EXECUTION_ID);
    JobExecution jobExecution = new JobExecution().withId(JOB_EXECUTION_ID);
    QuickExportRequest quickExportRequest = buildQuickCqlExportRequest("test");
    when(jobExecutionService.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(succeededFuture(jobExecution));
    when(jobExecutionService.update(any(JobExecution.class), eq(TENANT_ID))).thenReturn(succeededFuture(jobExecution));
    when(fileDefinitionService.update(any(FileDefinition.class), anyString())).thenReturn(succeededFuture(inProgressDef))
      .thenReturn(succeededFuture(completedDef));
    when(inventoryClient.getInstancesBulkUUIDsAsync(anyString(), eq(params))).thenReturn(Future.succeededFuture(of(new JsonObject(RestVerticleTestBase.getMockData(INSTANCE_BULK_IDS_ALL_VALID_MOCK_DATA_PATH)))));
    when(fileStorage.saveFileDataAsyncCQL(anyList(), any(FileDefinition.class))).thenReturn(succeededFuture(completedDef));

    // when
    Future<FileDefinition> fileDefinitionFuture = fileUploadService.uploadFileDependsOnTypeForQuickExport(quickExportRequest, fileDefinition, params);

    // then
    fileDefinitionFuture.onComplete(ar ->
      context.verify(() -> {
        assertTrue(ar.succeeded());
        FileDefinition fileDefinitionResult = ar.result();
        verify(fileDefinitionService).update(fileDefinition.withStatus(IN_PROGRESS), TENANT_ID);
        verify(fileDefinitionService).update(fileDefinition.withStatus(COMPLETED), TENANT_ID);
        assertEquals(COMPLETED, fileDefinitionResult.getStatus());
        context.completeNow();
      }));
  }

  @Test
  void shouldFail_uploadFileDependsOnType_whenGetStartUploadingFail_uuidQuickExport(VertxTestContext context) {
    // given
    FileDefinition inProgressDef = new FileDefinition()
      .withId(fileDefinition.getId())
      .withStatus(IN_PROGRESS)
      .withJobExecutionId(JOB_EXECUTION_ID);
    QuickExportRequest quickExportRequest = buildQuickExportRequest(Collections.singletonList("uuid"));
    when(jobExecutionService.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(succeededFuture(new JobExecution().withId(JOB_EXECUTION_ID)));
    when(fileDefinitionService.update(any(FileDefinition.class), anyString())).thenReturn(succeededFuture(inProgressDef))
      .thenReturn(failedFuture(new RuntimeException()));
    when(fileStorage.saveFileDataAsyncCQL(anyList(), any(FileDefinition.class))).thenReturn(failedFuture(new RuntimeException()));
    when(usersClient.getById(anyString(), anyString(), any(OkapiConnectionParams.class))).thenReturn(Optional.of(user));
    // when
    Future<FileDefinition> fileDefinitionFuture = fileUploadService.uploadFileDependsOnTypeForQuickExport(quickExportRequest, fileDefinition, params);

    // then
    fileDefinitionFuture.onComplete(ar ->
      context.verify(() -> {
        assertTrue(ar.failed());
        verify(fileDefinitionService, times(2)).update(any(FileDefinition.class), eq(TENANT_ID));
        verify(usersClient).getById(anyString(), anyString(), eq(params));
        verify(jobExecutionService).prepareAndSaveJobForFailedExport(any(JobExecution.class), any(FileDefinition.class), eq(user), anyInt(), anyBoolean(), anyString());
        context.completeNow();
      }));
  }

  @Test
  void shouldFail_uploadFileDependsOnType_whenUpdateJobExecutionFail_uuidQuickExport(VertxTestContext context) {
    // given
    FileDefinition inProgressDef = new FileDefinition()
      .withId(fileDefinition.getId())
      .withStatus(IN_PROGRESS)
      .withJobExecutionId(JOB_EXECUTION_ID);
    QuickExportRequest quickExportRequest = buildQuickExportRequest(Collections.singletonList("uuid"));
    when(jobExecutionService.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(succeededFuture(new JobExecution().withId(JOB_EXECUTION_ID)));
    when(jobExecutionService.update(any(JobExecution.class), eq(TENANT_ID))).thenReturn(failedFuture(new RuntimeException()));
    when(fileDefinitionService.update(any(FileDefinition.class), anyString())).thenReturn(succeededFuture(inProgressDef))
      .thenReturn(failedFuture(new RuntimeException()));
    when(fileStorage.saveFileDataAsyncCQL(anyList(), any(FileDefinition.class))).thenReturn(succeededFuture(fileDefinition));

    // when
    Future<FileDefinition> fileDefinitionFuture = fileUploadService.uploadFileDependsOnTypeForQuickExport(quickExportRequest, fileDefinition, params);

    // then
    fileDefinitionFuture.onComplete(ar ->
      context.verify(() -> {
        assertTrue(ar.failed());
        verify(fileDefinitionService, times(2)).update(eq(fileDefinition), eq(TENANT_ID));
        context.completeNow();
      }));
  }

  @Test
  void shouldSucceed_uploadFileDependsOnType_uuidQuickExport(VertxTestContext context) {
    // given
    FileDefinition inProgressDef = new FileDefinition()
      .withId(fileDefinition.getId())
      .withStatus(IN_PROGRESS)
      .withJobExecutionId(JOB_EXECUTION_ID);
    FileDefinition completedDef = new FileDefinition()
      .withId(fileDefinition.getId())
      .withStatus(COMPLETED)
      .withJobExecutionId(JOB_EXECUTION_ID);
    QuickExportRequest quickExportRequest = buildQuickExportRequest(Collections.singletonList("uuid"));
    JobExecution jobExecution = new JobExecution().withId(JOB_EXECUTION_ID);
    when(jobExecutionService.getById(JOB_EXECUTION_ID, TENANT_ID)).thenReturn(succeededFuture(jobExecution));
    when(jobExecutionService.update(any(JobExecution.class), eq(TENANT_ID))).thenReturn(succeededFuture(jobExecution));
    when(fileDefinitionService.update(any(FileDefinition.class), anyString())).thenReturn(succeededFuture(inProgressDef))
      .thenReturn(succeededFuture(completedDef));
    when(fileStorage.saveFileDataAsyncCQL(anyList(), any(FileDefinition.class))).thenReturn(succeededFuture(completedDef));

    // when
    Future<FileDefinition> fileDefinitionFuture = fileUploadService.uploadFileDependsOnTypeForQuickExport(quickExportRequest, fileDefinition, params);

    // then
    fileDefinitionFuture.onComplete(ar ->
      context.verify(() -> {
        assertTrue(ar.succeeded());
        FileDefinition fileDefinitionResult = ar.result();
        verify(fileDefinitionService).update(fileDefinition.withStatus(IN_PROGRESS), TENANT_ID);
        verify(fileDefinitionService).update(fileDefinition.withStatus(COMPLETED), TENANT_ID);
        assertEquals(COMPLETED, fileDefinitionResult.getStatus());
        context.completeNow();
      }));
  }

  private QuickExportRequest buildQuickCqlExportRequest(String criteria) {
    return new QuickExportRequest()
      .withCriteria(criteria)
      .withRecordType(QuickExportRequest.RecordType.INSTANCE)
      .withType(QuickExportRequest.Type.CQL)
      .withMetadata(new Metadata().withCreatedByUserId(UUID.randomUUID().toString()));
  }

  private QuickExportRequest buildQuickExportRequest(List<String> uuids) {
    return new QuickExportRequest()
      .withType(QuickExportRequest.Type.UUID)
      .withUuids(uuids)
      .withRecordType(QuickExportRequest.RecordType.INSTANCE)
      .withMetadata(new Metadata().withCreatedByUserId(UUID.randomUUID().toString()));
  }

}
