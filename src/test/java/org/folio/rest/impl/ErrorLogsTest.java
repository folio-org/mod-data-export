package org.folio.rest.impl;

import com.google.common.collect.Lists;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.Context;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpStatus;
import org.folio.dao.ErrorLogDao;
import org.folio.rest.jaxrs.model.AffectedRecord;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.folio.service.ApplicationTestConfig;
import org.folio.spring.SpringContextUtil;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class ErrorLogsTest extends RestVerticleTestBase {

  @Autowired
  private ErrorLogDao errorLogDao;

  public ErrorLogsTest() {
    Context vertxContext = vertx.getOrCreateContext();
    SpringContextUtil.init(vertxContext.owner(), vertxContext, DataExportTest.TestMock.class);
    SpringContextUtil.autowireDependencies(this, vertxContext);
  }

  @Test
  void shouldReturnLogsByJobExecutionId(VertxTestContext context) {
    // given
    String jobExecutionId = UUID.randomUUID().toString();
    Date date = new Date();
    String logId = UUID.randomUUID().toString();
    String recordId = UUID.randomUUID().toString();
    AffectedRecord instanceRecord = new AffectedRecord()
      .withRecordType(AffectedRecord.RecordType.INSTANCE)
      .withId(recordId)
      .withHrid("instance hrid")
      .withTitle("instance title");
    ErrorLog errorLog = new ErrorLog()
      .withCreatedDate(date)
      .withJobExecutionId(jobExecutionId)
      .withLogLevel(ErrorLog.LogLevel.ERROR)
      .withId(logId)
      .withAffectedRecord(instanceRecord)
      .withErrorMessageCode("error.messageCode");
    errorLogDao.save(errorLog, okapiConnectionParams.getTenantId());

    // when
    vertx.setTimer(3000, handler -> {
      Response response = RestAssured.given()
        .spec(jsonRequestSpecification)
        .get(ERROR_LOGS_SERVICE_URL + "?query=jobExecutionId=" + jobExecutionId);

      // then
      context.verify(() -> {
        ErrorLogCollection errorLogCollection = response.as(ErrorLogCollection.class);
        Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertFalse(errorLogCollection.getErrorLogs().isEmpty());
        ErrorLog errorLog1 = errorLogCollection.getErrorLogs().get(0);
        Assert.assertEquals(date, errorLog1.getCreatedDate());
        Assert.assertEquals(jobExecutionId, errorLog1.getJobExecutionId());
        Assert.assertEquals(ErrorLog.LogLevel.ERROR, errorLog1.getLogLevel());
        Assert.assertEquals(logId, errorLog1.getId());
        Assert.assertEquals("error.messageCode", errorLog1.getErrorMessageCode());
        AffectedRecord affectedRecord = errorLog1.getAffectedRecord();
        Assert.assertEquals(recordId, affectedRecord.getId());
        Assert.assertEquals("instance hrid", affectedRecord.getHrid());
        Assert.assertEquals("instance title", affectedRecord.getTitle());
        Assert.assertEquals(AffectedRecord.RecordType.INSTANCE, affectedRecord.getRecordType());
        Assert.assertTrue(affectedRecord.getAffectedRecords().isEmpty());
        context.completeNow();
      });
    });
  }

  @Test
  void shouldReturnErrorLogsCollectionWithTwoRecords(VertxTestContext context) {
    // given
    AffectedRecord instanceRecord = new AffectedRecord()
      .withRecordType(AffectedRecord.RecordType.INSTANCE)
      .withId(UUID.randomUUID().toString())
      .withHrid("instance hrid")
      .withTitle("instance title");
    AffectedRecord holdingRecord = new AffectedRecord()
      .withAffectedRecords(Lists.newArrayList(instanceRecord))
      .withRecordType(AffectedRecord.RecordType.HOLDINGS)
      .withHrid("holdings hrid")
      .withId(UUID.randomUUID().toString())
      .withTitle("holdings title");
    String jobExecutionId = UUID.randomUUID().toString();
    ErrorLog errorLog1 = new ErrorLog()
      .withCreatedDate(new Date())
      .withJobExecutionId(jobExecutionId)
      .withLogLevel(ErrorLog.LogLevel.ERROR)
      .withId(UUID.randomUUID().toString())
      .withErrorMessageCode("error.messageCode")
      .withAffectedRecord(holdingRecord);
    ErrorLog errorLog2 = new ErrorLog()
      .withCreatedDate(new Date())
      .withJobExecutionId(jobExecutionId)
      .withLogLevel(ErrorLog.LogLevel.ERROR)
      .withId(UUID.randomUUID().toString())
      .withErrorMessageCode("error.messageCode")
      .withAffectedRecord(holdingRecord);

    // when
    errorLogDao.save(errorLog1, okapiConnectionParams.getTenantId())
      .onSuccess(errorLog -> {
        errorLogDao.save(errorLog2, okapiConnectionParams.getTenantId());
      });


    vertx.setTimer(3000, handler -> {
      Response response = getRequest(ERROR_LOGS_SERVICE_URL + "?query=jobExecutionId=" + jobExecutionId);
      // then
      context.verify(() -> {
        ErrorLogCollection errorLogCollection = response.as(ErrorLogCollection.class);
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertEquals(2, errorLogCollection.getTotalRecords().intValue());
        context.completeNow();
      });
    });

  }


}
