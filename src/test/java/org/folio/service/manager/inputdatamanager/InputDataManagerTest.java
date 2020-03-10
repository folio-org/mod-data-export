package org.folio.service.manager.inputdatamanager;

import com.google.common.collect.Lists;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import org.assertj.core.util.Maps;
import org.folio.dao.impl.JobExecutionDaoImpl;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.manager.exportmanager.ExportManager;
import org.folio.service.manager.exportmanager.ExportPayload;
import org.folio.service.manager.inputdatamanager.datacontext.InputDataContext;
import org.folio.service.manager.inputdatamanager.reader.SourceReader;
import org.folio.service.manager.exportresult.ExportResult;
import org.folio.service.upload.definition.FileDefinitionService;
import org.folio.util.OkapiConnectionParams;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertThat;

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

  @InjectMocks
  @Spy
  private InputDataManagerImpl inputDataManager;

  @Mock
  private SourceReader sourceReader;
  @Mock
  private JobExecutionDaoImpl jobExecutionDao;
  @Mock
  private FileDefinitionService fileDefinitionService;
  @Mock
  private Iterator<List<String>> sourceStream;
  @Mock
  private JsonObject exportRequestJson;
  @Mock
  private JsonObject requestParamsJson;
  @Mock
  private ExportManager exportManager;
  @Mock
  private InputDataContext inputDataContext;

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

  private ExportRequest exportRequest;
  private FileDefinition requestFileDefinition;
  private Map<String, String> requestParams;
  private FileDefinition fileExportDefinition;


  @Before
  public void setUp() {
    initializeInputDataManager();
    requestFileDefinition = createRequestFileDefinition();
    fileExportDefinition = createFileExportDefinition();
    exportRequest = createExportRequest();
    requestParams = Maps.<String, String>newHashMap(OKAPI_HEADER_TENANT, TENANT_ID);
    when(exportRequestJson.mapTo(ExportRequest.class)).thenReturn(exportRequest);
    doReturn(exportManager).when(inputDataManager).getExportManager();
    doReturn(2).when(inputDataManager).getBatchSize();
  }

  @Test
  public void shouldNotInitExportSuccessfully_andSetStatusError_whenSourceStreamReaderEmpty() {
    //given
    doReturn(TIMESTAMP).when(inputDataManager).getCurrentTimestamp();
    when(sourceReader.getFileContentIterator(requestFileDefinition, BATCH_SIZE)).thenReturn(sourceStream);
    when(sourceStream.hasNext()).thenReturn(false);

    //when
    inputDataManager.initBlocking(exportRequestJson, requestParams);

    //then
    verify(fileDefinitionService).save(fileExportDefinitionCaptor.capture(), eq(TENANT_ID));
    FileDefinition fileDefinition = fileExportDefinitionCaptor.getValue();
    assertThat(fileDefinition.getStatus(), equalTo(FileDefinition.Status.ERROR));
    assertThat(fileDefinition.getFileName(), equalTo(FILE_NAME + DELIMETER + TIMESTAMP));
  }


  @Test
  public void shouldInitInputDataContextBeforeExportData_whenSourceStreamNotEmpty() {
    //given
    doReturn(TIMESTAMP).when(inputDataManager).getCurrentTimestamp();
    when(sourceReader.getFileContentIterator(requestFileDefinition, BATCH_SIZE)).thenReturn(sourceStream);
    when(sourceStream.hasNext()).thenReturn(true);
    when(fileDefinitionService.save(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture(fileExportDefinition));

    //when
    inputDataManager.initBlocking(exportRequestJson, requestParams);

    //then
    verify(inputDataLocalMap).put(eq(JOB_EXECUTION_ID), inputDataContextCaptor.capture());
    InputDataContext inputDataContext = inputDataContextCaptor.getValue();
    assertThat(inputDataContext.getFileContentIterator(), equalTo(sourceStream));
  }

  @Test
  public void shouldCreate_andSaveFileExportDefinitionBeforeExport_whenSourceStreamNotEmpty() {
    //given
    doReturn(TIMESTAMP).when(inputDataManager).getCurrentTimestamp();
    when(sourceReader.getFileContentIterator(requestFileDefinition, BATCH_SIZE)).thenReturn(sourceStream);
    when(sourceStream.hasNext()).thenReturn(true, false);
    when(fileDefinitionService.save(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture(fileExportDefinition));

    //when
    inputDataManager.initBlocking(exportRequestJson, requestParams);

    //then
    FileDefinition actualFileExportDefinition = fileExportDefinitionCaptor.getValue();
    assertThat(actualFileExportDefinition.getStatus(), equalTo(FileDefinition.Status.IN_PROGRESS));
    assertThat(actualFileExportDefinition.getFileName(), equalTo(FILE_NAME + DELIMETER + TIMESTAMP));
  }

  @Test
  public void shouldInit_andExportData_whenSourceStreamHasOneChunk() {
    //given
    doReturn(TIMESTAMP).when(inputDataManager).getCurrentTimestamp();
    when(sourceReader.getFileContentIterator(requestFileDefinition, BATCH_SIZE)).thenReturn(sourceStream);
    when(sourceStream.hasNext()).thenReturn(true, false);
    when(fileDefinitionService.save(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture(fileExportDefinition));
    when(inputDataContext.getFileContentIterator()).thenReturn(sourceStream);
    when(sourceStream.next()).thenReturn(EXPECTED_IDS);

    //when
    inputDataManager.initBlocking(exportRequestJson, requestParams);

    //then
    verify(exportManager).exportData(exportPayloadJsonCaptor.capture());
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
    when(sourceReader.getFileContentIterator(requestFileDefinition, BATCH_SIZE)).thenReturn(sourceStream);
    when(sourceStream.hasNext()).thenReturn(true, true);
    when(fileDefinitionService.save(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture(fileExportDefinition));
    when(inputDataContext.getFileContentIterator()).thenReturn(sourceStream);
    when(sourceStream.next()).thenReturn(EXPECTED_IDS);

    //when
    inputDataManager.initBlocking(exportRequestJson, requestParams);

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
  public void shouldFinishExportWithErrors_whenProceedWithExportStatusError() {
    //given
    ExportPayload exportPayload = createExportPayload();
    when(fileDefinitionService.update(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture());
    when(inputDataLocalMap.containsKey(JOB_EXECUTION_ID)).thenReturn(true);
    when(inputDataLocalMap.get(JOB_EXECUTION_ID)).thenReturn(inputDataContext);

    //when
    inputDataManager.proceedBlocking(JsonObject.mapFrom(exportPayload), ExportResult.ERROR);

    //then
    FileDefinition.Status actualFileDefinitionStatus = fileExportDefinitionCaptor.getValue().getStatus();
    assertThat(actualFileDefinitionStatus, equalTo(FileDefinition.Status.ERROR));
    verify(inputDataLocalMap).remove(JOB_EXECUTION_ID);
  }

  @Test
  public void shouldFinishExportSuccessfully_whenProceedWithExportStatusCompleted() {
    //given
    ExportPayload exportPayload = createExportPayload();
    when(fileDefinitionService.update(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture());
    when(inputDataLocalMap.containsKey(JOB_EXECUTION_ID)).thenReturn(true);
    when(inputDataLocalMap.get(JOB_EXECUTION_ID)).thenReturn(inputDataContext);

    //when
    inputDataManager.proceedBlocking(JsonObject.mapFrom(exportPayload), ExportResult.COMPLETED);

    //then
    FileDefinition.Status actualFileDefinitionStatus = fileExportDefinitionCaptor.getValue().getStatus();
    assertThat(actualFileDefinitionStatus, equalTo(FileDefinition.Status.COMPLETED));
    verify(inputDataLocalMap).remove(JOB_EXECUTION_ID);
  }

  @Test
  public void shouldFinishExportWithErrors_whenProceedWithExportStatusInProgress_andSourceStreamNull() {
    //given
    ExportPayload exportPayload = createExportPayload();
    when(fileDefinitionService.update(fileExportDefinitionCaptor.capture(), eq(TENANT_ID))).thenReturn(Future.succeededFuture());
    when(inputDataLocalMap.containsKey(JOB_EXECUTION_ID)).thenReturn(true);
    when(inputDataLocalMap.get(JOB_EXECUTION_ID)).thenReturn(inputDataContext);
    when(inputDataContext.getFileContentIterator()).thenReturn(null);

    //when
    inputDataManager.proceedBlocking(JsonObject.mapFrom(exportPayload), ExportResult.IN_PROGRESS);

    //then
    FileDefinition.Status actualFileDefinitionStatus = fileExportDefinitionCaptor.getValue().getStatus();
    assertThat(actualFileDefinitionStatus, equalTo(FileDefinition.Status.ERROR));
    verify(inputDataLocalMap).remove(JOB_EXECUTION_ID);
  }

  @Test
  public void shouldExportNextChunk_whenProceedWithExportStatusInProgress_andSourceStreamHasMoreChunksToExport() {
    //given
    ExportPayload exportPayload = createExportPayload();
    when(inputDataLocalMap.get(JOB_EXECUTION_ID)).thenReturn(inputDataContext);
    when(inputDataContext.getFileContentIterator()).thenReturn(sourceStream);
    when(sourceStream.hasNext()).thenReturn(true, true);
    when(sourceStream.next()).thenReturn(EXPECTED_IDS);

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
      .withFileDefinition(requestFileDefinition);
  }

  private FileDefinition createFileExportDefinition() {
    return new FileDefinition()
      .withFileName(EXPORT_FILE_DEFINITION_NAME);
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
}
