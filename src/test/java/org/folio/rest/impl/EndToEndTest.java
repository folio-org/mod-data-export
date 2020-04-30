package org.folio.rest.impl;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import kotlin.text.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpStatus;
import org.folio.TestUtil;
import org.folio.clients.StorageClient;
import org.folio.clients.UsersClient;
import org.folio.config.ApplicationConfig;
import org.folio.dao.FileDefinitionDao;
import org.folio.dao.JobExecutionDao;
import org.folio.rest.RestVerticleTestBase;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.jaxrs.model.JobExecution.Status.SUCCESS;
import static org.folio.rest.jaxrs.model.JobExecution.Status.NEW;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;


@RunWith(VertxUnitRunner.class)
public class EndToEndTest extends RestVerticleTestBase {

  private static final String EXPORT_URL = "/data-export/export";
  private static final String FILE_DEFINITION_SERVICE_URL = "/data-export/fileDefinitions/";
  private static final String STORAGE_DIRECTORY_PATH = "./storage/files";
  private static final String FILES_FOR_UPLOAD_DIRECTORY = "endToEndTestFiles/";
  private static final String UPLOAD_URL = "/upload";
  private static final String SRS_RESPONSE_FILE_NAME = "clients/srsResponse.json";
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

  private static UsersClient mockUsersClient = Mockito.mock(UsersClient.class);
  private static StorageClient mockStorageClient = Mockito.mock(StorageClient.class);
  private static ExportStorageService mockExportStorageService = Mockito.mock(ExportStorageService.class);

  @Autowired
  private JobExecutionDao jobExecutionDao;
  @Autowired
  private FileDefinitionDao fileDefinitionDao;

  public EndToEndTest() {
    Context vertxContext = vertx.getOrCreateContext();
    SpringContextUtil.init(vertxContext.owner(), vertxContext, EndToEndTest.TestConfig.class);
    SpringContextUtil.autowireDependencies(this, vertxContext);
  }

  @Before
  public void before() {
    when(mockUsersClient.getById(ArgumentMatchers.anyString(), ArgumentMatchers.any(OkapiConnectionParams.class))).thenReturn(Optional.of(USER));
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(new File(STORAGE_DIRECTORY_PATH));
  }

  @Test
  public void shouldReturn_204Status_forHappyPathExport(TestContext context) throws IOException {
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
  public void shouldExportFileWithRecords_whenExportInOneBatch(TestContext context) throws IOException {
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
      .compose(fileExportDefinitionOptional -> assertCompletedFileDefinitionAndExportedFileInOneBatch(context, fileExportDefinitionOptional.get()))
      .compose(fileExportDefinition -> jobExecutionDao.getById(fileExportDefinition.getJobExecutionId(), okapiConnectionParams.getTenantId())
        .compose(jobExecutionOptional -> assertSuccessJobExecution(context, fileExportDefinition, jobExecutionOptional, CURRENT_RECORDS_2))
        .onComplete(succeeded -> async.complete())
      ));
  }

