package org.folio.rest.impl;

import static org.folio.TestUtil.getFileFromResources;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.jaxrs.model.JobExecution.Status.COMPLETED;
import static org.folio.rest.jaxrs.model.JobExecution.Status.COMPLETED_WITH_ERRORS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Context;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.folio.TestUtil;
import org.folio.config.ApplicationConfig;
import org.folio.dao.FileDefinitionDao;
import org.folio.dao.JobExecutionDao;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ExternalPathResolver;
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

  public DataExportTest() {
    Context vertxContext = vertx.getOrCreateContext();
    SpringContextUtil.init(vertxContext.owner(), vertxContext, DataExportTest.TestMock.class);
    SpringContextUtil.autowireDependencies(this, vertxContext);
  }

  @Test
  void testExport_UnderlyingSrsOnly_COMPLETED_job(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(UUIDS_FOR_COMPLETED_JOB);
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
  void testExport_UnderlyingSrsOnly_COMPLETED_WITH_ERRORS_job(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(UUIDS_FOR_COMPLETED_WITH_ERRORS_JOB);
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
  void testExport_GenerateRecordsOnFly(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(UUIDS_INVENTORY);
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

  @Disabled("Disabled for Q3-2020(and until futher decision is made) as we are going to generate marc on the fly for custom profiles")
  @Test
  void testExport_UnderlyingSrsWithProfileTransformations(VertxTestContext context) throws IOException {
    postToTenant(CUSTOM_TENANT_HEADER);
    // given
    String tenantId = CUSTOM_TEST_TENANT;
    FileDefinition uploadedFileDefinition = uploadFile("uuids_forTransformation.csv", tenantId);
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
  void testExport_UnderlyingSrsWithProfileTransformationsNoCallToSRS(VertxTestContext context) throws IOException {
    postToTenant(CUSTOM_TENANT_HEADER);
    // given
    String tenantId = CUSTOM_TEST_TENANT;
    FileDefinition uploadedFileDefinition = uploadFile("uuids_forTransformation.csv", tenantId);
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

  private FileDefinition uploadFile(String fileName, String tenantId) throws IOException {
    File fileToUpload = TestUtil.getFileFromResources(FILES_FOR_UPLOAD_DIRECTORY + fileName);
    RequestSpecification binaryRequestSpecification = buildRequestSpecification(tenantId);

    FileDefinition givenFileDefinition = new FileDefinition()
      .withId(UUID.randomUUID().toString())
      .withFileName(fileName);

    postRequest(JsonObject.mapFrom(givenFileDefinition), FILE_DEFINITION_SERVICE_URL, tenantId);

    return RestAssured.given()
      .spec(binaryRequestSpecification)
      .when()
      .body(FileUtils.openInputStream(fileToUpload))
      .post(FILE_DEFINITION_SERVICE_URL + givenFileDefinition.getId() + UPLOAD_URL)
      .then()
      .extract().body().as(FileDefinition.class);
  }

  private FileDefinition uploadFile(String fileName) throws IOException {
    return uploadFile(fileName, okapiConnectionParams.getTenantId());
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
    String expectedMarcRecord = TestUtil.getExpectedMarcFromJson(expectedJsonRecords);
    assertEquals(expectedMarcRecord, actualGeneratedFileContent);
    assertEquals(FileDefinition.Status.COMPLETED, fileExportDefinition.getStatus());
  }

  private void assertJobExecution(JobExecution jobExecution, JobExecution.Status status, Integer numberOfExportedRecords) {
    assertEquals(status, jobExecution.getStatus());
    assertNotNull(jobExecution.getCompletedDate());
    assertEquals(numberOfExportedRecords, jobExecution.getProgress().getExported());
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

  private void validateExternalCallsForReferenceData(){
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
