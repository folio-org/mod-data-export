package org.folio.rest.impl;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Context;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
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
import org.folio.service.export.storage.ExportStorageService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ExternalPathResolver;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.folio.rest.jaxrs.model.JobExecution.Status.SUCCESS;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;

@RunWith(VertxUnitRunner.class)
public class DataExportTest extends RestVerticleTestBase {

  private static final long TIMER_DELAY = 5000L;
  private static final String UUIDS = "uuids.csv";
  private static final String UUIDS_INVENTORY = "uuids_inventory.csv";
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
  public void testExport_UnderlyingSrsOnly(TestContext context) throws IOException {
    Async async = context.async();
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(UUIDS);
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition();
    //when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler -> {
      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), tenantId).onSuccess(optionalFileDefinition -> {
          FileDefinition fileExportDefinition = optionalFileDefinition.get();
          context.verify(v-> {
            assertSuccessJobExecution(jobExecution, EXPORTED_RECORDS_NUMBER_2, TOTAL_NUMBER_2);
            assertCompletedFileDefinitionAndExportedFile(fileExportDefinition);
            validateExternalCalls();
          });
          async.complete();
        });
      });
    });
  }


  @Test
  public void testExport_GenerateRecordsOnFly(TestContext context) throws IOException {
    Async async = context.async();
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(UUIDS_INVENTORY);
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition();
    //when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler -> {
      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), tenantId).onSuccess(optionalFileDefinition -> {
          context.verify(v -> {
            assertSuccessJobExecution(jobExecution, EXPORTED_RECORDS_NUMBER_1, TOTAL_NUMBER_1);
            validateExternalCallsForInventory();
          });
          async.complete();
        });
      });
    });
  }

  private ArgumentCaptor<FileDefinition> captureFileExportDefinition() {
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = ArgumentCaptor.forClass(FileDefinition.class);
    doNothing().when(mockExportStorageService).storeFile(fileExportDefinitionCaptor.capture(), eq(okapiConnectionParams.getTenantId()));
    return fileExportDefinitionCaptor;
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

  private ExportRequest buildExportRequest(FileDefinition uploadedFileDefinition) {
    return new ExportRequest()
      .withFileDefinitionId(uploadedFileDefinition.getId())
      .withJobProfileId(UUID.randomUUID().toString());
  }

  private void assertCompletedFileDefinitionAndExportedFile(FileDefinition fileExportDefinition) {
    String actualGeneratedFileContent = TestUtil.readFileContent(fileExportDefinition.getSourcePath());
    String expectedGeneratedFileContent = TestUtil.readFileContentFromResources(FILES_FOR_UPLOAD_DIRECTORY + "GeneratedFileForSrsRecordsOnly.mrc");
    Assert.assertEquals(expectedGeneratedFileContent, actualGeneratedFileContent);
    Assert.assertEquals(FileDefinition.Status.COMPLETED, fileExportDefinition.getStatus());
  }

  private void assertSuccessJobExecution(JobExecution jobExecution, Integer numberOfExportedRecords, String totalNumberOfRecords) {
    Assert.assertEquals(SUCCESS, jobExecution.getStatus());
    Assert.assertNotNull(jobExecution.getCompletedDate());
    Assert.assertEquals(numberOfExportedRecords, jobExecution.getProgress().getCurrent());
    Assert.assertEquals(totalNumberOfRecords, jobExecution.getProgress().getTotal());
  }

  private void validateExternalCalls() {
    Assert.assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.SRS).size());
    Assert.assertNull(MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE));
    Assert.assertNull(MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.CONTENT_TERMS));
  }

  private void validateExternalCallsForInventory() {
    Assert.assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.SRS).size());
    Assert.assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE).size());
    Assert.assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.CONTENT_TERMS).size());
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
