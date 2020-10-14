package org.folio.rest.impl;

import static org.folio.TestUtil.getFileFromResources;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.jaxrs.model.FileDefinition.UploadFormat.CQL;
import static org.folio.rest.jaxrs.model.FileDefinition.UploadFormat.CSV;
import static org.folio.rest.jaxrs.model.JobExecution.Status.COMPLETED;
import static org.folio.rest.jaxrs.model.JobExecution.Status.COMPLETED_WITH_ERRORS;
import static org.folio.rest.jaxrs.model.JobExecution.Status.FAIL;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.folio.TestUtil;
import org.folio.config.ApplicationConfig;
import org.folio.dao.FileDefinitionDao;
import org.folio.dao.JobExecutionDao;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.FileDefinition.UploadFormat;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.service.logs.ErrorLogService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ExternalPathResolver;
import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Context;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataExportTest extends RestVerticleTestBase {

  private static final String DATA_EXPORT_JOB_PROFILES_ENDPOINT = "/data-export/job-profiles";
  private static final String DATA_EXPORT_MAPPING_PROFILES_ENDPOINT = "/data-export/mapping-profiles";
  private static final long TIMER_DELAY = 5000L;
  private static final String UUIDS_FOR_COMPLETED_JOB = "uuids_for_completed_job.csv";
  private static final String UUIDS_FOR_COMPLETED_WITH_ERRORS_JOB = "uuids_for_completed_with_errors_job.csv";
  private static final String UUIDS_INVENTORY = "uuids_inventory.csv";
  private static final String EMPTY_FILE = "InventoryUUIDsEmptyFile.csv";
  private static final String FILE_WHEN_INVENTORY_RETURNS_500 = "inventoryUUIDReturn500.csv";
  private static final String UUIDS_CQL = "InventoryUUIDs.cql";
  public static final int EXPORTED_RECORDS_EMPTY = 0;
  public static final int EXPORTED_RECORDS_NUMBER_1 = 1;
  public static final int EXPORTED_RECORDS_NUMBER_2 = 2;
  public static final int EXPORTED_RECORDS_NUMBER_3 = 3;
  private static final String CUSTOM_TEST_TENANT = "custom_test_tenant";
  private static final Header CUSTOM_TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, CUSTOM_TEST_TENANT);

  private static ExportStorageService mockExportStorageService = Mockito.mock(ExportStorageService.class);

  @Autowired
  private JobExecutionDao jobExecutionDao;
  @Autowired
  private FileDefinitionDao fileDefinitionDao;
  @Autowired
  private ErrorLogService errorLogService;

  public DataExportTest() {
    Context vertxContext = vertx.getOrCreateContext();
    SpringContextUtil.init(vertxContext.owner(), vertxContext, DataExportTest.TestMock.class);
    SpringContextUtil.autowireDependencies(this, vertxContext);
  }


