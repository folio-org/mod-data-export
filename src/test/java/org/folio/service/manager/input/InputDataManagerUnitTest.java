package org.folio.service.manager.input;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.util.Maps;
import org.folio.clients.UsersClient;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.Progress;
import org.folio.service.file.definition.FileDefinitionService;
import org.folio.service.file.reader.SourceReader;
import org.folio.service.job.JobExecutionServiceImpl;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.manager.export.ExportManager;
import org.folio.service.manager.export.ExportPayload;
import org.folio.service.manager.export.ExportResult;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;

import com.google.common.collect.Lists;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InputDataManagerUnitTest {

  private static final int BATCH_SIZE = 2;
  private static final String FILE_NAME = "InventoryUUIDs.csv";
  private static final String INPUT_DATA_LOCAL_MAP_KEY = "inputDataLocalMap";
  private static final String TENANT_ID = "diku";
  private static final String JOB_EXECUTION_ID = "jobExecutionId";
  private static final String JOB_PROFILE_ID = "jobProfileId";
  private static final String MAPPING_PROFILE_ID = "jobExecutionId";
  private static final String EXPORT_FILE_DEFINITION_NAME = "exportFileDefinition";
  private static final String FILE_DIRECTORY = "src/test/resources/";
  private static final String SPRING_CONTEXT_NAME = "springContext";
  private static final String THREAD_WORKER_NAME = "input-data-manager-thread-worker";
  private static final List<String> EXPECTED_IDS =
    Arrays.asList("c8b50e3f-0446-429c-960e-03774b88223f",
      "aae06d90-a8c2-4514-b227-5756f1f5f5d6",
      "d5c7968c-17e7-4ab1-8aeb-3109e1b77c80",
      "a5e9ccb3-737b-43b0-8f4a-f32a04c9ae16",
      "c5d662af-b0be-4851-bb9c-de70bba3dfce");
  private static final String DELIMETER = "-";
  private static final String FILE_EXPORT_DEFINITION_KEY = "fileExportDefinition";
  private static final String OKAPI_CONNECTION_PARAMS_KEY = "okapiConnectionParams";
  private static final String JOB_EXECUTION_ID_KEY = "jobExecutionId";
  private static final String LAST_KEY = "last";
  private static final String IDENTIFIERS_KEY = "identifiers";
  private static final String TENANT_ID_KEY = "tenantId";
  private static final JsonObject USER = new JsonObject()
    .put("personal", new JsonObject()
      .put("firstname", "John")
      .put("lastname", "Doe")
    );
  private static final int TOTAL_COUNT_2 = 2;
  private static final int TOTAL_COUNT_4 = 4;

  @InjectMocks
  @Spy
  private InputDataManagerImpl inputDataManager;

  @Mock
  private SourceReader sourceReader;
  @Mock
  private FileDefinitionService fileDefinitionService;
  @Mock
  private JobExecutionServiceImpl jobExecutionService;
  @Mock
  private JsonObject exportRequestJson;
  @Mock
  private ExportManager exportManager;
  @Mock
  private InputDataContext inputDataContext;
  @Mock
  private UsersClient usersClient;
  @Mock
  private ErrorLogService errorLogService;

  private Context context;
  private AbstractApplicationContext springContext;
  private AutowireCapableBeanFactory beanFactory;
  private Vertx vertx;
  private WorkerExecutor executor;
  private SharedData sharedData;
  private LocalMap<String, InputDataContext> inputDataLocalMap;

  @Captor
  private ArgumentCaptor<JsonObject> exportPayloadJsonCaptor;
  @Captor
  private ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor;
  @Captor
  private ArgumentCaptor<InputDataContext> inputDataContextCaptor;
  @Captor
  private ArgumentCaptor<JobExecution> jobExecutionCaptor;

  private ExportRequest exportRequest;
  private FileDefinition requestFileDefinition;
  private Map<String, String> requestParams;
  private FileDefinition fileExportDefinition;
  private JobExecution jobExecution;
  private MappingProfile mappingProfile;

  @BeforeEach
  public void setup() {
    initializeInputDataManager();
    requestFileDefinition = createRequestFileDefinition();
    fileExportDefinition = createFileExportDefinition();
    exportRequest = createExportRequest();
    requestParams = Maps.<String, String>newHashMap(OKAPI_HEADER_TENANT, TENANT_ID);
    jobExecution = new JobExecution().withId(JOB_EXECUTION_ID).withStatus(JobExecution.Status.NEW).withHrId(1);
    mappingProfile = new MappingProfile().withId(MAPPING_PROFILE_ID);
    when(exportRequestJson.mapTo(ExportRequest.class)).thenReturn(exportRequest);
    when(jobExecutionService.getById(eq(JOB_EXECUTION_ID), eq(TENANT_ID))).thenReturn(Future.succeededFuture(jobExecution));
    when(jobExecutionService.update(jobExecution, TENANT_ID)).thenReturn(Future.succeededFuture(jobExecution));
    when(usersClient.getById(anyString(), anyString(), any(OkapiConnectionParams.class))).thenReturn(Optional.of(USER));
    doReturn(exportManager).when(inputDataManager).getExportManager();
    doReturn(2).when(inputDataManager).getBatchSize();
    doReturn(sourceReader).when(inputDataManager).initSourceReader(any(FileDefinition.class), anyString(), anyString(), anyInt());

  }

  @Test
  @Order(1)
  void shouldNotInitExportSuccessfully_andSetStatusError_whenSourceStreamReaderEmpty() {
    //given
    when(sourceReader.hasNext()).thenReturn(false);
    when(fileDefinitionService.save(any(FileDefinition.class), eq(TENANT_ID))).thenReturn(Future.succeededFuture(new FileDefinition()));
    doCallRealMethod().when(jobExecutionService).prepareAndSaveJobForFailedExport(any(), any(FileDefinition.class), eq(USER), eq(0), eq(true), eq(TENANT_ID));
    //when
    inputDataManager.initBlocking(exportRequestJson, JsonObject.mapFrom(requestFileDefinition), JsonObject.mapFrom(mappingProfile), JsonObject.mapFrom(jobExecution), requestParams);

    //then
    verify(sourceReader).close();
    verify(fileDefinitionService).save(fileExportDefinitionCaptor.capture(), eq(TENANT_ID));
    verify(jobExecutionService).prepareAndSaveJobForFailedExport(jobExecutionCaptor.capture(), any(FileDefinition.class), eq(USER), eq(0), eq(true), eq(TENANT_ID));
    assertNotNull(jobExecutionCaptor.getValue().getCompletedDate());
    FileDefinition fileDefinition = fileExportDefinitionCaptor.getValue();
    assertThat(fileDefinition.getStatus(), equalTo(FileDefinition.Status.ERROR));
    assertThat(fileDefinition.getFileName(), equalTo("InventoryUUIDs" + DELIMETER + jobExecution.getHrId() + ".mrc"));
    verify(errorLogService).saveGeneralError(ErrorCode.ERROR_READING_FROM_INPUT_FILE.getCode(), JOB_EXECUTION_ID, TENANT_ID);
  }

  @Test
  @Order(2)
  void shouldInitInputDataContextBeforeExportData_whenSourceStreamNotEmpty() {
    //given
    when(sourceReader.hasNext()).thenReturn(true);
    when(sourceReader.totalCount()).thenReturn(TOTAL_COUNT_2);
    doCallRealMethod().when(jobExecutionService).prepareJobForExport(eq(JOB_EXECUTION_ID), any(FileDefinition.class), eq(USER), eq(TOTAL_COUNT_2), eq(true), eq(TENANT_ID));
    when(fileDefinitionService.save(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture(fileExportDefinition));
    //when
    inputDataManager.initBlocking(exportRequestJson, JsonObject.mapFrom(requestFileDefinition), JsonObject.mapFrom(mappingProfile), JsonObject.mapFrom(jobExecution), requestParams);

    //then
    verify(jobExecutionService).update(jobExecution, TENANT_ID);
    assertJobExecutionDataWereUpdated();
    verify(inputDataLocalMap).put(eq(JOB_EXECUTION_ID), inputDataContextCaptor.capture());
    InputDataContext inputDataContext = inputDataContextCaptor.getValue();
    assertThat(inputDataContext.getSourceReader(), equalTo(sourceReader));
  }

  @Test
  @Order(3)
  void shouldCreate_andSaveFileExportDefinitionBeforeExport_whenSourceStreamNotEmpty() {
    //given
    when(sourceReader.hasNext()).thenReturn(true, false);
    when(sourceReader.totalCount()).thenReturn(TOTAL_COUNT_2);
    doCallRealMethod().when(jobExecutionService).prepareJobForExport(eq(JOB_EXECUTION_ID), any(FileDefinition.class), eq(USER), eq(TOTAL_COUNT_2), eq(true), eq(TENANT_ID));
    when(fileDefinitionService.save(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture(fileExportDefinition));

    //when
    inputDataManager.initBlocking(exportRequestJson, JsonObject.mapFrom(requestFileDefinition), JsonObject.mapFrom(mappingProfile), JsonObject.mapFrom(jobExecution), requestParams);

    //then
    verify(jobExecutionService).update(jobExecution, TENANT_ID);
    assertJobExecutionDataWereUpdated();
    FileDefinition actualFileExportDefinition = fileExportDefinitionCaptor.getValue();
    assertThat(actualFileExportDefinition.getStatus(), equalTo(FileDefinition.Status.IN_PROGRESS));
    assertThat(actualFileExportDefinition.getFileName(), equalTo("InventoryUUIDs" + DELIMETER + jobExecution.getHrId() + ".mrc"));
  }

  @Test
  @Order(4)
  void shouldInit_andExportData_whenSourceStreamHasOneChunk() {
    //given
    when(sourceReader.hasNext()).thenReturn(true, false);
    when(sourceReader.totalCount()).thenReturn(TOTAL_COUNT_2);
    doCallRealMethod().when(jobExecutionService).prepareJobForExport(eq(JOB_EXECUTION_ID), any(FileDefinition.class), eq(USER), eq(TOTAL_COUNT_2), eq(true), eq(TENANT_ID));
    when(fileDefinitionService.save(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture(fileExportDefinition));
    when(inputDataContext.getSourceReader()).thenReturn(sourceReader);
    when(sourceReader.readNext()).thenReturn(EXPECTED_IDS);

    //when
    inputDataManager.initBlocking(exportRequestJson, JsonObject.mapFrom(requestFileDefinition), JsonObject.mapFrom(mappingProfile), JsonObject.mapFrom(jobExecution), requestParams);

    //then
    verify(exportManager).exportData(exportPayloadJsonCaptor.capture());
    verify(jobExecutionService).update(jobExecution, TENANT_ID);
    assertJobExecutionDataWereUpdated();
    JsonObject exportRequest = exportPayloadJsonCaptor.getValue();
    assertThat(exportRequest.getJsonObject(FILE_EXPORT_DEFINITION_KEY), equalTo(JsonObject.mapFrom(fileExportDefinition)));
    assertThat(exportRequest.getJsonObject(OKAPI_CONNECTION_PARAMS_KEY).getString(TENANT_ID_KEY), equalTo(TENANT_ID));
    assertThat(exportRequest.getString(JOB_EXECUTION_ID_KEY), equalTo(JOB_EXECUTION_ID));
    assertThat(exportRequest.getBoolean(LAST_KEY), equalTo(true));
    assertThat(exportRequest.getJsonArray(IDENTIFIERS_KEY), equalTo(new JsonArray(EXPECTED_IDS)));
  }

  @Test
  @Order(5)
  void shouldInit_andExportData_whenSourceStreamHasTwoChunks() {
    //given
    when(sourceReader.hasNext()).thenReturn(true, true);
    when(sourceReader.totalCount()).thenReturn(TOTAL_COUNT_4);
    doCallRealMethod().when(jobExecutionService).prepareJobForExport(eq(JOB_EXECUTION_ID), any(FileDefinition.class), eq(USER), eq(TOTAL_COUNT_4), eq(true), eq(TENANT_ID));
    when(fileDefinitionService.save(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture(fileExportDefinition));
    when(inputDataContext.getSourceReader()).thenReturn(sourceReader);
    when(sourceReader.readNext()).thenReturn(EXPECTED_IDS);

    //when
    inputDataManager.initBlocking(exportRequestJson, JsonObject.mapFrom(requestFileDefinition), JsonObject.mapFrom(mappingProfile), JsonObject.mapFrom(jobExecution), requestParams);

    //then
    verify(exportManager).exportData(exportPayloadJsonCaptor.capture());
    verify(jobExecutionService).update(jobExecution, TENANT_ID);
    assertJobExecutionDataWereUpdated();
    JsonObject exportRequest = exportPayloadJsonCaptor.getValue();
    assertThat(exportRequest.getJsonObject(FILE_EXPORT_DEFINITION_KEY), equalTo(JsonObject.mapFrom(fileExportDefinition)));
    assertThat(exportRequest.getJsonObject(OKAPI_CONNECTION_PARAMS_KEY).getString(TENANT_ID_KEY), equalTo(TENANT_ID));
    assertThat(exportRequest.getString(JOB_EXECUTION_ID_KEY), equalTo(JOB_EXECUTION_ID));
    assertThat(exportRequest.getBoolean(LAST_KEY), equalTo(false));
    assertThat(exportRequest.getJsonArray(IDENTIFIERS_KEY), equalTo(new JsonArray(EXPECTED_IDS)));
  }

  @Test
  @Order(6)
  void shouldFinishExportWithErrors_whenProceedWithExportStatusError() {
    //given
    jobExecution.withProgress(new Progress());
    ExportPayload exportPayload = createExportPayload();
    doCallRealMethod().when(jobExecutionService).updateJobStatusById(eq(JOB_EXECUTION_ID), eq(JobExecution.Status.FAIL), eq(TENANT_ID));
    when(fileDefinitionService.update(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture());
    when(inputDataLocalMap.containsKey(JOB_EXECUTION_ID)).thenReturn(true);
    when(inputDataLocalMap.get(JOB_EXECUTION_ID)).thenReturn(inputDataContext);
    when(inputDataContext.getSourceReader()).thenReturn(sourceReader);

    //when
    inputDataManager.proceedBlocking(JsonObject.mapFrom(exportPayload), ExportResult.failed(ErrorCode.NO_FILE_GENERATED));

    //then
    verify(jobExecutionService).update(jobExecution, TENANT_ID);
    assertJobStatus(JobExecution.Status.FAIL);
    assertNotNull(jobExecution.getCompletedDate());
    FileDefinition.Status actualFileDefinitionStatus = fileExportDefinitionCaptor.getValue().getStatus();
    assertThat(actualFileDefinitionStatus, equalTo(FileDefinition.Status.ERROR));
    verify(sourceReader).close();
    verify(inputDataLocalMap).remove(JOB_EXECUTION_ID);
  }

  @Test
  @Order(7)
  void shouldFinishExportSuccessfully_whenProceedWithExportStatusCompleted() {
    //given
    jobExecution.withProgress(new Progress());
    ExportPayload exportPayload = createExportPayload();
    doCallRealMethod().when(jobExecutionService).updateJobStatusById(eq(JOB_EXECUTION_ID), eq(JobExecution.Status.COMPLETED), eq(TENANT_ID));
    when(fileDefinitionService.update(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture());
    when(inputDataLocalMap.containsKey(JOB_EXECUTION_ID)).thenReturn(true);
    when(inputDataLocalMap.get(JOB_EXECUTION_ID)).thenReturn(inputDataContext);
    when(inputDataContext.getSourceReader()).thenReturn(sourceReader);
    when(errorLogService.isErrorsByErrorCodePresent(anyList(), anyString(), anyString())).thenReturn(Future.succeededFuture(false));

    //when
    inputDataManager.proceedBlocking(JsonObject.mapFrom(exportPayload), ExportResult.completed());

    //then
    verify(jobExecutionService).update(jobExecution, TENANT_ID);
    assertJobStatus(JobExecution.Status.COMPLETED);
    assertNotNull(jobExecution.getCompletedDate());
    FileDefinition.Status actualFileDefinitionStatus = fileExportDefinitionCaptor.getValue().getStatus();
    assertThat(actualFileDefinitionStatus, equalTo(FileDefinition.Status.COMPLETED));
    verify(sourceReader).close();
    verify(inputDataLocalMap).remove(JOB_EXECUTION_ID);
  }

  @Test
  @Order(8)
  void shouldFinishExportWithErrors_whenProceedWithExportStatusCompleted_ButErrorLogsRelatesToTheUUIDsPresent() {
    //given
    jobExecution.withProgress(new Progress());
    ExportPayload exportPayload = createExportPayload();
    doCallRealMethod().when(jobExecutionService).updateJobStatusById(eq(JOB_EXECUTION_ID), eq(JobExecution.Status.COMPLETED_WITH_ERRORS), eq(TENANT_ID));
    when(fileDefinitionService.update(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture());
    when(inputDataLocalMap.containsKey(JOB_EXECUTION_ID)).thenReturn(true);
    when(inputDataLocalMap.get(JOB_EXECUTION_ID)).thenReturn(inputDataContext);
    when(inputDataContext.getSourceReader()).thenReturn(sourceReader);
    when(errorLogService.isErrorsByErrorCodePresent(anyList(), anyString(), anyString())).thenReturn(Future.succeededFuture(true));

    //when
    inputDataManager.proceedBlocking(JsonObject.mapFrom(exportPayload), ExportResult.completed());

    //then
      verify(jobExecutionService).update(jobExecution, TENANT_ID);
      assertJobStatus(JobExecution.Status.COMPLETED_WITH_ERRORS);
      assertNotNull(jobExecution.getCompletedDate());
      FileDefinition.Status actualFileDefinitionStatus = fileExportDefinitionCaptor.getValue().getStatus();
      assertThat(actualFileDefinitionStatus, equalTo(FileDefinition.Status.ERROR));
      verify(sourceReader).close();
      verify(inputDataLocalMap).remove(JOB_EXECUTION_ID);

  }

  @Test
  @Order(9)
  void shouldFinishExportWithErrors_whenProceedWithExportStatusInProgress_andSourceStreamNull() {
    //given
    jobExecution.withProgress(new Progress());
    ExportPayload exportPayload = createExportPayload();
    when(fileDefinitionService.update(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture());
    doCallRealMethod().when(jobExecutionService).updateJobStatusById(eq(JOB_EXECUTION_ID), eq(JobExecution.Status.FAIL), eq(TENANT_ID));
    when(inputDataLocalMap.containsKey(JOB_EXECUTION_ID)).thenReturn(true);
    when(inputDataLocalMap.get(JOB_EXECUTION_ID)).thenReturn(inputDataContext);
    when(inputDataContext.getSourceReader()).thenReturn(null);

    //when
    inputDataManager.proceedBlocking(JsonObject.mapFrom(exportPayload), ExportResult.inProgress());

    //then
    verify(jobExecutionService).update(jobExecution, TENANT_ID);
    assertJobStatus(JobExecution.Status.FAIL);
    assertNotNull(jobExecution.getCompletedDate());
    FileDefinition.Status actualFileDefinitionStatus = fileExportDefinitionCaptor.getValue().getStatus();
    assertThat(actualFileDefinitionStatus, equalTo(FileDefinition.Status.ERROR));
    verify(inputDataLocalMap).remove(JOB_EXECUTION_ID);
  }

  @Test
  @Order(10)
  void shouldExportNextChunk_whenProceedWithExportStatusInProgress_andSourceStreamHasMoreChunksToExport() {
    //given
    ExportPayload exportPayload = createExportPayload();
    when(inputDataLocalMap.get(JOB_EXECUTION_ID)).thenReturn(inputDataContext);
    when(inputDataContext.getSourceReader()).thenReturn(sourceReader);
    when(sourceReader.hasNext()).thenReturn(true, true);
    when(sourceReader.readNext()).thenReturn(EXPECTED_IDS);

    //when
    inputDataManager.proceedBlocking(JsonObject.mapFrom(exportPayload), ExportResult.inProgress());

    //then
    verify(exportManager).exportData(exportPayloadJsonCaptor.capture());
    JsonObject exportRequest = exportPayloadJsonCaptor.getValue();
    assertThat(exportRequest.getJsonObject(FILE_EXPORT_DEFINITION_KEY), equalTo(JsonObject.mapFrom(fileExportDefinition)));
    assertThat(exportRequest.getJsonObject(OKAPI_CONNECTION_PARAMS_KEY).getString(TENANT_ID_KEY), equalTo(TENANT_ID));
    assertThat(exportRequest.getString(JOB_EXECUTION_ID_KEY), equalTo(JOB_EXECUTION_ID));
    assertThat(exportRequest.getBoolean(LAST_KEY), equalTo(false));
    assertThat(exportRequest.getJsonArray(IDENTIFIERS_KEY), equalTo(new JsonArray(EXPECTED_IDS)));
  }

  @Test
  @Order(10)
  void shouldFailToExport_whenPrepareJobForExport_Fail() {
      //given
    jobExecution.withProgress(new Progress());
    when(sourceReader.hasNext()).thenReturn(true, true);
    when(sourceReader.readNext()).thenReturn(EXPECTED_IDS);
    when(fileDefinitionService.save(any(FileDefinition.class), eq(TENANT_ID))).thenReturn(Future.succeededFuture(new FileDefinition()));
    when(fileDefinitionService.update(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture());
    when(inputDataLocalMap.get(null)).thenReturn(inputDataContext);
    when(inputDataContext.getSourceReader()).thenReturn(sourceReader);
    when(jobExecutionService.prepareJobForExport(anyString(), any(FileDefinition.class), any(JsonObject.class), anyInt(), anyBoolean(), anyString())).thenReturn(Future.failedFuture("error"));

    //when
    inputDataManager.initBlocking(exportRequestJson, JsonObject.mapFrom(requestFileDefinition), JsonObject.mapFrom(mappingProfile), JsonObject.mapFrom(jobExecution), requestParams);

    //then
    verify(jobExecutionService).prepareAndSaveJobForFailedExport(any(JobExecution.class), any(FileDefinition.class), any(JsonObject.class), anyInt(), anyBoolean(), anyString());
    verify(exportManager, never()).exportData(any(JsonObject.class));
  }

  @Test
  @Order(11)
  void shouldFailToExport_whenIsNotDefaultHoldingsProfile() {
    //given
    var mappingProfile = new MappingProfile().withId(MAPPING_PROFILE_ID);
    var exportRequest = new ExportRequest()
      .withIdType(ExportRequest.IdType.HOLDING)
      .withFileDefinitionId(UUID.randomUUID().toString())
      .withJobProfileId(JOB_PROFILE_ID)
      .withMetadata(new Metadata().withCreatedByUserId(UUID.randomUUID().toString()));
    when(exportRequestJson.mapTo(ExportRequest.class)).thenReturn(exportRequest);
    when(fileDefinitionService.save(any(FileDefinition.class), eq(TENANT_ID))).thenReturn(Future.succeededFuture(new FileDefinition()));

    //when
    inputDataManager.initBlocking(exportRequestJson, JsonObject.mapFrom(requestFileDefinition), JsonObject.mapFrom(mappingProfile), JsonObject.mapFrom(jobExecution), requestParams);

    //then
    verify(jobExecutionService).prepareAndSaveJobForFailedExport(any(JobExecution.class), any(FileDefinition.class), any(JsonObject.class), anyInt(), anyBoolean(), anyString());
    verify(exportManager, never()).exportData(any(JsonObject.class));
  }

  @Test
  @Order(12)
  void shouldFailToExport_whenIsDefaultHoldingsProfile_andCqlFormat() {
    //given
    var mappingProfile = new MappingProfile().withId(MAPPING_PROFILE_ID);
    var exportRequest = new ExportRequest()
      .withIdType(ExportRequest.IdType.HOLDING)
      .withFileDefinitionId(UUID.randomUUID().toString())
      .withJobProfileId("1ef7d0ac-f0a8-42b5-bbbb-c7e249009c13")
      .withMetadata(new Metadata().withCreatedByUserId(UUID.randomUUID().toString()));
    var requestFileDefinition = new FileDefinition()
      .withUploadFormat(FileDefinition.UploadFormat.CQL);
    when(exportRequestJson.mapTo(ExportRequest.class)).thenReturn(exportRequest);
    when(fileDefinitionService.save(any(FileDefinition.class), eq(TENANT_ID))).thenReturn(Future.succeededFuture(new FileDefinition()));

    //when
    inputDataManager.initBlocking(exportRequestJson, JsonObject.mapFrom(requestFileDefinition), JsonObject.mapFrom(mappingProfile), JsonObject.mapFrom(jobExecution), requestParams);

    //then
    verify(jobExecutionService).prepareAndSaveJobForFailedExport(any(JobExecution.class), any(FileDefinition.class), any(JsonObject.class), anyInt(), anyBoolean(), anyString());
    verify(exportManager, never()).exportData(any(JsonObject.class));
  }

  @Test
  @Order(13)
  void shouldFailToExport_whenIsNotDefaultAuthorityProfile() {
    //given
    var mappingProfile = new MappingProfile().withId(MAPPING_PROFILE_ID);
    var exportRequest = new ExportRequest()
      .withIdType(ExportRequest.IdType.AUTHORITY)
      .withFileDefinitionId(UUID.randomUUID().toString())
      .withJobProfileId(JOB_PROFILE_ID)
      .withMetadata(new Metadata().withCreatedByUserId(UUID.randomUUID().toString()));
    when(exportRequestJson.mapTo(ExportRequest.class)).thenReturn(exportRequest);
    when(fileDefinitionService.save(any(FileDefinition.class), eq(TENANT_ID))).thenReturn(Future.succeededFuture(new FileDefinition()));

    //when
    inputDataManager.initBlocking(exportRequestJson, JsonObject.mapFrom(requestFileDefinition), JsonObject.mapFrom(mappingProfile), JsonObject.mapFrom(jobExecution), requestParams);

    //then
    verify(jobExecutionService).prepareAndSaveJobForFailedExport(any(JobExecution.class), any(FileDefinition.class), any(JsonObject.class), anyInt(), anyBoolean(), anyString());
    verify(exportManager, never()).exportData(any(JsonObject.class));
  }

  @Test
  @Order(14)
  void shouldFailToExport_whenIsDefaultHAuthorityProfile_andCqlFormat() {
    //given
    var mappingProfile = new MappingProfile().withId(MAPPING_PROFILE_ID);
    var exportRequest = new ExportRequest()
      .withIdType(ExportRequest.IdType.HOLDING)
      .withFileDefinitionId(UUID.randomUUID().toString())
      .withJobProfileId("5d636597-a59d-4391-a270-4e79d5ba70e3")
      .withMetadata(new Metadata().withCreatedByUserId(UUID.randomUUID().toString()));
    var requestFileDefinition = new FileDefinition()
      .withUploadFormat(FileDefinition.UploadFormat.CQL);
    when(exportRequestJson.mapTo(ExportRequest.class)).thenReturn(exportRequest);
    when(fileDefinitionService.save(any(FileDefinition.class), eq(TENANT_ID))).thenReturn(Future.succeededFuture(new FileDefinition()));

    //when
    inputDataManager.initBlocking(exportRequestJson, JsonObject.mapFrom(requestFileDefinition), JsonObject.mapFrom(mappingProfile), JsonObject.mapFrom(jobExecution), requestParams);

    //then
    verify(jobExecutionService).prepareAndSaveJobForFailedExport(any(JobExecution.class), any(FileDefinition.class), any(JsonObject.class), anyInt(), anyBoolean(), anyString());
    verify(exportManager, never()).exportData(any(JsonObject.class));
  }

  private void initializeInputDataManager() {
    context = Mockito.mock(Context.class);
    springContext = Mockito.mock(AbstractApplicationContext.class);
    beanFactory = Mockito.mock(AutowireCapableBeanFactory.class);
    vertx = Mockito.mock(Vertx.class);
    executor = Mockito.mock(WorkerExecutor.class);
    sharedData = Mockito.mock(SharedData.class);
    inputDataLocalMap = Mockito.mock(LocalMap.class);

    when(context.get(SPRING_CONTEXT_NAME)).thenReturn(springContext);
    when(springContext.getAutowireCapableBeanFactory()).thenReturn(beanFactory);
    doNothing().when(beanFactory).autowireBean(any(InputDataManagerImpl.class));

    when(context.get(ExportManager.class.getName())).thenReturn(exportManager);

    when(context.owner()).thenReturn(vertx);
    when(vertx.createSharedWorkerExecutor(THREAD_WORKER_NAME, 1)).thenReturn(executor);
    when(vertx.sharedData()).thenReturn(sharedData);
    when(sharedData.<String, InputDataContext>getLocalMap(INPUT_DATA_LOCAL_MAP_KEY)).thenReturn(inputDataLocalMap);

    inputDataManager = new InputDataManagerImpl(context);
    MockitoAnnotations.initMocks(this);
  }

  private ExportRequest createExportRequest() {
    return new ExportRequest()
      .withFileDefinitionId(UUID.randomUUID().toString())
      .withJobProfileId(JOB_PROFILE_ID)
      .withMetadata(new Metadata().withCreatedByUserId(UUID.randomUUID().toString()));
  }

  private FileDefinition createFileExportDefinition() {
    return new FileDefinition()
      .withFileName(EXPORT_FILE_DEFINITION_NAME)
      .withJobExecutionId(JOB_EXECUTION_ID);
  }

  private FileDefinition createRequestFileDefinition() {
    return new FileDefinition()
      .withFileName(FILE_NAME)
      .withSourcePath(FILE_DIRECTORY + FILE_NAME)
      .withJobExecutionId(JOB_EXECUTION_ID)
      .withStatus(FileDefinition.Status.COMPLETED);
  }

  private ExportPayload createExportPayload() {
    ExportPayload exportPayload = new ExportPayload();
    exportPayload.setOkapiConnectionParams(new OkapiConnectionParams(requestParams));
    exportPayload.setFileExportDefinition(fileExportDefinition);
    exportPayload.setJobExecutionId(JOB_EXECUTION_ID);
    return exportPayload;
  }

  private Iterator<List<String>> mockIterator() {
    return Lists.partition(EXPECTED_IDS, BATCH_SIZE).iterator();
  }

  private void assertJobExecutionDataWereUpdated() {
    String fileName = fileExportDefinitionCaptor.getValue().getFileName();
    assertTrue(jobExecution.getExportedFiles().stream()
      .anyMatch(exportedFile -> exportedFile.getFileName().equals(fileName)));
    assertEquals(JobExecution.Status.IN_PROGRESS, jobExecution.getStatus());
    assertNotNull(jobExecution.getStartedDate());
  }

  private void assertJobStatus(JobExecution.Status status) {
    assertEquals(jobExecution.getStatus(), status);
  }
}
