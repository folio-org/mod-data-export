package org.folio.rest.impl;

import static org.folio.rest.jaxrs.model.JobExecution.Status.COMPLETED;
import static org.folio.rest.jaxrs.model.JobExecution.Status.NEW;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpStatus;
import org.folio.TestUtil;
import org.folio.clients.InventoryClient;
import org.folio.clients.SourceRecordStorageClient;
import org.folio.clients.UsersClient;
import org.folio.dao.FileDefinitionDao;
import org.folio.dao.JobExecutionDao;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Charsets;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@Disabled("Disabled until all tests are moved to DataExportTest ")
@RunWith(VertxUnitRunner.class)
class EndToEndTest extends RestVerticleTestBase {

  private static final String SRS_RESPONSE_FILE_NAME = "mockData/srs/get_records_response.json";
  private static final String FILE_WITH_NON_EXITING_UUID = "InventoryUUIDsNonExiting.csv";
  private static final String FILE_WITH_TWO_BATCHES_OF_UUIDS = "InventoryUUIDsTwoBatches.csv";
  private static final String EMPTY_FILE = "InventoryUUIDsEmptyFile.csv";
  private static final String FILE_WITH_ONE_BATCH_OF_UUIDS = "InventoryUUIDsOneBatch.csv";
  private static final String DASH = "-";
  private static final String MRC_EXTENSION = "mrc";
  private static final long TIMER_DELAY = 5000L;
  private static final JsonObject USER = new JsonObject()
    .put("personal", new JsonObject()
      .put("firstName", "John")
      .put("lastName", "Doe")
    );
  private static final int CURRENT_RECORDS_2 = 2;
  private static final int CURRENT_RECORDS_8 = 8;
  private static final int LIMIT = 20;
  private static final String TOTAL_4 = "4";
  public static final String TOTAL_51 = "51";

  private static UsersClient mockUsersClient = Mockito.mock(UsersClient.class);
  private static SourceRecordStorageClient mockSrsClient = Mockito.mock(SourceRecordStorageClient.class);
  private static InventoryClient mockInventoryClient = Mockito.mock(InventoryClient.class);
  private static ExportStorageService mockExportStorageService = Mockito.mock(ExportStorageService.class);

  @Autowired
  private JobExecutionDao jobExecutionDao;
  @Autowired
  private FileDefinitionDao fileDefinitionDao;

  public EndToEndTest() {
    Context vertxContext = vertx.getOrCreateContext();
//    SpringContextUtil.init(vertxContext.owner(), vertxContext, EndToEndTest.TestConfig.class);
//    SpringContextUtil.autowireDependencies(this, vertxContext);
  }

  @Disabled("This test class will be removed in the future")
  public void before() {
    when(mockUsersClient.getById(anyString(), anyString(), ArgumentMatchers.any(OkapiConnectionParams.class))).thenReturn(Optional.of(USER));
    when(mockInventoryClient.getNatureOfContentTerms(anyString(), ArgumentMatchers.any(OkapiConnectionParams.class))).thenReturn(Collections.emptyMap());
  }


  @Test
  void shouldReturn_204Status_forHappyPathExport(TestContext context) throws IOException, InterruptedException {
    Async async = context.async();

    //given
    FileDefinition uploadedFileDefinition = givenUploadFile(FILE_WITH_ONE_BATCH_OF_UUIDS);

    //when
    ExportRequest exportRequest = getExportRequest(uploadedFileDefinition);
    Response response = postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);