//  @Test
//  void testExport_uploadingEmptyCqlFile_FAILED_job(VertxTestContext context) throws IOException {
//    //given
//    String tenantId = okapiConnectionParams.getTenantId();
//    FileDefinition uploadedFileDefinition = uploadFile(EMPTY_FILE, CQL, buildRequestSpecification(tenantId));
//    // when
//    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition);
//    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
//    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
//    // then
//    vertx.setTimer(TIMER_DELAY, handler ->
//      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
//        JobExecution jobExecution = optionalJobExecution.get();
//        context.verify(() -> {
//          assertJobExecution(jobExecution, FAIL, EXPORTED_RECORDS_EMPTY);
//          context.completeNow();
//        });
//      }));
//  }
//
//  @Test
//  void testExport_uploadingEmptyCsvFile_FAILED_job(VertxTestContext context) throws IOException {
//    //given
//    String tenantId = okapiConnectionParams.getTenantId();
//    FileDefinition uploadedFileDefinition = uploadFile(EMPTY_FILE, CSV, buildRequestSpecification(tenantId));
//    // when
//    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition);
//    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
//    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
//    // then
//    vertx.setTimer(TIMER_DELAY, handler ->
//      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
//        JobExecution jobExecution = optionalJobExecution.get();
//        context.verify(() -> {
//          assertJobExecution(jobExecution, FAIL, EXPORTED_RECORDS_EMPTY);
//          context.completeNow();
//        });
//      }));
//  }

  @Test
  void testExportByCSV_UnderlyingSrsOnly_COMPLETED_job(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(UUIDS_FOR_COMPLETED_JOB, CSV, buildRequestSpecification(tenantId));
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), tenantId).onSuccess(optionalFileDefinition -> {
          context.verify(() -> {
            FileDefinition fileExportDefinition = optionalFileDefinition.get();
            assertJobExecution(jobExecution, COMPLETED, EXPORTED_RECORDS_NUMBER_2);
            assertCompletedFileDefinitionAndExportedFile(fileExportDefinition);
            validateExternalCalls();
            context.completeNow();
          });
        });
      }));
  }

  @Test
  void testExportByCSV_UnderlyingSrsOnly_COMPLETED_WITH_ERRORS_job(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(UUIDS_FOR_COMPLETED_WITH_ERRORS_JOB, CSV, buildRequestSpecification(tenantId));
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), tenantId).onSuccess(optionalFileDefinition -> {
          context.verify(() -> {
            assertJobExecution(jobExecution, COMPLETED_WITH_ERRORS, EXPORTED_RECORDS_NUMBER_3);
            validateExternalCallsForInventory();
            context.completeNow();
          });
        });
      }));
  }

  @Test
  void testExportByCSV_GenerateRecordsOnFly(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(UUIDS_INVENTORY, CSV, buildRequestSpecification(tenantId));
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler -> {
      jobExecutionDao.getById(jobExecutionId, tenantId)
        .onSuccess(optionalJobExecution -> {
          JobExecution jobExecution = optionalJobExecution.get();
          fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), tenantId).onSuccess(optionalFileDefinition -> {
            context.verify(() -> {
              assertJobExecution(jobExecution, COMPLETED, EXPORTED_RECORDS_NUMBER_1);
              validateExternalCallsForInventory();
              context.completeNow();
            });
          });
        });
    });
  }

  @Test
  void testExportByCQL_GenerateRecordsOnFly_andUnderlyingSrs(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    RequestSpecification requestSpecificationForMockServer = new RequestSpecBuilder()
      .setContentType(ContentType.BINARY)
      .addHeader(OKAPI_HEADER_TENANT, tenantId)
      .addHeader(OKAPI_HEADER_URL, MOCK_OKAPI_URL)
      .setBaseUri(BASE_OKAPI_URL)
      .build();
    FileDefinition uploadedFileDefinition = uploadFile(UUIDS_CQL, CQL, requestSpecificationForMockServer);
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler -> {
      jobExecutionDao.getById(jobExecutionId, tenantId)
        .onSuccess(optionalJobExecution -> {
          JobExecution jobExecution = optionalJobExecution.get();
          fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), tenantId).onSuccess(optionalFileDefinition -> {
            context.verify(() -> {
              assertJobExecution(jobExecution, COMPLETED, EXPORTED_RECORDS_NUMBER_2);
              validateExternalCallsForInventory();
              context.completeNow();
            });
          });
        });
    });
  }

  @Disabled("Disabled for Q3-2020(and until futher decision is made) as we are going to generate marc on the fly for custom profiles")
  @Test
  void testExportByCSV_UnderlyingSrsWithProfileTransformations(VertxTestContext context) throws IOException {
    postToTenant(CUSTOM_TENANT_HEADER);
    // given
    String tenantId = CUSTOM_TEST_TENANT;
    FileDefinition uploadedFileDefinition = uploadFile("uuids_forTransformation.csv", CSV, tenantId, buildRequestSpecification(tenantId));
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    String jobProfileId = buildCustomJobProfile(CUSTOM_TEST_TENANT);
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, jobProfileId);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL, tenantId);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), tenantId).onSuccess(optionalFileDefinition -> {
          context.verify(() -> {
            FileDefinition fileExportDefinition = optionalFileDefinition.get();
            assertJobExecution(jobExecution, COMPLETED, EXPORTED_RECORDS_NUMBER_1);
            assertCompletedFileDefinitionAndExportedFile(fileExportDefinition, "expected_marc_MappingTransformations.json");
            validateExternalCallsForMappingProfileTransformations();
            context.completeNow();
          });
        });
      }));
  }

  @Test
  void testExportByCSV_UnderlyingSrsWithProfileTransformationsNoCallToSRS(VertxTestContext context) throws IOException {
    postToTenant(CUSTOM_TENANT_HEADER);
    // given
    String tenantId = CUSTOM_TEST_TENANT;
    FileDefinition uploadedFileDefinition = uploadFile("uuids_forTransformation.csv", CSV, tenantId, buildRequestSpecification(tenantId));
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    String jobProfileId = buildCustomJobProfile(CUSTOM_TEST_TENANT);
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, jobProfileId);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL, tenantId);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), tenantId).onSuccess(optionalFileDefinition -> {
          context.verify(() -> {
            assertJobExecution(jobExecution, COMPLETED, EXPORTED_RECORDS_NUMBER_1);
            validateExternalCallsForMappingProfileTransformations();
            context.completeNow();
          });
        });
      }));
  }


  @Test
  void shouldNotExportFile_whenInventoryReturnServerError(VertxTestContext context) throws IOException, InterruptedException {
    postToTenant(CUSTOM_TENANT_HEADER);
    // given
    String tenantId = CUSTOM_TEST_TENANT;
    //given
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    FileDefinition uploadedFileDefinition = uploadFile(FILE_WHEN_INVENTORY_RETURNS_500, CSV, CUSTOM_TEST_TENANT, buildRequestSpecification(tenantId));
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, "6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a");

    //when
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL, tenantId);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();

    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        errorLogService.getByJobExecutionId("jobExecutionId=" + jobExecutionId, 0, 20, tenantId).onSuccess(errorLogCollection -> {
          context.verify(() -> {
            assertJobExecution(jobExecution, FAIL, 0);
            assertErrorLogs(errorLogCollection, jobExecutionId);
            context.completeNow();
          });
        });
      }));
  }


  private String buildCustomJobProfile(String tenantID) {
    String mappingProfile = TestUtil.readFileContentFromResources(FILES_FOR_UPLOAD_DIRECTORY + "mappingProfile.json");
    JsonObject mappingProfilejs = new JsonObject(mappingProfile);
    postRequest(mappingProfilejs, DATA_EXPORT_MAPPING_PROFILES_ENDPOINT, tenantID);

    String jobProfile = TestUtil.readFileContentFromResources(FILES_FOR_UPLOAD_DIRECTORY + "jobProfile.json");
    JsonObject jobProfilejs = new JsonObject(jobProfile);
    Response response = postRequest(jobProfilejs, DATA_EXPORT_JOB_PROFILES_ENDPOINT, tenantID);
    return response.then()
      .extract()
      .path("id");
  }

  private ArgumentCaptor<FileDefinition> captureFileExportDefinition(String tenantId) {
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = ArgumentCaptor.forClass(FileDefinition.class);
    doNothing().when(mockExportStorageService).storeFile(fileExportDefinitionCaptor.capture(), eq(tenantId));
    return fileExportDefinitionCaptor;
  }

  private FileDefinition uploadFile(String fileName, UploadFormat format, String tenantId, RequestSpecification binaryRequestSpecification) throws IOException {
    File fileToUpload = TestUtil.getFileFromResources(FILES_FOR_UPLOAD_DIRECTORY + fileName);

    FileDefinition givenFileDefinition = new FileDefinition()
      .withId(UUID.randomUUID().toString())
      .withFileName(fileName)
      .withUploadFormat(format);

    postRequest(JsonObject.mapFrom(givenFileDefinition), FILE_DEFINITION_SERVICE_URL, tenantId);

    return RestAssured.given()
      .spec(binaryRequestSpecification)
      .when()
      .body(FileUtils.openInputStream(fileToUpload))
      .post(FILE_DEFINITION_SERVICE_URL + givenFileDefinition.getId() + UPLOAD_URL)
      .then()
      .extract().body().as(FileDefinition.class);
  }

  private FileDefinition uploadFile(String fileName, UploadFormat format, RequestSpecification binaryRequestSpecification) throws IOException {
    return uploadFile(fileName, format, okapiConnectionParams.getTenantId(), binaryRequestSpecification);
  }

  private ExportRequest buildExportRequest(FileDefinition uploadedFileDefinition) {
    return new ExportRequest()
      .withFileDefinitionId(uploadedFileDefinition.getId())
      .withJobProfileId(DEFAULT_JOB_PROFILE_ID);
  }

  private ExportRequest buildExportRequest(FileDefinition uploadedFileDefinition, String jobProfileID) {
    return new ExportRequest()
      .withFileDefinitionId(uploadedFileDefinition.getId())
      .withJobProfileId(jobProfileID);
  }

  private void assertCompletedFileDefinitionAndExportedFile(FileDefinition fileExportDefinition) {
    String actualGeneratedFileContent = TestUtil.readFileContent(fileExportDefinition.getSourcePath());
    String expectedGeneratedFileContent = TestUtil.readFileContentFromResources(FILES_FOR_UPLOAD_DIRECTORY + "GeneratedFileForSrsRecordsOnly.mrc");
    assertEquals(expectedGeneratedFileContent, actualGeneratedFileContent);
    assertEquals(FileDefinition.Status.COMPLETED, fileExportDefinition.getStatus());
  }

  private void assertCompletedFileDefinitionAndExportedFile(FileDefinition fileExportDefinition, String fileName) throws FileNotFoundException {
    String actualGeneratedFileContent = TestUtil.readFileContent(fileExportDefinition.getSourcePath());
    File expectedJsonRecords = getFileFromResources(FILES_FOR_UPLOAD_DIRECTORY + fileName);
    String expectedMarcRecord = TestUtil.getMarcFromJson(expectedJsonRecords);
    assertEquals(expectedMarcRecord, actualGeneratedFileContent);
    assertEquals(FileDefinition.Status.COMPLETED, fileExportDefinition.getStatus());
  }

  private void assertJobExecution(JobExecution jobExecution, JobExecution.Status status, Integer numberOfExportedRecords) {
    assertEquals(status, jobExecution.getStatus());
    assertNotNull(jobExecution.getCompletedDate());
    assertEquals(numberOfExportedRecords, jobExecution.getProgress().getExported());
  }

  private void assertErrorLogs(ErrorLogCollection errorLogCollection, String jobExecutionId) {
    Assert.assertEquals(4, errorLogCollection.getErrorLogs().size());
    ErrorLog errorLog1 = errorLogCollection.getErrorLogs().get(0);
    ErrorLog errorLog2 = errorLogCollection.getErrorLogs().get(1);
    ErrorLog errorLog3 = errorLogCollection.getErrorLogs().get(2);
    ErrorLog errorLog4 = errorLogCollection.getErrorLogs().get(3);
    assertEquals(jobExecutionId, errorLog1.getJobExecutionId());
    assertEquals(jobExecutionId, errorLog2.getJobExecutionId());
    assertEquals(jobExecutionId, errorLog3.getJobExecutionId());
    assertEquals(jobExecutionId, errorLog4.getJobExecutionId());
    assertEquals(ErrorLog.LogLevel.ERROR, errorLog1.getLogLevel());
    assertEquals(ErrorLog.LogLevel.ERROR, errorLog2.getLogLevel());
    assertEquals(ErrorLog.LogLevel.ERROR, errorLog3.getLogLevel());
    assertEquals(ErrorLog.LogLevel.ERROR, errorLog4.getLogLevel());
    for (ErrorLog errorLog : errorLogCollection.getErrorLogs()) {
      Assert.assertTrue(errorLog.getReason().contains("Get invalid response with status: 500")
        || errorLog.getReason().contains("Nothing to export: no binary file generated")
        || errorLog.getReason().contains("Some records are not found in srs and inventory, number of not found records: 1")
        || errorLog.getReason().contains("Export file definition is not valid with id:"));
    }
  }

  private void validateExternalCalls() {
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.POST, ExternalPathResolver.SRS).size());
    assertNull(MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE));
    assertNull(MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.CONTENT_TERMS));
    assertNull(MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.IDENTIFIER_TYPES));
  }

  private void validateExternalCallsForInventory() {
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.POST, ExternalPathResolver.SRS).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE).size());
    validateExternalCallsForReferenceData();
  }

  /**
   * No calls to SRS to be made in case of custom profile
   */
  private void validateExternalCallsForMappingProfileTransformations() {
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE).size());
    assertNull(MockServer.getServerRqRsData(HttpMethod.POST, ExternalPathResolver.SRS));
    validateExternalCallsForReferenceData();
  }

  private void validateExternalCallsForReferenceData() {
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.CONTENT_TERMS).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.IDENTIFIER_TYPES).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.CONTRIBUTOR_NAME_TYPES).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.LOCATIONS).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.LIBRARIES).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.CAMPUSES).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTITUTIONS).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.MATERIAL_TYPES).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE_FORMATS).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE_TYPES).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.ELECTRONIC_ACCESS_RELATIONSHIPS).size());
  }

  @Configuration
  @Import(ApplicationConfig.class)
  public static class TestMock {

    @Bean
    @Primary
    public ExportStorageService getMockExportStorageService() {
      return mockExportStorageService;
    }

  }
}
