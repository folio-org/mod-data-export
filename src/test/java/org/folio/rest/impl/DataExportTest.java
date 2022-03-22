package org.folio.rest.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
import org.folio.rest.jaxrs.model.QuickExportRequest;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.service.logs.ErrorLogService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ErrorCode;
import org.folio.util.ExternalPathResolver;
import org.folio.util.HelperUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
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
import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import static org.folio.TestUtil.DATA_EXPORT_JOB_PROFILES_ENDPOINT;
import static org.folio.TestUtil.DATA_EXPORT_MAPPING_PROFILES_ENDPOINT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.jaxrs.model.FileDefinition.UploadFormat.CQL;
import static org.folio.rest.jaxrs.model.FileDefinition.UploadFormat.CSV;
import static org.folio.rest.jaxrs.model.JobExecution.Status.COMPLETED;
import static org.folio.rest.jaxrs.model.JobExecution.Status.COMPLETED_WITH_ERRORS;
import static org.folio.rest.jaxrs.model.JobExecution.Status.FAIL;
import static org.folio.util.ErrorCode.INVALID_EXPORT_FILE_DEFINITION_ID;
import static org.folio.util.ErrorCode.NO_FILE_GENERATED;
import static org.folio.util.ErrorCode.SOME_UUIDS_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataExportTest extends RestVerticleTestBase {

  private static final long TIMER_DELAY = 5000L;
  private static final String INSTANCE_UUIDS_FOR_COMPLETED_JOB = "uuids_for_completed_job.csv";
  private static final String INSTANCE_UUIDS_FOR_COMPLETED_WITH_ERRORS_JOB = "uuids_for_completed_with_errors_job.csv";
  private static final String INSTANCE_UUIDS_INVENTORY = "instance_uuids_inventory.csv";
  private static final String HOLDING_UUIDS_INVENTORY = "holding_uuids_inventory.csv";
  private static final String AUTHORITY_UUIDS_INVENTORY = "authority_uuids_inventory.csv";
  private static final String HOLDING_UUID_GENERATE_ON_THE_FLY = "holding_uuid.csv";
  private static final String HOLDING_UUIDS_WITHOUT_SRS_RECORD = "holding_uuids_without_srs_record.csv";
  private static final String INSTANCE_UUIDS_INVENTORY_TWO_BATCHES = "InventoryUUIDsTwoBatches.csv";
  private static final String EMPTY_FILE = "InventoryUUIDsEmptyFile.csv";
  private static final String FILE_WHEN_INVENTORY_RETURNS_500 = "inventoryUUIDReturn500.csv";
  private static final String INSTANCE_UUIDS_CQL = "InventoryUUIDs.cql";
  private static final String INSTANCE_ID = "7fbd5d84-62d1-44c6-9c45-6cb173998bbd";
  private static final String JOB_EXECUTION_ID_FIELD = "jobExecutionId";
  private static final String JOB_EXECUTION_HR_ID_FIELD = "jobExecutionHrId";
  private static final String DEFAULT_HOLDING_JOB_PROFILE = "5e9835fc-0e51-44c8-8a47-f7b8fce35da7";
  private static final String DEFAULT_INSTANCE_JOB_PROFILE = "6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a";
  private static final String DEFAULT_AUTHORITY_JOB_PROFILE = "56944b1c-f3f9-475b-bed0-7387c33620ce";
  private static final int EXPORTED_RECORDS_EMPTY = 0;
  private static final int EXPORTED_RECORDS_NUMBER_1 = 1;
  private static final int EXPORTED_RECORDS_NUMBER_2 = 2;
  private static final int EXPORTED_RECORDS_NUMBER_3 = 3;
  private static final String CUSTOM_TEST_TENANT = "custom_test_tenant";
  private static final Header CUSTOM_TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, CUSTOM_TEST_TENANT);

  private static ExportStorageService mockExportStorageService = Mockito.mock(ExportStorageService.class);
  private static String srsJobProfileId;
  private static String customJobProfileId;
  private static String srsMappingProfileId;
  private static String customMappingProfileId;

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

  @BeforeAll
  public static void setup() throws MalformedURLException {
    // the tenant API is now async, so creating the custom tenant prior to running
    postToTenant(CUSTOM_TENANT_HEADER).statusCode(201);
  }


  @Test
  @Order(1)
  void testExport_uploadingCqlEmptyFile_FAILED_job(VertxTestContext context) throws IOException, InterruptedException {
    //given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(EMPTY_FILE, CQL, buildRequestSpecification(tenantId));
    // when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, ExportRequest.IdType.INSTANCE);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    context.awaitCompletion(5, TimeUnit.SECONDS);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(7000L, handler ->
      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        context.verify(() -> {
          assertJobExecution(jobExecution, FAIL, EXPORTED_RECORDS_EMPTY);
          context.completeNow();
        });
      }));
  }

  @Test
  @Order(2)
  void testExportByCSV_UnderlyingSrsOnlyWithProfileTransformations_COMPLETED_job(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(INSTANCE_UUIDS_FOR_COMPLETED_JOB, CSV, buildRequestSpecification(tenantId));
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    buildSrsJobProfile(okapiConnectionParams.getTenantId());
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, srsJobProfileId, ExportRequest.IdType.INSTANCE);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), tenantId).onSuccess(optionalFileDefinition -> {
          errorLogService.isErrorsByErrorCodePresent(ErrorCode.errorCodesAccordingToExport(), jobExecutionId, tenantId).onSuccess(isErrorsPresent -> {
            context.verify(() -> {
              FileDefinition fileExportDefinition = optionalFileDefinition.get();
              assertJobExecution(jobExecution, COMPLETED, EXPORTED_RECORDS_NUMBER_2);
              assertFalse(isErrorsPresent);
              assertCompletedFileDefinitionAndExportedFile(fileExportDefinition, "GeneratedFileForSrsRecordsOnly.mrc");
              validateExternalCallsForSrs();
              context.completeNow();
            });
          });
        });
      }));
  }

  @Test
  @Order(3)
  void testExportByCSV_UnderlyingSrsOnlyWithProfileTransformations_COMPLETED_WITH_ERRORS_job(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(INSTANCE_UUIDS_FOR_COMPLETED_WITH_ERRORS_JOB, CSV, buildRequestSpecification(tenantId));
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    buildSrsJobProfile(okapiConnectionParams.getTenantId());
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, srsJobProfileId, ExportRequest.IdType.INSTANCE);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), tenantId).onSuccess(optionalFileDefinition -> {
          errorLogService.getByQuery(HelperUtils.getErrorLogCriterionByJobExecutionIdAndErrorMessageCode(jobExecutionId, SOME_UUIDS_NOT_FOUND.getCode()), tenantId)
            .onComplete(ar -> {
              context.verify(() -> {
                assertJobExecution(jobExecution, COMPLETED_WITH_ERRORS, EXPORTED_RECORDS_NUMBER_3);
                validateExternalCallsForSrsAndInventory(1);
                assertTrue(ar.succeeded());
                List<ErrorLog> errorLogList = ar.result();
                assertEquals(1, errorLogList.size());
                assertNotFoundUUIDsErrorLog(errorLogList.get(0), jobExecutionId);
              });
              context.completeNow();
            });
        });
      }));
  }

  @Test
  @Order(4)
  void testExportByCSV_UnderlyingSrsOnlyWithProfileTransformations_COMPLETED_WITH_ERRORS_With2Batches(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(INSTANCE_UUIDS_INVENTORY_TWO_BATCHES, CSV, buildRequestSpecification(tenantId));
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    buildSrsJobProfile(okapiConnectionParams.getTenantId());
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, srsJobProfileId, ExportRequest.IdType.INSTANCE);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), tenantId).onSuccess(optionalFileDefinition -> {
          context.verify(() -> {
            assertJobExecution(jobExecution, COMPLETED_WITH_ERRORS, EXPORTED_RECORDS_NUMBER_3);
            validateExternalCallsForSrsAndInventory(2);
            context.completeNow();
          });
        });
      }));
  }

  @Test
  @Order(5)
  void testExportByCSV_GenerateRecordsOnFly_whenSrsMarcRecordsEmpty(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(INSTANCE_UUIDS_INVENTORY, CSV, buildRequestSpecification(tenantId));
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    buildSrsJobProfile(okapiConnectionParams.getTenantId());
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, srsJobProfileId, ExportRequest.IdType.INSTANCE);
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
              validateExternalCallsForSrsAndInventory(1);
              assertCompletedFileDefinitionAndExportedFile(optionalFileDefinition.get(), "GeneratedRecordsByDefaultRulesAndTransformations.mrc");
              context.completeNow();
            });
          });
        });
    });
  }

  @Test
  @Order(6)
  void testExportByCQL_GenerateRecordsOnFly_andUnderlyingSrs(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    RequestSpecification requestSpecificationForMockServer = new RequestSpecBuilder()
      .setContentType(ContentType.BINARY)
      .addHeader(OKAPI_HEADER_TENANT, tenantId)
      .addHeader(OKAPI_HEADER_URL, MOCK_OKAPI_URL)
      .setBaseUri(BASE_OKAPI_URL)
      .build();
    FileDefinition uploadedFileDefinition = uploadFile(INSTANCE_UUIDS_CQL, CQL, requestSpecificationForMockServer);
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    buildSrsJobProfile(okapiConnectionParams.getTenantId());
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, srsJobProfileId, ExportRequest.IdType.INSTANCE);
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
              validateExternalCallsForSrsAndInventory(1);
              context.completeNow();
            });
          });
        });
    });
  }

  @Test
  @Order(7)
  void testExportByCSV_UnderlyingSrsWithProfileTransformationsNoCallToSRS(VertxTestContext context) throws IOException {
    // given
    String tenantId = CUSTOM_TEST_TENANT;
    FileDefinition uploadedFileDefinition = uploadFile("uuids_forTransformation.csv", CSV, tenantId, buildRequestSpecification(tenantId));
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    buildCustomJobProfile(CUSTOM_TEST_TENANT);
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, customJobProfileId, ExportRequest.IdType.INSTANCE);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL, tenantId);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        fileDefinitionDao.getById(fileExportDefinitionCaptor.getValue().getId(), tenantId).onSuccess(optionalFileDefinition -> {
          context.verify(() -> {
            assertJobExecution(jobExecution, COMPLETED, EXPORTED_RECORDS_NUMBER_1);
            validateExternalCallsForInventoryAndReferenceData();
            context.completeNow();
          });
        });
      }));
  }

  @Test
  @Order(8)
  void shouldNotExportFile_whenInventoryReturnServerError(VertxTestContext context) throws IOException, InterruptedException {
    // given
    String tenantId = CUSTOM_TEST_TENANT;
    //given
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    buildCustomJobProfile(CUSTOM_TEST_TENANT);
    FileDefinition uploadedFileDefinition = uploadFile(FILE_WHEN_INVENTORY_RETURNS_500, CSV, CUSTOM_TEST_TENANT, buildRequestSpecification(tenantId));
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, customJobProfileId, ExportRequest.IdType.INSTANCE);

    //when
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL, tenantId);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();

    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(jobExecutionId, tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        errorLogService.get("jobExecutionId=" + jobExecutionId, 0, 20, tenantId).onSuccess(errorLogCollection -> {
          context.verify(() -> {
            assertJobExecution(jobExecution, FAIL, 0);
            assertErrorLogs(errorLogCollection, jobExecutionId);
            context.completeNow();
          });
        });
      }));
  }

  @Test
  @Order(9)
  void testQuickExport_uploadingCqlType_COMPLETED_WITH_ERRORS_job(VertxTestContext context) {
    //given
    String tenantId = okapiConnectionParams.getTenantId();
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    QuickExportRequest exportRequest = buildQuickCqlExportRequest("(languages=\"uk\")");
    JsonObject response = new JsonObject(postRequest(JsonObject.mapFrom(exportRequest), QUICK_EXPORT_URL)
      .body().prettyPrint());
    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(fileExportDefinitionCaptor.getValue().getJobExecutionId(), tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        context.verify(() -> {
          assertEquals(jobExecution.getId(), response.getString(JOB_EXECUTION_ID_FIELD));
          assertEquals(jobExecution.getHrId(), response.getInteger(JOB_EXECUTION_HR_ID_FIELD));
          assertJobExecution(jobExecution, COMPLETED_WITH_ERRORS, EXPORTED_RECORDS_NUMBER_1);
          context.completeNow();
        });
      }));
  }

  @Test
  @Order(10)
  void testQuickExport_uploadingCqlType_COMPLETED_job(VertxTestContext context) {
    //given
    String tenantId = okapiConnectionParams.getTenantId();
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    QuickExportRequest exportRequest = buildQuickCqlExportRequest("(languages=\"eng\")");
    JsonObject response = new JsonObject(postRequest(JsonObject.mapFrom(exportRequest), QUICK_EXPORT_URL)
      .body().prettyPrint());
    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(fileExportDefinitionCaptor.getValue().getJobExecutionId(), tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        context.verify(() -> {
          assertJobExecution(jobExecution, COMPLETED, EXPORTED_RECORDS_NUMBER_1);
          assertEquals(jobExecution.getId(), response.getString(JOB_EXECUTION_ID_FIELD));
          assertEquals(jobExecution.getHrId(), response.getInteger(JOB_EXECUTION_HR_ID_FIELD));
          context.completeNow();
        });
      }));
  }

  @Test
  @Order(11)
  void testQuickExport_uploadingUuidType_COMPLETED_job(VertxTestContext context) {
    //given
    String tenantId = okapiConnectionParams.getTenantId();
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    QuickExportRequest exportRequest = buildQuickExportRequest(Collections.singletonList(INSTANCE_ID));
    JsonObject response = new JsonObject(postRequest(JsonObject.mapFrom(exportRequest), QUICK_EXPORT_URL)
      .body().prettyPrint());
    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(fileExportDefinitionCaptor.getValue().getJobExecutionId(), tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        context.verify(() -> {
          assertJobExecution(jobExecution, COMPLETED, EXPORTED_RECORDS_NUMBER_1);
          assertEquals(jobExecution.getId(), response.getString(JOB_EXECUTION_ID_FIELD));
          assertEquals(jobExecution.getHrId(), response.getInteger(JOB_EXECUTION_HR_ID_FIELD));
          context.completeNow();
        });
      }));
  }

  @Test
  @Order(12)
  void testQuickExport_uploadingUuidType_COMPLETED_WITH_ERRORS_job(VertxTestContext context) {
    //given
    String tenantId = okapiConnectionParams.getTenantId();
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    List<String> uuids = new ArrayList<>();
    uuids.add(INSTANCE_ID);
    uuids.add(UUID.randomUUID().toString());
    // when
    QuickExportRequest exportRequest = buildQuickExportRequest(uuids);
    JsonObject response = new JsonObject(postRequest(JsonObject.mapFrom(exportRequest), QUICK_EXPORT_URL)
      .body().prettyPrint());
    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(fileExportDefinitionCaptor.getValue().getJobExecutionId(), tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        context.verify(() -> {
          assertJobExecution(jobExecution, COMPLETED_WITH_ERRORS, EXPORTED_RECORDS_NUMBER_1);
          assertEquals(jobExecution.getId(), response.getString(JOB_EXECUTION_ID_FIELD));
          assertEquals(jobExecution.getHrId(), response.getInteger(JOB_EXECUTION_HR_ID_FIELD));
          context.completeNow();
        });
      }));
  }

  @Test
  @Order(13)
  void testQuickExport_uploadingUuidType_customFileName(VertxTestContext context) {
    //given
    String tenantId = okapiConnectionParams.getTenantId();
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    List<String> uuids = new ArrayList<>();
    uuids.add(INSTANCE_ID);
    uuids.add(UUID.randomUUID().toString());
    // when
    QuickExportRequest exportRequest = buildQuickExportRequest(uuids);
    exportRequest.setFileName("testName");
    JsonObject response = new JsonObject(postRequest(JsonObject.mapFrom(exportRequest), QUICK_EXPORT_URL)
      .body().prettyPrint());
    // then
    vertx.setTimer(TIMER_DELAY, handler ->
      jobExecutionDao.getById(fileExportDefinitionCaptor.getValue().getJobExecutionId(), tenantId).onSuccess(optionalJobExecution -> {
        JobExecution jobExecution = optionalJobExecution.get();
        context.verify(() -> {
          assertEquals(jobExecution.getId(), response.getString(JOB_EXECUTION_ID_FIELD));
          assertEquals(jobExecution.getHrId(), response.getInteger(JOB_EXECUTION_HR_ID_FIELD));
          assertJobExecution(jobExecution, COMPLETED_WITH_ERRORS, EXPORTED_RECORDS_NUMBER_1);
          assertEquals("testName-" + jobExecution.getHrId() + ".mrc", fileExportDefinitionCaptor.getValue().getFileName());
          context.completeNow();
        });
      }));
  }

  @Test
  @Order(14)
  void testHoldingsExportByCSV(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(HOLDING_UUIDS_INVENTORY, CSV, buildRequestSpecification(tenantId));
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, DEFAULT_HOLDING_JOB_PROFILE, ExportRequest.IdType.HOLDING);
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
              validateExternalCallsForSrs(1);
              assertCompletedFileDefinitionAndExportedFile(optionalFileDefinition.get(), "GeneratedRecordsFromHoldingRecord.mrc");
              context.completeNow();
            });
          });
        });
    });
  }

  @Test
  @Order(15)
  void testHoldingsExportByCSV_NoBinaryFileGenerated(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(HOLDING_UUIDS_WITHOUT_SRS_RECORD, CSV, buildRequestSpecification(tenantId));
    // when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, DEFAULT_HOLDING_JOB_PROFILE, ExportRequest.IdType.HOLDING);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler -> {
      jobExecutionDao.getById(jobExecutionId, tenantId)
        .onSuccess(optionalJobExecution -> {
          JobExecution jobExecution = optionalJobExecution.get();
          errorLogService.get("jobExecutionId=" + jobExecutionId, 0, 20, tenantId).onSuccess(errorLogs -> {
            context.verify(() -> {
              assertJobExecution(jobExecution, FAIL, EXPORTED_RECORDS_EMPTY);
              validateExternalCallsForSrs(1);
              ErrorLog errorLog = errorLogs.getErrorLogs().get(1);
              assertEquals(NO_FILE_GENERATED.getCode(), errorLog.getErrorMessageCode());
              context.completeNow();
            });
          });
        });
    });
  }

  @Test
  @Order(16)
  void testHoldingsExportByCSV_whenNotDefaultHoldingJobProfileSpecified(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(HOLDING_UUIDS_INVENTORY, CSV, buildRequestSpecification(tenantId));

    // when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, DEFAULT_INSTANCE_JOB_PROFILE, ExportRequest.IdType.HOLDING);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler -> {
      jobExecutionDao.getById(jobExecutionId, tenantId)
        .onSuccess(optionalJobExecution -> {
          JobExecution jobExecution = optionalJobExecution.get();
          errorLogService.get("jobExecutionId=" + jobExecutionId, 0, 20, tenantId).onSuccess(errorLogs -> {
            context.verify(() -> {
              assertEquals(FAIL, jobExecution.getStatus());
              assertNotNull(jobExecution.getCompletedDate());
              assertNotNull(jobExecution.getRunBy());
              assertEquals(1, errorLogs.getErrorLogs().size());
              ErrorLog errorLog = errorLogs.getErrorLogs().get(0);
              assertEquals(ErrorCode.ERROR_ONLY_DEFAULT_HOLDING_JOB_PROFILE_IS_SUPPORTED.getCode(), errorLog.getErrorMessageCode());
              assertEquals(ErrorCode.ERROR_ONLY_DEFAULT_HOLDING_JOB_PROFILE_IS_SUPPORTED.getDescription(), errorLog.getErrorMessageValues().get(0));
              context.completeNow();
            });
          });
        });
    });
  }

  @Test
  @Order(17)
  void testHoldingsExportByCSV_whenCqlIdTypeSpecifiedForFileDefinition(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(HOLDING_UUIDS_INVENTORY, CQL, buildRequestSpecification(tenantId));

    // when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, DEFAULT_HOLDING_JOB_PROFILE, ExportRequest.IdType.HOLDING);
    postRequest(JsonObject.mapFrom(exportRequest), EXPORT_URL);
    String jobExecutionId = uploadedFileDefinition.getJobExecutionId();
    // then
    vertx.setTimer(TIMER_DELAY, handler -> {
      jobExecutionDao.getById(jobExecutionId, tenantId)
        .onSuccess(optionalJobExecution -> {
          JobExecution jobExecution = optionalJobExecution.get();
          errorLogService.get("jobExecutionId=" + jobExecutionId, 0, 20, tenantId).onSuccess(errorLogs -> {
            context.verify(() -> {
              assertEquals(FAIL, jobExecution.getStatus());
              assertNotNull(jobExecution.getCompletedDate());
              assertNotNull(jobExecution.getRunBy());
              assertEquals(1, errorLogs.getErrorLogs().size());
              ErrorLog errorLog = errorLogs.getErrorLogs().get(0);
              assertEquals(ErrorCode.INVALID_UPLOADED_FILE_EXTENSION_FOR_HOLDING_ID_TYPE.getCode(), errorLog.getErrorMessageCode());
              assertEquals(ErrorCode.INVALID_UPLOADED_FILE_EXTENSION_FOR_HOLDING_ID_TYPE.getDescription(), errorLog.getErrorMessageValues().get(0));
              context.completeNow();
            });
          });
        });
    });
  }

  @Test
  @Order(18)
  void testHoldingsExportByCSV_shouldGenerateRecordsOnTheFly(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(HOLDING_UUID_GENERATE_ON_THE_FLY, CSV, buildRequestSpecification(tenantId));

    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, DEFAULT_HOLDING_JOB_PROFILE, ExportRequest.IdType.HOLDING);
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
              assertCompletedFileDefinitionAndExportedFile(optionalFileDefinition.get(), "generatedOnTheFlyRecordFromHolding.mrc");
              context.completeNow();
            });
          });
        });
    });
  }

  @Test
  @Order(19)
  void testExportByCSV_whenFileIsTooLarge(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    // when
    Response response = uploadFile(INSTANCE_UUIDS_INVENTORY, CSV, buildRequestSpecification(tenantId), 500_001);
    // then
    vertx.setTimer(TIMER_DELAY, handler -> {
      context.verify(() -> {
        assertEquals(413, response.getStatusCode());
        context.completeNow();
      });
    });
  }
  
  @Test
  @Order(14)
  void testAuthorityExportByCSV(VertxTestContext context) throws IOException {
    // given
    String tenantId = okapiConnectionParams.getTenantId();
    FileDefinition uploadedFileDefinition = uploadFile(AUTHORITY_UUIDS_INVENTORY, CSV, buildRequestSpecification(tenantId));
    ArgumentCaptor<FileDefinition> fileExportDefinitionCaptor = captureFileExportDefinition(tenantId);
    // when
    ExportRequest exportRequest = buildExportRequest(uploadedFileDefinition, DEFAULT_AUTHORITY_JOB_PROFILE, ExportRequest.IdType.AUTHORITY);
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
              validateExternalCallsForSrs(1);
              assertCompletedFileDefinitionAndExportedFile(optionalFileDefinition.get(),
                "GeneratedRecordsFromAuthorityRecord.mrc");
              context.completeNow();
            });
          });
        });
    });
  }

  private void buildCustomJobProfile(String tenantID) {
    if (customMappingProfileId == null) {
      String mappingProfile = TestUtil.readFileContentFromResources(FILES_FOR_UPLOAD_DIRECTORY + "mappingProfile.json");
      JsonObject mappingProfilejs = new JsonObject(mappingProfile);
      customMappingProfileId = postRequest(mappingProfilejs, DATA_EXPORT_MAPPING_PROFILES_ENDPOINT, tenantID)
        .then()
        .extract()
        .path("id");
    }
    if (customJobProfileId == null) {
      String jobProfile = TestUtil.readFileContentFromResources(FILES_FOR_UPLOAD_DIRECTORY + "jobProfile.json");
      JsonObject jobProfilejs = new JsonObject(jobProfile);
      customJobProfileId = postRequest(jobProfilejs, DATA_EXPORT_JOB_PROFILES_ENDPOINT, tenantID)
        .then()
        .extract()
        .path("id");
    }
  }

  private void buildSrsJobProfile(String tenantID) {
    if (srsMappingProfileId == null) {
      String srsMappingProfile = TestUtil.readFileContentFromResources(FILES_FOR_UPLOAD_DIRECTORY + "srsMappingProfile.json");
      JsonObject srsMappingProfileJson = new JsonObject(srsMappingProfile);
      srsMappingProfileId = postRequest(srsMappingProfileJson, DATA_EXPORT_MAPPING_PROFILES_ENDPOINT, tenantID)
        .then()
        .extract()
        .path("id");
    }
    if (srsJobProfileId == null) {
      String srsJobProfile = TestUtil.readFileContentFromResources(FILES_FOR_UPLOAD_DIRECTORY + "srsJobProfile.json");
      JsonObject srsJobProfileJson = new JsonObject(srsJobProfile);
      Response response = postRequest(srsJobProfileJson, DATA_EXPORT_JOB_PROFILES_ENDPOINT, tenantID);
      srsJobProfileId = response.then()
        .extract()
        .path("id");
    }
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

  private Response uploadFile(String fileName, UploadFormat format, String tenantId, RequestSpecification binaryRequestSpecification, Integer size) throws IOException {

    FileDefinition givenFileDefinition = new FileDefinition()
      .withId(UUID.randomUUID().toString())
      .withFileName(fileName)
      .withUploadFormat(format)
      .withSize(size)
      .withStatus(FileDefinition.Status.NEW);

    return postRequest(JsonObject.mapFrom(givenFileDefinition), FILE_DEFINITION_SERVICE_URL, tenantId);
  }

  private FileDefinition uploadFile(String fileName, UploadFormat format, RequestSpecification binaryRequestSpecification) throws IOException {
    return uploadFile(fileName, format, okapiConnectionParams.getTenantId(), binaryRequestSpecification);
  }

  private Response uploadFile(String fileName, UploadFormat format, RequestSpecification binaryRequestSpecification, Integer size) throws IOException {
    return uploadFile(fileName, format, okapiConnectionParams.getTenantId(), binaryRequestSpecification, size);
  }

  private ExportRequest buildExportRequest(FileDefinition uploadedFileDefinition, ExportRequest.IdType idType) {
    return new ExportRequest()
      .withFileDefinitionId(uploadedFileDefinition.getId())
      .withJobProfileId(DEFAULT_JOB_PROFILE_ID)
      .withIdType(idType);
  }

  private ExportRequest buildExportRequest(FileDefinition uploadedFileDefinition, String jobProfileID, ExportRequest.IdType idType) {
    return new ExportRequest()
      .withFileDefinitionId(uploadedFileDefinition.getId())
      .withJobProfileId(jobProfileID)
      .withIdType(idType);
  }

  private QuickExportRequest buildQuickCqlExportRequest(String criteria) {
    return new QuickExportRequest()
      .withCriteria(criteria)
      .withRecordType(QuickExportRequest.RecordType.INSTANCE)
      .withType(QuickExportRequest.Type.CQL);
  }

  private QuickExportRequest buildQuickExportRequest(List<String> uuids) {
    return new QuickExportRequest()
      .withType(QuickExportRequest.Type.UUID)
      .withUuids(uuids)
      .withRecordType(QuickExportRequest.RecordType.INSTANCE);
  }

  private void assertCompletedFileDefinitionAndExportedFile(FileDefinition fileExportDefinition, String fileName) {
    String actualGeneratedFileContent = TestUtil.readFileContent(fileExportDefinition.getSourcePath()).replaceAll("\n","");
    String expectedGeneratedFileContent = TestUtil.readFileContentFromResources(FILES_FOR_UPLOAD_DIRECTORY + fileName).replaceAll("\n","");
    assertEquals(expectedGeneratedFileContent, actualGeneratedFileContent);
    assertEquals(FileDefinition.Status.COMPLETED, fileExportDefinition.getStatus());
  }

  private void assertJobExecution(JobExecution jobExecution, JobExecution.Status status, Integer numberOfExportedRecords) {
    assertEquals(status, jobExecution.getStatus());
    assertNotNull(jobExecution.getCompletedDate());
    assertEquals(numberOfExportedRecords, jobExecution.getProgress().getExported());
    assertNotNull(jobExecution.getExportedFiles().iterator().next().getFileName());
    assertNotNull(jobExecution.getRunBy());
  }

  private void assertErrorLogs(ErrorLogCollection errorLogCollection, String jobExecutionId) {
    assertEquals(4, errorLogCollection.getErrorLogs().size());
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
     assertTrue(errorLog.getErrorMessageCode().contains(ErrorCode.ERROR_GETTING_INSTANCES_BY_IDS.getCode())
        || errorLog.getErrorMessageCode().contains(NO_FILE_GENERATED.getCode())
        || errorLog.getErrorMessageCode().contains(SOME_UUIDS_NOT_FOUND.getCode())
        || errorLog.getErrorMessageCode().contains(INVALID_EXPORT_FILE_DEFINITION_ID.getCode()));
    }
  }

  private void assertNotFoundUUIDsErrorLog(ErrorLog errorLog, String jobExecutionId) {
    Assertions.assertEquals(jobExecutionId, errorLog.getJobExecutionId());
    Assertions.assertEquals(ErrorLog.LogLevel.ERROR, errorLog.getLogLevel());
    Assertions.assertTrue(errorLog.getErrorMessageCode().contains(SOME_UUIDS_NOT_FOUND.getCode()));
  }

  private void validateExternalCallsForSrs() {
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.POST, ExternalPathResolver.SRS).size());
    assertNull(MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE));
  }

  private void validateExternalCallsForSrsAndInventory(int expectedNumber) {
    assertEquals(expectedNumber, MockServer.getServerRqRsData(HttpMethod.POST, ExternalPathResolver.SRS).size());
    assertEquals(expectedNumber, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE).size());
  }

  private void validateExternalCallsForSrs(int expectedNumber) {
    assertEquals(expectedNumber, MockServer.getServerRqRsData(HttpMethod.POST, ExternalPathResolver.SRS).size());
  }

  /**
   * No calls to SRS to be made in case of custom profile
   */
  private void validateExternalCallsForInventoryAndReferenceData() {
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE).size());
    assertNull(MockServer.getServerRqRsData(HttpMethod.POST, ExternalPathResolver.SRS));
    validateExternalCallsForReferenceData();
  }

  private void validateExternalCallsForReferenceData() {
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.CONTENT_TERMS).size());
    assertEquals(3, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.IDENTIFIER_TYPES).size());
    assertEquals(3, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.CONTRIBUTOR_NAME_TYPES).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.LOCATIONS).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.LIBRARIES).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.CAMPUSES).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTITUTIONS).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.MATERIAL_TYPES).size());
    assertEquals(1, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE_FORMATS).size());
    assertEquals(3, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.INSTANCE_TYPES).size());
    assertEquals(3, MockServer.getServerRqRsData(HttpMethod.GET, ExternalPathResolver.ELECTRONIC_ACCESS_RELATIONSHIPS).size());
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
