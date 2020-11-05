
package org.folio.service.logs;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.collections4.map.HashedMap;
import org.assertj.core.util.Lists;
import org.folio.dao.impl.ErrorLogDaoImpl;
import org.folio.rest.jaxrs.model.AffectedRecord;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.persist.Criteria.Criterion;
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
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.HOLDINGS;
import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.ITEM;
import static org.folio.rest.jaxrs.model.ErrorLog.LogLevel.ERROR;
import static org.folio.util.ErrorCode.SOME_RECORDS_FAILED;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
  private static final String ITEMS = "items";
  private static ErrorLog errorLog;
  private static ErrorLogCollection errorLogCollection;

  @Spy
  @InjectMocks
  private ErrorLogServiceImpl errorLogService;
  @Mock
  private ErrorLogDaoImpl errorLogDao;
  @Captor
  private ArgumentCaptor<ErrorLog> errorLogCaptor;

  @BeforeAll
  static void beforeEach() {
    errorLog = new ErrorLog()
      .withId(UUID.randomUUID().toString())
      .withJobExecutionId(UUID.randomUUID().toString())
      .withLogLevel(ERROR)
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
  }

  @Test
  void getByJobExecutionId_shouldReturnFailedFuture_whenErrorLogDoesNotExist(VertxTestContext context) {
    // given
    when(errorLogDao.get(QUERY, 0, 0, TENANT_ID)).thenReturn(failedFuture("Error"));
    // when
    Future<ErrorLogCollection> future = errorLogService.get(QUERY, 0, 0, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.failed());
      context.completeNow();
    }));
  }

  @Test
  void getByJobExecutionId_shouldCallDaoGet(VertxTestContext context) {
    // given
    when(errorLogDao.get(QUERY, 5, 10, TENANT_ID)).thenReturn(succeededFuture(errorLogCollection));
    // when
    Future<ErrorLogCollection> future = errorLogService.get(QUERY, 5, 10, TENANT_ID);
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
      Assert.assertEquals(ERROR, errorLog.getLogLevel());
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
      Assert.assertEquals(ERROR, errorLog.getLogLevel());
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
    Future<ErrorLog> future = errorLogService.saveWithAffectedRecord(createRecord(), ERROR_REASON, JOB_EXECUTION_ID, TENANT_ID);
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
    JsonObject defaultRecord = createRecord();
    instanceObject.put("instance", defaultRecord.getJsonObject("instance"));
    JsonArray holdingRecords =  defaultRecord.getJsonArray("holdings");
    instanceObject.put("holdings", holdingRecords);
    for (Object holdingRecord : holdingRecords) {
        JsonObject holding = (JsonObject) holdingRecord;
        holding.remove(ITEMS);
    }
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
      Assert.assertEquals(ERROR, errorLog.getLogLevel());
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

  @Test
  void populateNotFoundUUIDsNumberErrorLog_shouldCallDaoSave_ifResponseIsEmptyList(VertxTestContext context) {
    // given
    int number = 1;
    when(errorLogDao.getByQuery(any(Criterion.class), anyString())).thenReturn(succeededFuture(emptyList()));
    // when
    errorLogService.populateUUIDsNotFoundNumberErrorLog(JOB_EXECUTION_ID, number, TENANT_ID);
    // then
    verify(errorLogDao).save(any(ErrorLog.class), eq(TENANT_ID));

    context.completeNow();
  }

  @Test
  void populateNotFoundUUIDsNumberErrorLog_shouldCallDaoUpdate_ifResponseIsWithLog(VertxTestContext context) {
    // given
    ErrorLog errorLog = new ErrorLog()
      .withLogLevel(ERROR)
      .withJobExecutionId(JOB_EXECUTION_ID)
      .withReason(SOME_RECORDS_FAILED.getDescription() + 1);
    int number = 1;
    when(errorLogDao.getByQuery(any(Criterion.class), anyString())).thenReturn(succeededFuture(singletonList(errorLog)));
    // when
    errorLogService.populateUUIDsNotFoundNumberErrorLog(JOB_EXECUTION_ID, number, TENANT_ID);
    // then
    verify(errorLogDao).update(errorLog, TENANT_ID);

    context.completeNow();
  }

  private void assertErrorLogWithHoldingsAndItems(ErrorLog errorLog, boolean isItemPresent) {
    Assert.assertEquals(ERROR, errorLog.getLogLevel());
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

  private JsonObject createRecord() {
    JsonObject defaultRecord = new JsonObject();
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
    holdingRecord.put("items", new JsonArray().add(itemRecord));
    defaultRecord.put("instance", instanceRecord);
    defaultRecord.put("holdings", holdings);
    return defaultRecord;
  }

}
