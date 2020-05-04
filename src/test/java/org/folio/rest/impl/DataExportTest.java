package org.folio.rest.impl;

import static org.folio.rest.jaxrs.model.JobExecution.Status.SUCCESS;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@RunWith(VertxUnitRunner.class)
public class DataExportTest  extends RestVerticleTestBase{

  private static final long TIMER_DELAY = 5000L;
  private static final String UUIDS = "uuids.csv";

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
  public void shouldNotCallInventory_whenUploadFileHasOnlyUnderlyingSRS(TestContext context) throws IOException, InterruptedException {
    Async async = context.async();

    //given
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = givenCaptureFileExportDefinition();
    FileDefinition uploadedFileDefinition = givenUploadFile(UUIDS);

    //when
    ExportRequest exportRequest = getExportRequest(uploadedFileDefinition);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);

    // then
    vertx.setTimer(TIMER_DELAY, handler -> fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), okapiConnectionParams.getTenantId())
      .compose(fileExportDefinitionOptional -> assertCompletedFileDefinitionAndExportedFile(context, fileExportDefinitionOptional))
      .compose(fileExportDefinition -> jobExecutionDao.getById(fileExportDefinition.getJobExecutionId(), okapiConnectionParams.getTenantId())
        .compose(jobExecutionOptional -> assertSuccessJobExecution(context, fileExportDefinition, jobExecutionOptional, 2))
        .compose(optional -> validateExternalCalls(context))
        .onComplete(succeeded -> async.complete())
      ));
  }

  private Future<FileDefinition> validateExternalCalls(TestContext context) {
    context.assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.SRS).size());
    context.assertNull(MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE));
    context.assertNull(MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.CONTENT_TERMS));
    return Future.succeededFuture();
  }

  private ArgumentCaptor<FileDefinition> givenCaptureFileExportDefinition() {
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = ArgumentCaptor.forClass(FileDefinition.class);
    doNothing().when(mockExportStorageService).storeFile(fileExportDefinitionCaptor.capture(), eq(okapiConnectionParams.getTenantId()));
    return fileExportDefinitionCaptor;
  }

  private ExportRequest getExportRequest(FileDefinition uploadedFileDefinition) {
    return new ExportRequest()
      .withFileDefinitionId(uploadedFileDefinition.getId())
      .withJobProfileId(UUID.randomUUID().toString());
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

  private Future<FileDefinition> assertCompletedFileDefinitionAndExportedFile(TestContext context, Optional<FileDefinition> fileExportDefinitionOptional) {
    FileDefinition fileExportDefinition = fileExportDefinitionOptional.get();
    context.assertEquals(fileExportDefinition.getStatus(), FileDefinition.Status.COMPLETED);
    return Future.succeededFuture(fileExportDefinition);
  }

  private Future<Object> assertSuccessJobExecution(TestContext context, FileDefinition fileDefinition,  Optional<JobExecution> jobExecutionOptional, Integer currentNumber) {
    JobExecution jobExecution = jobExecutionOptional.get();
    context.assertEquals(jobExecution.getStatus(), SUCCESS);
    context.assertNotNull(jobExecution.getCompletedDate());
    context.assertEquals(jobExecution.getProgress().getCurrent(), currentNumber);
    return Future.succeededFuture();
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
