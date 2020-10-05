package org.folio.service.logs;

import io.vertx.core.Future;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class ErrorLogServiceUnitTest {
  private static final String QUERY = "query";
  private static final String TENANT_ID = "diku";
  private static final String JOB_EXECUTION_ID = "jobExecutionId";
  private static final String ERROR_REASON = "Error reason";


  @Spy
  @InjectMocks
  private ErrorLogServiceImpl errorLogService;
  @Mock
  private ErrorLogDaoImpl errorLogDao;

  @Captor
  private ArgumentCaptor<ErrorLog> errorLogCaptor;

  private static OkapiConnectionParams okapiConnectionParams;
  private static ErrorLog errorLog;
  private static ErrorLogCollection errorLogCollection;

  @BeforeAll
  static void beforeEach() {
    errorLog = new ErrorLog()
      .withId(UUID.randomUUID().toString())
      .withJobExecutionId(UUID.randomUUID().toString())
      .withLogLevel(ErrorLog.LogLevel.ERROR)
      .withReason("Error reason")
      .withCreatedData(new Date())
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
      .put("title", "The Journal of ecclesiastical history");
    when(errorLogDao.save(any(ErrorLog.class), eq(TENANT_ID))).thenReturn(succeededFuture(errorLog));
    // when
    Future<ErrorLog> future = errorLogService.saveWithAffectedRecord(instanceRecord, ERROR_REASON, JOB_EXECUTION_ID, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(errorLogDao).save(errorLogCaptor.capture(), eq(TENANT_ID));
      ErrorLog errorLog = errorLogCaptor.getValue();
      Assert.assertEquals(errorLog.getLogLevel(), ErrorLog.LogLevel.ERROR);
      Assert.assertEquals(errorLog.getReason(), ERROR_REASON);
      Assert.assertEquals(errorLog.getJobExecutionId(), JOB_EXECUTION_ID);
      Assert.assertEquals(errorLog.getAffectedRecord().getRecordType(), AffectedRecord.RecordType.INSTANCE);
      Assert.assertEquals(errorLog.getAffectedRecord().getHrid(), "1");
      Assert.assertEquals(errorLog.getAffectedRecord().getId(), "c8b50e3f-0446-429c-960e-03774b88223f");
      Assert.assertEquals(errorLog.getAffectedRecord().getTitle(), "The Journal of ecclesiastical history");
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
      Assert.assertEquals(errorLog.getLogLevel(), ErrorLog.LogLevel.ERROR);
      Assert.assertEquals(errorLog.getReason(), ERROR_REASON);
      Assert.assertEquals(errorLog.getJobExecutionId(), JOB_EXECUTION_ID);
      Assert.assertEquals(errorLog.getAffectedRecord().getRecordType(), AffectedRecord.RecordType.INSTANCE);
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
      Assert.assertEquals(errorLog.getLogLevel(), ErrorLog.LogLevel.ERROR);
      Assert.assertEquals(errorLog.getReason(), ERROR_REASON);
      Assert.assertEquals(errorLog.getJobExecutionId(), JOB_EXECUTION_ID);
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

}
