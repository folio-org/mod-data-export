package org.folio.rest.impl;

import static org.folio.rest.jaxrs.model.JobExecution.Status.SUCCESS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Context;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.folio.TestUtil;
import org.folio.config.ApplicationConfig;
import org.folio.dao.FileDefinitionDao;
import org.folio.dao.JobExecutionDao;
import org.folio.rest.MockServer;
import org.folio.rest.RestVerticleTestBase;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ExternalPathResolver;
import org.junit.jupiter.api.Test;
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
class DataExportTest extends RestVerticleTestBase {

  private static final long TIMER_DELAY = 5000L;
  private static final String UUIDS = "uuids.csv";
  private static final String UUIDS_INVENTORY = "uuids_inventory.csv";
  private static final String MAPPING_PROFILE_SERVICE_URL = "/data-export/mappingProfiles";
  private static final String JOB_PROFILE_SERVICE_URL = "/data-export/jobProfiles";
  public static final int EXPORTED_RECORDS_NUMBER_2 = 2;
  public static final String TOTAL_NUMBER_2 = "2";
  public static final int EXPORTED_RECORDS_NUMBER_1 = 1;
  public static final String TOTAL_NUMBER_1 = "1";

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
  void testExport_UnderlyingSrsOnly(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(UUIDS);
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition();
    // when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, DEFAULT_JOB_PROFILE_ID);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
      JobExecution jobExecution = optionalJobExecution.get();
      fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), tenantId).onSuccess(optionalFileDefinition -> {
        context.verify(() -> {
          FileDefinition fileExportDefinition = optionalFileDefinition.get();
          assertSuccessJobExecution(jobExecution, EXPORTED_RECORDS_NUMBER_2, TOTAL_NUMBER_2);
          assertCompletedFileDefinitionAndExportedFile(fileExportDefinition);
          validateExternalCalls();
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
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition();
    // when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, DEFAULT_JOB_PROFILE_ID);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler -> {
      jobExecutionDao.getById(jobExecutionId, tenantId)
        .onSuccess(optionalJobExecution -> {
          JobExecution jobExecution = optionalJobExecution.get();
          fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue()
            .getId(), tenantId)
            .onSuccess(optionalFileDefinition -> {
              context.verify(() -> {
                assertSuccessJobExecution(jobExecution, EXPORTED_RECORDS_NUMBER_1, TOTAL_NUMBER_1);
                validateExternalCallsForInventory();
                context.completeNow();
              });
            });
        });
    });
  }

  @Test
  void testExport_GenerateRecordsOnFly_withMappingTransformations(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    MappingProfile mappingProfile = uploadMappingProfile();
    JobProfile jobProfile = uploadJobProfile(mappingProfile.getId());
    FileDefinition uploadedFileDefinition = uploadFile(UUIDS_INVENTORY);
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition();
    // when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, jobProfile.getId());
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler -> {
      jobExecutionDao.getById(jobExecutionId, tenantId)
        .onSuccess(optionalJobExecution -> {
          JobExecution jobExecution = optionalJobExecution.get();
          fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue()
            .getId(), tenantId)
            .onSuccess(optionalFileDefinition -> {
              context.verify(() -> {
                assertSuccessJobExecution(jobExecution, EXPORTED_RECORDS_NUMBER_1, TOTAL_NUMBER_1);
                validateExternalCallsForInventoryWithTransformations();
                context.completeNow();
              });
            });
        });
    });
  }

  private ArgumentCaptor<FileDefinition> captureFileExportDefinition() {
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = ArgumentCaptor.forClass(FileDefinition.class);
    doNothing().when(mockExportStorageService).storeFile(fileExportDefinitionCaptor.capture(), eq(okapiConnectionParams.getTenantId()));
    return fileExportDefinitionCaptor;
  }

  private MappingProfile uploadMappingProfile() throws IOException {
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withName("mappingProfile")
      .withTransformations(Arrays.asList(
        new Transformations()
          .withEnabled(true)
          .withFieldId("permanentLocationId")
          .withPath("$.holdings[*].permanentLocationId")
          .withTransformation("905  $a")
          .withRecordType(RecordType.HOLDINGS),
        new Transformations()
          .withEnabled(true)
          .withFieldId("temporaryLocationId")
          .withPath("$.holdings[*].temporaryLocationId")
          .withTransformation("906  $b")
          .withRecordType(RecordType.HOLDINGS)
      ))
      .withRecordTypes(Arrays.asList(RecordType.HOLDINGS))
      .withOutputFormat(MappingProfile.OutputFormat.MARC);

    RequestSpecification binaryRequestSpecification = buildRequestSpecification();

    return postRequest(JsonObject.mapFrom(mappingProfile), MAPPING_PROFILE_SERVICE_URL)
      .body()
      .as(MappingProfile.class);
  }

  private JobProfile uploadJobProfile(String mappingProfileId) throws IOException {
    JobProfile jobProfile = new JobProfile()
      .withName("jobProfile")
      .withDestination("fileSystem")
      .withMappingProfileId(mappingProfileId);

    RequestSpecification binaryRequestSpecification = buildRequestSpecification();

    return postRequest(JsonObject.mapFrom(jobProfile), JOB_PROFILE_SERVICE_URL)
      .body()
      .as(JobProfile.class);
  }

  private FileDefinition uploadFile(String fileName) throws IOException {
    File fileToUpload = TestUtil.getFileFromResources(FILES_FOR_UPLOAD_DIRECTORY + fileName);
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

  private ExportRequest buildExportRequest(FileDefinition uploadedFileDefinition, String jobProfileId) {
    return new ExportRequest()
      .withFileDefinitionId(uploadedFileDefinition.getId())
      .withJobProfileId(jobProfileId);
  }

  private void assertCompletedFileDefinitionAndExportedFile(FileDefinition fileExportDefinition) {
    String actualGeneratedFileContent = TestUtil.readFileContent(fileExportDefinition.getSourcePath());
    String expectedGeneratedFileContent = TestUtil.readFileContentFromResources(FILES_FOR_UPLOAD_DIRECTORY + "GeneratedFileForSrsRecordsOnly.mrc");
    assertEquals(expectedGeneratedFileContent, actualGeneratedFileContent);
    assertEquals(FileDefinition.Status.COMPLETED, fileExportDefinition.getStatus());
  }

  private void assertSuccessJobExecution(JobExecution jobExecution, Integer numberOfExportedRecords, String totalNumberOfRecords) {
    assertEquals(SUCCESS, jobExecution.getStatus());
    assertNotNull(jobExecution.getCompletedDate());
    assertEquals(numberOfExportedRecords, jobExecution.getProgress().getExported());
  }

  private void validateExternalCalls() {
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.SRS).size());
    assertNull(MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE));
    assertNull(MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.CONTENT_TERMS));
    assertNull(MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.IDENTIFIER_TYPES));
  }

  private void validateExternalCallsForInventory() {
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.SRS).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.CONTENT_TERMS).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.IDENTIFIER_TYPES).size());
  }

  private void validateExternalCallsForInventoryWithTransformations() {
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.SRS).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.CONTENT_TERMS).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.IDENTIFIER_TYPES).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.LOCATIONS).size());
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