  @Test
  public void shouldExportFileWithRecords_whenExportInTwoBatches(TestContext context) throws IOException {
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
      .compose(fileExportDefinitionOptional -> assertCompletedFileDefinitionAndExportedFileInTwoBatches(context, fileExportDefinitionOptional.get()))
      .compose(fileExportDefinition -> jobExecutionDao.getById(fileExportDefinition.getJobExecutionId(), okapiConnectionParams.getTenantId())
        .compose(jobExecutionOptional -> assertSuccessJobExecution(context, fileExportDefinition, jobExecutionOptional, CURRENT_RECORDS_8))
        .onComplete(succeeded -> async.complete())
      ));
  }

  @Test
  public void shouldNotExportFile_whenUploadedFileContainsOnlyNonExistingUuid(TestContext context) throws IOException {
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
  public void shouldUpdateJobExecutionStatusToFail_whenUploadedFileIsEmpty(TestContext context) throws IOException {
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
  public void shouldReturn_400Status_forReUploadFile(TestContext context) throws IOException {
    Async async = context.async();

    FileDefinition uploadedFileDefinition = givenUploadFile(FILE_WITH_ONE_BATCH_OF_UUIDS);
    //when
    File fileToUpload = getFileFromResourceByName(FILES_FOR_UPLOAD_DIRECTORY + FILE_WITH_ONE_BATCH_OF_UUIDS);
    RequestSpecification binaryRequestSpecification = buildRequestSpecification();

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
    File fileToUpload = getFileFromResourceByName(FILES_FOR_UPLOAD_DIRECTORY + fileName);
    RequestSpecification binaryRequestSpecification = buildRequestSpecification();

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



  private RequestSpecification buildRequestSpecification() {
    return new RequestSpecBuilder()
      .setContentType(ContentType.BINARY)
      .addHeader(OKAPI_HEADER_TENANT, TENANT_ID)
      .setBaseUri(OKAPI_URL)
      .build();
  }

  private void givenSetUpSoureRecordMockToReturnEmptyRecords() {
    when(mockStorageClient.getByIdsFromSRS(any(List.class), any(OkapiConnectionParams.class), eq(LIMIT))).thenReturn(Optional.empty());
  }

  private ArgumentCaptor<FileDefinition> givenCaptureFileExportDefinition() {
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = ArgumentCaptor.forClass(FileDefinition.class);
    doNothing().when(mockExportStorageService).storeFile(fileExportDefinitionCaptor.capture(), eq(okapiConnectionParams.getTenantId()));
    return fileExportDefinitionCaptor;
  }

  private void givenSetSourceStorageMockToReturnRecords() throws IOException {
    String json = FileUtils.readFileToString(getFileFromResourceByName(SRS_RESPONSE_FILE_NAME), Charsets.UTF_8);
    JsonObject data = new JsonObject(json);
    when(mockStorageClient.getByIdsFromSRS(any(List.class), any(OkapiConnectionParams.class), eq(LIMIT))).thenReturn(Optional.of(data));
  }

  private Future<FileDefinition> assertCompletedFileDefinitionAndExportedFileInOneBatch(TestContext context, FileDefinition fileExportDefinition) {
    File actualGeneratedFile = new File(fileExportDefinition.getSourcePath());
    String actualGeneratedFileContent = readFileContent(context, actualGeneratedFile);
    String expectedGeneratedFileContent = TestUtil.getResourceAsString("endToEndTestFiles/ExpectedGeneratedFileIn1Batch.mrc");
    context.assertEquals(fileExportDefinition.getStatus(), FileDefinition.Status.COMPLETED);
    context.assertNotNull(actualGeneratedFile);
    context.assertEquals(FilenameUtils.getExtension(actualGeneratedFile.getName()), MRC_EXTENSION);
    context.assertEquals(expectedGeneratedFileContent, actualGeneratedFileContent);
    return Future.succeededFuture(fileExportDefinition);
  }

  private Future<FileDefinition> assertCompletedFileDefinitionAndExportedFileInTwoBatches(TestContext context, FileDefinition fileExportDefinition) {
    File actualGeneratedFile = new File(fileExportDefinition.getSourcePath());
    context.assertEquals(fileExportDefinition.getStatus(), FileDefinition.Status.COMPLETED);
    context.assertNotNull(actualGeneratedFile);
    context.assertEquals(FilenameUtils.getExtension(actualGeneratedFile.getName()), MRC_EXTENSION);
    return Future.succeededFuture(fileExportDefinition);
  }

  private Future<Object> assertSuccessJobExecution(TestContext context, FileDefinition fileDefinition,  Optional<JobExecution> jobExecutionOptional, Integer currentNumber) {
    JobExecution jobExecution = jobExecutionOptional.get();
    context.assertTrue(isFileNameContainsJobExecutionHrId(new File(fileDefinition.getSourcePath()).getName(), jobExecution.getHrId()));
    context.assertEquals(jobExecution.getStatus(), SUCCESS);
    context.assertNotNull(jobExecution.getCompletedDate());
    context.assertEquals(jobExecution.getProgress().getCurrent(), currentNumber);
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

  private File getFileFromResourceByName(String fileName) {
    ClassLoader classLoader = getClass().getClassLoader();
    return new File(Objects.requireNonNull(classLoader.getResource(fileName)).getFile());
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

  private boolean isFileNameContainsJobExecutionHrId(String generatedFileName, String jobExecutionHrId) {
    return FilenameUtils.getBaseName(generatedFileName).split(DASH)[1].equals(jobExecutionHrId);
  }

  @Configuration
  @Import(ApplicationConfig.class)
  public static class TestConfig {

    @Bean
    @Primary
    public UsersClient getMockUsersClient() {
      return mockUsersClient;
    }

    @Bean
    @Primary
    public StorageClient getMockSourceRecordStorageClient() {
      return mockStorageClient;
    }

    @Bean
    @Primary
    public ExportStorageService getMockExportStorageService() {
      return mockExportStorageService;
    }
  }
}
