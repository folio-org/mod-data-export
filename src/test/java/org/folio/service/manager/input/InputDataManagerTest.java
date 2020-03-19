package org.folio.service.manager.input;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;

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
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.service.job.JobExecutionService;
import org.folio.service.manager.export.ExportManager;
import org.folio.service.manager.export.ExportPayload;
import org.folio.service.manager.export.ExportResult;
import org.folio.service.file.reader.SourceReader;
import org.folio.service.file.definition.FileDefinitionService;
import org.folio.util.OkapiConnectionParams;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;

import com.google.common.collect.Lists;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InputDataManagerTest {

  private static final int BATCH_SIZE = 2;
  private static final String FILE_NAME = "InventoryUUIDs.csv";
  private static final String INPUT_DATA_LOCAL_MAP_KEY = "inputDataLocalMap";
  private static final String TENANT_ID = "diku";
  private static final String JOB_EXECUTION_ID = "jobExecutionId";
  private static final String EXPORT_FILE_DEFINITION_NAME = "exportFileDefinition";
  private static final String TIMESTAMP = "timestamp";
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

  @InjectMocks
  @Spy
  private InputDataManagerImpl inputDataManager;

  @Mock
  private SourceReader sourceReader;
  @Mock
  private FileDefinitionService fileDefinitionService;
  @Mock
  private JobExecutionService jobExecutionService;
  @Mock
  private JsonObject exportRequestJson;
  @Mock
  private ExportManager exportManager;
  @Mock
  private InputDataContext inputDataContext;
  @Mock
  private UsersClient usersClient;

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


  @Before
  public void setUp() {
    initializeInputDataManager();
    requestFileDefinition = createRequestFileDefinition();
    fileExportDefinition = createFileExportDefinition();
    exportRequest = createExportRequest();
    requestParams = Maps.<String, String>newHashMap(OKAPI_HEADER_TENANT, TENANT_ID);
    jobExecution = new JobExecution().withId(JOB_EXECUTION_ID).withStatus(JobExecution.Status.NEW);
    when(exportRequestJson.mapTo(ExportRequest.class)).thenReturn(exportRequest);
    when(jobExecutionService.getById(eq(JOB_EXECUTION_ID), eq(TENANT_ID))).thenReturn(Future.succeededFuture(Optional.of(jobExecution)));
    when(jobExecutionService.update(jobExecution, TENANT_ID)).thenReturn(Future.succeededFuture(jobExecution));
    when(usersClient.getById(ArgumentMatchers.anyString(), ArgumentMatchers.any(OkapiConnectionParams.class))).thenReturn(Optional.of(USER));
    doReturn(exportManager).when(inputDataManager).getExportManager();
    doReturn(2).when(inputDataManager).getBatchSize();
    doReturn(sourceReader).when(inputDataManager).initSourceReader(requestFileDefinition, BATCH_SIZE);

  }

  @Test
  public void shouldNotInitExportSuccessfully_andSetStatusError_whenSourceStreamReaderEmpty() {
    //given
    doReturn(TIMESTAMP).when(inputDataManager).getCurrentTimestamp();
    when(sourceReader.hasNext()).thenReturn(false);

    //when
    inputDataManager.initBlocking(exportRequestJson, requestParams);

    //then
    verify(sourceReader).close();
    verify(fileDefinitionService).save(fileExportDefinitionCaptor.capture(), eq(TENANT_ID));
    verify(jobExecutionService).update(jobExecutionCaptor.capture(), eq(TENANT_ID));
    assertNotNull(jobExecutionCaptor.getValue().getCompletedDate());
    FileDefinition fileDefinition = fileExportDefinitionCaptor.getValue();
    assertThat(fileDefinition.getStatus(), equalTo(FileDefinition.Status.ERROR));
    assertThat(fileDefinition.getFileName(), equalTo("InventoryUUIDs" + DELIMETER + TIMESTAMP + ".mrc"));
  }

  @Test
  public void shouldInitInputDataContextBeforeExportData_whenSourceStreamNotEmpty() {
    //given
    doReturn(TIMESTAMP).when(inputDataManager).getCurrentTimestamp();
    when(sourceReader.hasNext()).thenReturn(true);
    when(fileDefinitionService.save(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture(fileExportDefinition));

    //when
    inputDataManager.initBlocking(exportRequestJson, requestParams);

    //then
    verify(jobExecutionService).update(jobExecution, TENANT_ID);
    assertJobExecutionDataWereUpdated();
    verify(inputDataLocalMap).put(eq(JOB_EXECUTION_ID), inputDataContextCaptor.capture());
    InputDataContext inputDataContext = inputDataContextCaptor.getValue();
    assertThat(inputDataContext.getSourceReader(), equalTo(sourceReader));
  }

  @Test
  public void shouldCreate_andSaveFileExportDefinitionBeforeExport_whenSourceStreamNotEmpty() {
    //given
    doReturn(TIMESTAMP).when(inputDataManager).getCurrentTimestamp();
    when(sourceReader.hasNext()).thenReturn(true, false);
    when(fileDefinitionService.save(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture(fileExportDefinition));

    //when
    inputDataManager.initBlocking(exportRequestJson, requestParams);

    //then
    verify(jobExecutionService).update(jobExecution, TENANT_ID);
    assertJobExecutionDataWereUpdated();
    FileDefinition actualFileExportDefinition = fileExportDefinitionCaptor.getValue();
    assertThat(actualFileExportDefinition.getStatus(), equalTo(FileDefinition.Status.IN_PROGRESS));
    assertThat(actualFileExportDefinition.getFileName(), equalTo("InventoryUUIDs" + DELIMETER + TIMESTAMP + ".mrc"));
  }

  @Test
  public void shouldInit_andExportData_whenSourceStreamHasOneChunk() {
    //given
    doReturn(TIMESTAMP).when(inputDataManager).getCurrentTimestamp();
    when(sourceReader.hasNext()).thenReturn(true, false);
    when(fileDefinitionService.save(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture(fileExportDefinition));
    when(inputDataContext.getSourceReader()).thenReturn(sourceReader);
    when(sourceReader.readNext()).thenReturn(EXPECTED_IDS);

    //when
    inputDataManager.initBlocking(exportRequestJson, requestParams);

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
  public void shouldInit_andExportData_whenSourceStreamHasTwoChunks() {
    //given
    doReturn(TIMESTAMP).when(inputDataManager).getCurrentTimestamp();
    when(sourceReader.hasNext()).thenReturn(true, true);
    when(fileDefinitionService.save(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture(fileExportDefinition));
    when(inputDataContext.getSourceReader()).thenReturn(sourceReader);
    when(sourceReader.readNext()).thenReturn(EXPECTED_IDS);

    //when
    inputDataManager.initBlocking(exportRequestJson, requestParams);

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
  public void shouldFinishExportWithErrors_whenProceedWithExportStatusError() {
    //given
    ExportPayload exportPayload = createExportPayload();
    when(fileDefinitionService.update(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture());
    when(inputDataLocalMap.containsKey(JOB_EXECUTION_ID)).thenReturn(true);
    when(inputDataLocalMap.get(JOB_EXECUTION_ID)).thenReturn(inputDataContext);
    when(inputDataContext.getSourceReader()).thenReturn(sourceReader);

    //when
    inputDataManager.proceedBlocking(JsonObject.mapFrom(exportPayload), ExportResult.ERROR);

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
  public void shouldFinishExportSuccessfully_whenProceedWithExportStatusCompleted() {
    //given
    ExportPayload exportPayload = createExportPayload();
    when(fileDefinitionService.update(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture());
    when(inputDataLocalMap.containsKey(JOB_EXECUTION_ID)).thenReturn(true);
    when(inputDataLocalMap.get(JOB_EXECUTION_ID)).thenReturn(inputDataContext);
    when(inputDataContext.getSourceReader()).thenReturn(sourceReader);

    //when
    inputDataManager.proceedBlocking(JsonObject.mapFrom(exportPayload), ExportResult.COMPLETED);

    //then
    verify(jobExecutionService).update(jobExecution, TENANT_ID);
    assertJobStatus(JobExecution.Status.SUCCESS);
    assertNotNull(jobExecution.getCompletedDate());
    FileDefinition.Status actualFileDefinitionStatus = fileExportDefinitionCaptor.getValue().getStatus();
    assertThat(actualFileDefinitionStatus, equalTo(FileDefinition.Status.COMPLETED));
    verify(sourceReader).close();
    verify(inputDataLocalMap).remove(JOB_EXECUTION_ID);
  }

  @Test
  public void shouldFinishExportWithErrors_whenProceedWithExportStatusInProgress_andSourceStreamNull() {
    //given
    ExportPayload exportPayload = createExportPayload();
    when(fileDefinitionService.update(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture());
    when(inputDataLocalMap.containsKey(JOB_EXECUTION_ID)).thenReturn(true);
    when(inputDataLocalMap.get(JOB_EXECUTION_ID)).thenReturn(inputDataContext);
    when(inputDataContext.getSourceReader()).thenReturn(null);

    //when
    inputDataManager.proceedBlocking(JsonObject.mapFrom(exportPayload), ExportResult.IN_PROGRESS);

    //then
    verify(jobExecutionService).update(jobExecution, TENANT_ID);
    assertJobStatus(JobExecution.Status.FAIL);
    assertNotNull(jobExecution.getCompletedDate());
    FileDefinition.Status actualFileDefinitionStatus = fileExportDefinitionCaptor.getValue().getStatus();
    assertThat(actualFileDefinitionStatus, equalTo(FileDefinition.Status.ERROR));
    verify(inputDataLocalMap).remove(JOB_EXECUTION_ID);
  }

  @Test
  public void shouldExportNextChunk_whenProceedWithExportStatusInProgress_andSourceStreamHasMoreChunksToExport() {
    //given
    ExportPayload exportPayload = createExportPayload();
    when(inputDataLocalMap.get(JOB_EXECUTION_ID)).thenReturn(inputDataContext);
    when(inputDataContext.getSourceReader()).thenReturn(sourceReader);
    when(sourceReader.hasNext()).thenReturn(true, true);
    when(sourceReader.readNext()).thenReturn(EXPECTED_IDS);

    //when
    inputDataManager.proceedBlocking(JsonObject.mapFrom(exportPayload), ExportResult.IN_PROGRESS);

    //then
    verify(exportManager).exportData(exportPayloadJsonCaptor.capture());
    JsonObject exportRequest = exportPayloadJsonCaptor.getValue();
    assertThat(exportRequest.getJsonObject(FILE_EXPORT_DEFINITION_KEY), equalTo(JsonObject.mapFrom(fileExportDefinition)));
    assertThat(exportRequest.getJsonObject(OKAPI_CONNECTION_PARAMS_KEY).getString(TENANT_ID_KEY), equalTo(TENANT_ID));
    assertThat(exportRequest.getString(JOB_EXECUTION_ID_KEY), equalTo(JOB_EXECUTION_ID));
    assertThat(exportRequest.getBoolean(LAST_KEY), equalTo(false));
    assertThat(exportRequest.getJsonArray(IDENTIFIERS_KEY), equalTo(new JsonArray(EXPECTED_IDS)));
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
      .withFileDefinition(requestFileDefinition)
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
      .withJobExecutionId(JOB_EXECUTION_ID);
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
