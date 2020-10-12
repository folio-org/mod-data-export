
package org.folio.service.logs;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.folio.dao.impl.ErrorLogDaoImpl;
import org.folio.rest.jaxrs.model.AffectedRecord;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.util.OkapiConnectionParams;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.HOLDINGS;
import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.ITEM;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class ErrorLogServiceUnitTest {
  private static final String QUERY = "query";
  private static final String TENANT_ID = "diku";
  private static final String JOB_EXECUTION_ID = "jobExecutionId";
  private static final String ERROR_REASON = "Error reason";
  private static final String INSTANCE_TITLE = "The Journal of ecclesiastical history";
  private static final String INSTANCE_HR_ID = "1";
  private static final String HOLDINGS_HR_ID = "2";
  private static final String ITEM_HR_ID = "3";
  private static final String INSTANCE_ID = "c8b50e3f-0446-429c-960e-03774b88223f";
  private static final String HOLDINGS_ID = "77d8456b-aec2-48ec-8deb-37c0a65983e6";
  private static final String ITEM_ID = "e2ecf553-3892-4205-aaff-6761a4d6ccfc";
  private static OkapiConnectionParams okapiConnectionParams;
  private static ErrorLog errorLog;
  private static ErrorLogCollection errorLogCollection;
  private static JsonObject instance;

  @Spy
  @InjectMocks
  private ErrorLogServiceImpl errorLogService;
  @Mock
  private ErrorLogDaoImpl errorLogDao;
  @Captor
  private ArgumentCaptor<ErrorLog> errorLogCaptor;

  @BeforeAll
  static void beforeEach() {
    instance = createRecord();
    errorLog = new ErrorLog()
      .withId(UUID.randomUUID().toString())
      .withJobExecutionId(UUID.randomUUID().toString())
      .withLogLevel(ErrorLog.LogLevel.ERROR)
      .withReason("Error reason")
      .withCreatedDate(new Date())
      .withMetadata(new Metadata()
        .withCreatedByUserId(UUID.randomUUID().toString())
        .withUpdatedByUserId(UUID.randomUUID().toString()));
    errorLogCollection = new ErrorLogCollection()
      .withErrorLogs(Lists.newArrayList(errorLog))
      .withTotalRecords(1);
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    okapiConnectionParams = new OkapiConnectionParams(headers);
  }

  @Test
  void getByJobExecutionId_shouldReturnFailedFuture_whenErrorLogDoesNotExist(VertxTestContext context) {
    // given
    when(errorLogDao.getByJobExecutionId(QUERY, 0, 0, TENANT_ID)).thenReturn(failedFuture("Error"));
    // when
    Future<ErrorLogCollection> future = errorLogService.getByJobExecutionId(QUERY, 0, 0, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.failed());
      context.completeNow();
    }));
  }

  @Test
  void getByJobExecutionId_shouldCallDaoGet(VertxTestContext context) {
    // given
    when(errorLogDao.getByJobExecutionId(QUERY, 5, 10, TENANT_ID)).thenReturn(succeededFuture(errorLogCollection));
    // when
    Future<ErrorLogCollection> future = errorLogService.getByJobExecutionId(QUERY, 5, 10, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      assertEquals(errorLogCollection, ar.result());
      context.completeNow();
    }));
  }

  @Test
  void save_shouldCallDaoSave_addUuidToTheErrorLog(VertxTestContext context) {
    // given
    errorLog.setId(null);
    when(errorLogDao.save(errorLog, TENANT_ID)).thenReturn(succeededFuture(errorLog));
    // when
    Future<ErrorLog> future = errorLogService.save(errorLog, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(errorLogDao).save(eq(errorLog), eq(TENANT_ID));
      Assert.assertNotNull(ar.result().getId());
      context.completeNow();
    }));
  }

  @Test
  void saveWithAffectedRecord_shouldSaveSuccessfully(VertxTestContext context) {
    // given
    JsonObject instanceRecord = new JsonObject()
      .put("hrid", "1")
      .put("id", "c8b50e3f-0446-429c-960e-03774b88223f")
      .put("title", INSTANCE_TITLE);
    JsonObject record = new JsonObject();
    record.put("instance", instanceRecord);
    when(errorLogDao.save(any(ErrorLog.class), eq(TENANT_ID))).thenReturn(succeededFuture(errorLog));
    // when
    Future<ErrorLog> future = errorLogService.saveWithAffectedRecord(record, ERROR_REASON, JOB_EXECUTION_ID, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(errorLogDao).save(errorLogCaptor.capture(), eq(TENANT_ID));
      ErrorLog errorLog = errorLogCaptor.getValue();
      Assert.assertEquals(ErrorLog.LogLevel.ERROR, errorLog.getLogLevel());
      Assert.assertEquals(ERROR_REASON, errorLog.getReason());
      Assert.assertEquals(JOB_EXECUTION_ID, errorLog.getJobExecutionId());
      Assert.assertEquals(AffectedRecord.RecordType.INSTANCE, errorLog.getAffectedRecord().getRecordType());
      Assert.assertEquals(INSTANCE_HR_ID, errorLog.getAffectedRecord().getHrid());
      Assert.assertEquals(INSTANCE_ID, errorLog.getAffectedRecord().getId());
      Assert.assertEquals(INSTANCE_TITLE, errorLog.getAffectedRecord().getTitle());
      Assert.assertTrue(errorLog.getAffectedRecord().getAffectedRecords().isEmpty());
      context.completeNow();
    }));
  }

  @Test
  void saveWithAffectedRecord_shouldSaveWithoutAffectedRecordInfo_whenRecordFieldsAreMissing(VertxTestContext context) {
    // given
    JsonObject instanceRecord = new JsonObject();
    when(errorLogDao.save(any(ErrorLog.class), eq(TENANT_ID))).thenReturn(succeededFuture(errorLog));
    // when
    Future<ErrorLog> future = errorLogService.saveWithAffectedRecord(instanceRecord, ERROR_REASON, JOB_EXECUTION_ID, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(errorLogDao).save(errorLogCaptor.capture(), eq(TENANT_ID));
      ErrorLog errorLog = errorLogCaptor.getValue();
      Assert.assertEquals(ErrorLog.LogLevel.ERROR, errorLog.getLogLevel());
      Assert.assertEquals(ERROR_REASON, errorLog.getReason());
      Assert.assertEquals(JOB_EXECUTION_ID, errorLog.getJobExecutionId());
      Assert.assertEquals(AffectedRecord.RecordType.INSTANCE, errorLog.getAffectedRecord().getRecordType());
      context.completeNow();
    }));
  }

  @Test
  void saveWithAffectedRecord_shouldSaveWithHoldingsAndItemsInfo(VertxTestContext context) {
    // given
    when(errorLogDao.save(any(ErrorLog.class), eq(TENANT_ID))).thenReturn(succeededFuture(errorLog));
    // when
    Future<ErrorLog> future = errorLogService.saveWithAffectedRecord(instance, ERROR_REASON, JOB_EXECUTION_ID, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(errorLogDao).save(errorLogCaptor.capture(), eq(TENANT_ID));
      ErrorLog errorLog = errorLogCaptor.getValue();
      assertErrorLogWithHoldingsAndItems(errorLog, true);
      context.completeNow();
    }));
  }

  @Test
  void saveWithAffectedRecord_shouldSaveWithHoldingsInfo(VertxTestContext context) {
    // given
    JsonObject instanceObject = new JsonObject();
    instanceObject.put("instance", instance.getJsonObject("instance"));
    instanceObject.put("holdings", instance.getJsonArray("holdings"));
    when(errorLogDao.save(any(ErrorLog.class), eq(TENANT_ID))).thenReturn(succeededFuture(errorLog));
    // when
    Future<ErrorLog> future = errorLogService.saveWithAffectedRecord(instanceObject, ERROR_REASON, JOB_EXECUTION_ID, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(errorLogDao).save(errorLogCaptor.capture(), eq(TENANT_ID));
      ErrorLog errorLog = errorLogCaptor.getValue();
      assertErrorLogWithHoldingsAndItems(errorLog, false);
      context.completeNow();
    }));
  }

  @Test
  void saveWithAffectedRecord_shouldSaveWithHoldingsInfoWithoutItemIfHoldingIdIsEmpty(VertxTestContext context) {
    // given
    JsonObject instanceObject = new JsonObject();
    instanceObject.put("instance", instance.getJsonObject("instance"));
    instanceObject.put("holdings", instance.getJsonArray("holdings"));
    instanceObject.put("items", instance.getJsonArray("items"));
    JsonArray items = instanceObject.getJsonArray("items");
    items.getJsonObject(0).put("holdingsRecordId", StringUtils.EMPTY);
    when(errorLogDao.save(any(ErrorLog.class), eq(TENANT_ID))).thenReturn(succeededFuture(errorLog));
    // when
    Future<ErrorLog> future = errorLogService.saveWithAffectedRecord(instanceObject, ERROR_REASON, JOB_EXECUTION_ID, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(errorLogDao).save(errorLogCaptor.capture(), eq(TENANT_ID));
      ErrorLog errorLog = errorLogCaptor.getValue();
      assertErrorLogWithHoldingsAndItems(errorLog, false);
      context.completeNow();
    }));
  }

  @Test
  void saveWithAffectedRecord_shouldSaveWithHoldingsInfoWithoutItemIfHoldingIdIsWrong(VertxTestContext context) {
    // given
    JsonObject instanceObject = new JsonObject();
    instanceObject.put("instance", instance.getJsonObject("instance"));
    instanceObject.put("holdings", instance.getJsonArray("holdings"));
    instanceObject.put("items", instance.getJsonArray("items"));
    JsonArray items = instanceObject.getJsonArray("items");
    items.getJsonObject(0).put("holdingsRecordId", UUID.randomUUID().toString());
    when(errorLogDao.save(any(ErrorLog.class), eq(TENANT_ID))).thenReturn(succeededFuture(errorLog));
    // when
    Future<ErrorLog> future = errorLogService.saveWithAffectedRecord(instanceObject, ERROR_REASON, JOB_EXECUTION_ID, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(errorLogDao).save(errorLogCaptor.capture(), eq(TENANT_ID));
      ErrorLog errorLog = errorLogCaptor.getValue();
      assertErrorLogWithHoldingsAndItems(errorLog, false);
      context.completeNow();
    }));
  }

  @Test
  void saveGeneralError_shouldSaveSuccessfully(VertxTestContext context) {
    // given
    when(errorLogDao.save(any(ErrorLog.class), eq(TENANT_ID))).thenReturn(succeededFuture(errorLog));
    // when
    Future<ErrorLog> future = errorLogService.saveGeneralError(ERROR_REASON, JOB_EXECUTION_ID, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(errorLogDao).save(errorLogCaptor.capture(), eq(TENANT_ID));
      ErrorLog errorLog = errorLogCaptor.getValue();
      Assert.assertEquals(ErrorLog.LogLevel.ERROR, errorLog.getLogLevel());
      Assert.assertEquals(ERROR_REASON, errorLog.getReason());
      Assert.assertEquals(JOB_EXECUTION_ID, errorLog.getJobExecutionId());
      context.completeNow();
    }));
  }

  @Test
  void delete_shouldCallDaoDelete(VertxTestContext context) {
    // given
    when(errorLogDao.deleteById(errorLog.getId(), TENANT_ID)).thenReturn(succeededFuture(true));
    // when
    Future<Boolean> future = errorLogService.deleteById(errorLog.getId(), TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(errorLogDao).deleteById(eq(errorLog.getId()), eq(TENANT_ID));
      context.completeNow();
    }));
  }

  @Test
  void update_shouldCallDaoUpdate(VertxTestContext context) {
    // given
    when(errorLogDao.update(errorLog, TENANT_ID)).thenReturn(succeededFuture(errorLog));
    // when
    Future<ErrorLog> future = errorLogService.update(errorLog, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(errorLogDao).update(eq(errorLog), eq(TENANT_ID));
      context.completeNow();
    }));
  }

  private void assertErrorLogWithHoldingsAndItems(ErrorLog errorLog, boolean isItemPresent) {
    Assert.assertEquals(ErrorLog.LogLevel.ERROR, errorLog.getLogLevel());
    Assert.assertEquals(ERROR_REASON, errorLog.getReason());
    Assert.assertEquals(JOB_EXECUTION_ID, errorLog.getJobExecutionId());
    AffectedRecord instanceAffectedRecord = errorLog.getAffectedRecord();
    Assert.assertEquals(AffectedRecord.RecordType.INSTANCE, instanceAffectedRecord.getRecordType());
    Assert.assertEquals(INSTANCE_HR_ID, instanceAffectedRecord.getHrid());
    Assert.assertEquals(INSTANCE_ID, instanceAffectedRecord.getId());
    Assert.assertEquals(INSTANCE_TITLE, instanceAffectedRecord.getTitle());
    AffectedRecord holdingAffectedRecord = errorLog.getAffectedRecord().getAffectedRecords().get(0);
    Assert.assertEquals(HOLDINGS_HR_ID, holdingAffectedRecord.getHrid());
    Assert.assertEquals(HOLDINGS_ID, holdingAffectedRecord.getId());
    Assert.assertEquals(HOLDINGS, holdingAffectedRecord.getRecordType());
    if (isItemPresent) {
      AffectedRecord itemAffectedRecord = holdingAffectedRecord.getAffectedRecords().get(0);
      Assert.assertEquals(ITEM_HR_ID, itemAffectedRecord.getHrid());
      Assert.assertEquals(ITEM_ID, itemAffectedRecord.getId());
      Assert.assertEquals(ITEM, itemAffectedRecord.getRecordType());
    } else {
      Assert.assertTrue(holdingAffectedRecord.getAffectedRecords().isEmpty());
    }
  }

  private static JsonObject createRecord() {
    instance = new JsonObject();
    JsonObject instanceRecord = new JsonObject()
      .put("hrid", INSTANCE_HR_ID)
      .put("id", INSTANCE_ID)
      .put("title", INSTANCE_TITLE);
    JsonObject holdingRecord = new JsonObject()
      .put("hrid", HOLDINGS_HR_ID)
      .put("id", HOLDINGS_ID)
      .put("instanceId", INSTANCE_ID);
    JsonObject itemRecord = new JsonObject()
      .put("hrid", ITEM_HR_ID)
      .put("id", ITEM_ID)
      .put("holdingsRecordId", HOLDINGS_ID);
    JsonArray holdings = new JsonArray().add(holdingRecord);
    JsonArray items = new JsonArray().add(itemRecord);
    instance.put("instance", instanceRecord);
    instance.put("holdings", holdings);
    instance.put("items", items);

    return instance;
  }

}