    // then
    vertx.setTimer(TIMER_DELAY, handler -> {
      context.assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
      async.complete();
    });
  }

  @Test
  void shouldExportFileWithRecords_whenExportInOneBatch(TestContext context) throws IOException, InterruptedException {
    Async async = context.async();

    //given
    givenSetSourceStorageMockToReturnRecords();
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = givenCaptureFileExportDefinition();
    FileDefinition uploadedFileDefinition = givenUploadFile(FILE_WITH_ONE_BATCH_OF_UUIDS);

    //when
    ExportRequest exportRequest = getExportRequest(uploadedFileDefinition);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);

    // then
    vertx.setTimer(TIMER_DELAY, handler -> fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), okapiConnectionParams.getTenantId())
      .compose(fileExportDefinitionOptional -> assertCompletedFileDefinitionAndExportedFile(context, fileExportDefinitionOptional))
      .compose(fileExportDefinition -> jobExecutionDao.getById(fileExportDefinition.getJobExecutionId(), okapiConnectionParams.getTenantId())
        .compose(jobExecutionOptional -> assertSuccessJobExecution(context, fileExportDefinition, jobExecutionOptional, CURRENT_RECORDS_2, TOTAL_4))
        .onComplete(succeeded -> async.complete())
      ));
  }

  @Test
  void shouldExportFileWithRecords_whenExportInTwoBatches(TestContext context) throws IOException, InterruptedException {
    Async async = context.async();

    //given
    givenSetSourceStorageMockToReturnRecords();
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = givenCaptureFileExportDefinition();
    FileDefinition uploadedFileDefinition = givenUploadFile(FILE_WITH_TWO_BATCHES_OF_UUIDS);

    //when
    ExportRequest exportRequest = getExportRequest(uploadedFileDefinition);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);

    // then
    vertx.setTimer(TIMER_DELAY, handler -> fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), okapiConnectionParams.getTenantId())
      .compose(fileExportDefinitionOptional -> assertCompletedFileDefinitionAndExportedFile(context, fileExportDefinitionOptional))
      .compose(fileExportDefinition -> jobExecutionDao.getById(fileExportDefinition.getJobExecutionId(), okapiConnectionParams.getTenantId())
        .compose(jobExecutionOptional -> assertSuccessJobExecution(context, fileExportDefinition, jobExecutionOptional, CURRENT_RECORDS_8, TOTAL_51))
        .onComplete(succeeded -> async.complete())
      ));
  }

  @Test
  void shouldNotExportFile_whenUploadedFileContainsOnlyNonExistingUuid(TestContext context) throws IOException, InterruptedException {
    Async async = context.async();

    //given
    givenSetUpSoureRecordMockToReturnEmptyRecords();
    FileDefinition uploadedFileDefinition = givenUploadFile(FILE_WITH_NON_EXITING_UUID);

    //when
    ExportRequest exportRequest = getExportRequest(uploadedFileDefinition);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);

    // then
    vertx.setTimer(10000L, handler -> {
      jobExecutionDao.getById(uploadedFileDefinition.getJobExecutionId(), okapiConnectionParams.getTenantId())
          .compose(jobExecutionOptional -> assertFailJobExecution(context, jobExecutionOptional))
          .onComplete(succeeded -> async.complete());
    });
  }

  @Test
  void shouldUpdateJobExecutionStatusToFail_whenUploadedFileIsEmpty(TestContext context) throws IOException, InterruptedException {
    Async async = context.async();

    //given
    FileDefinition uploadedFileDefinition = givenUploadFile(EMPTY_FILE);

    //when
    ExportRequest exportRequest = getExportRequest(uploadedFileDefinition);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);

    // then
    vertx.setTimer(TIMER_DELAY, handler -> {
      jobExecutionDao.getById(uploadedFileDefinition.getJobExecutionId(), okapiConnectionParams.getTenantId())
        .compose(jobExecutionOptional -> assertFailJobExecution(context, jobExecutionOptional))
        .onComplete(succeeded -> async.complete());
    });
  }

  @Test
  void shouldReturn_400Status_forReUploadFile(TestContext context) throws IOException, InterruptedException {
    Async async = context.async();

    FileDefinition uploadedFileDefinition = givenUploadFile(FILE_WITH_ONE_BATCH_OF_UUIDS);
    //when
    File fileToUpload = TestUtil.getFileFromResources(FILES_FOR_UPLOAD_DIRECTORY + FILE_WITH_ONE_BATCH_OF_UUIDS);
    RequestSpecification binaryRequestSpecification = buildRequestSpecification(okapiConnectionParams.getTenantId());

 // then
    vertx.setTimer(TIMER_DELAY, handler -> {
      jobExecutionDao.getById(uploadedFileDefinition.getJobExecutionId(), okapiConnectionParams.getTenantId())
        .compose(jobExecutionOptional -> assertCreationJobExecution(context, jobExecutionOptional))
        .compose(succeeded -> {
          return Future.succeededFuture();
        });
    });
    RestAssured.given()
    .spec(binaryRequestSpecification)
    .when()
    .body(FileUtils.openInputStream(fileToUpload))
    .post(FILE_DEFINITION_SERVICE_URL + uploadedFileDefinition.getId() + UPLOAD_URL)
    .then()
    .statusCode(HttpStatus.SC_BAD_REQUEST)
    .body(containsString(ErrorCode.FILE_ALREADY_UPLOADED.getDescription()))
    .log()
    .all();

    async.complete();

  }


  private FileDefinition givenUploadFile(String fileName) throws IOException {
    File fileToUpload = TestUtil.getFileFromResources(FILES_FOR_UPLOAD_DIRECTORY + fileName);
    RequestSpecification binaryRequestSpecification = buildRequestSpecification(okapiConnectionParams.getTenantId());

    FileDefinition givenFileDefinition = new FileDefinition()
      .withId(UUID.randomUUID().toString())
      .withFileName(fileName);

    postRequest(JsonObject.mapFrom(givenFileDefinition), FILE_DEFINITION_SERVICE_URL);

    return RestAssured.given()
      .spec(binaryRequestSpecification)
      .when()
      .body(FileUtils.openInputStream(fileToUpload))
      .post(FILE_DEFINITION_SERVICE_URL + givenFileDefinition.getId() + UPLOAD_URL)
      .then()
      .extract().body().as(FileDefinition.class);
  }



  private void givenSetUpSoureRecordMockToReturnEmptyRecords() {
    when(mockSrsClient.getRecordsByInstanceIds(any(List.class), anyString(), any(OkapiConnectionParams.class))).thenReturn(Optional.empty());
  }

  private ArgumentCaptor<FileDefinition> givenCaptureFileExportDefinition() {
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = ArgumentCaptor.forClass(FileDefinition.class);
    doNothing().when(mockExportStorageService).storeFile(fileExportDefinitionCaptor.capture(), eq(okapiConnectionParams.getTenantId()));
    return fileExportDefinitionCaptor;
  }

  private void givenSetSourceStorageMockToReturnRecords() throws IOException {
    String json = FileUtils.readFileToString(TestUtil.getFileFromResources(SRS_RESPONSE_FILE_NAME), Charsets.UTF_8);
    JsonObject data = new JsonObject(json);
    when(mockSrsClient.getRecordsByInstanceIds(any(List.class), anyString(), any(OkapiConnectionParams.class))).thenReturn(Optional.of(data));
  }

  private Future<FileDefinition> assertCompletedFileDefinitionAndExportedFile(TestContext context, Optional<FileDefinition> fileExportDefinitionOptional) {
    FileDefinition fileExportDefinition = fileExportDefinitionOptional.get();
    File generatedExportFile = new File(fileExportDefinition.getSourcePath());
    String generatedExportFileContent = readFileContent(context, generatedExportFile);
    String generatedFileName = generatedExportFile.getName();

    context.assertEquals(fileExportDefinition.getStatus(), FileDefinition.Status.COMPLETED);
    context.assertNotNull(generatedExportFileContent);
    context.assertEquals(FilenameUtils.getExtension(generatedFileName), MRC_EXTENSION);
    return Future.succeededFuture(fileExportDefinition);
  }

  private Future<Object> assertSuccessJobExecution(TestContext context, FileDefinition fileDefinition,  Optional<JobExecution> jobExecutionOptional, Integer currentNumber, String total) {
    JobExecution jobExecution = jobExecutionOptional.get();
    context.assertTrue(isFileNameContainsJobExecutionHrId(new File(fileDefinition.getSourcePath()).getName(), jobExecution.getHrId()));
    context.assertEquals(jobExecution.getStatus(), COMPLETED);
    context.assertNotNull(jobExecution.getCompletedDate());
    context.assertEquals(jobExecution.getProgress().getExported(), currentNumber);
    context.assertEquals(jobExecution.getProgress().getTotal(), total);
    return Future.succeededFuture();
  }

  private Future<Object> assertCreationJobExecution(TestContext context,  Optional<JobExecution> jobExecutionOptional) {
    JobExecution jobExecution = jobExecutionOptional.get();
    context.assertEquals(jobExecution.getStatus(), NEW);
    return Future.succeededFuture();
  }

  private Future<Object> assertFailJobExecution(TestContext context, Optional<JobExecution> jobExecutionOptional) {
    JobExecution jobExecution = jobExecutionOptional.get();
    context.assertEquals(jobExecution.getStatus(), JobExecution.Status.FAIL);
    return Future.succeededFuture();
  }

  private ExportRequest getExportRequest(FileDefinition uploadedFileDefinition) {
    return new ExportRequest()
      .withFileDefinitionId(uploadedFileDefinition.getId())
      .withJobProfileId(UUID.randomUUID().toString());
  }


  private String readFileContent(TestContext context, File generatedExportFile) {
    String generatedExportFileContent = null;
    try {
      generatedExportFileContent = FileUtils.readFileToString(generatedExportFile, Charsets.UTF_8);
    } catch (IOException e) {
      e.printStackTrace();
      context.fail();
    }
    return generatedExportFileContent;
  }

  private boolean isFileNameContainsJobExecutionHrId(String generatedFileName, int jobExecutionHrId) {
    return FilenameUtils.getBaseName(generatedFileName).split(DASH)[1].equals(jobExecutionHrId);
  }

//  @Configuration
//  @Import(ApplicationConfig.class)
//  public static class TestConfig {
//
//    @Bean
//    @Primary
//    public UsersClient getMockUsersClient() {
//      return mockUsersClient;
//    }
//
//    @Bean
//    @Primary
//    public InventoryClient getMockInventoryClient() { return mockInventoryClient; }
//
//    @Bean
//    @Primary
//    public SourceRecordStorageClient getMockSourceRecordStorageClient() {
//      return mockSrsClient;
//    }
//
//    @Bean
//    @Primary
//    public ExportStorageService getMockExportStorageService() {
//      return mockExportStorageService;
//    }
//  }
}
