package org.folio.service.logs;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dao.ErrorLogDao;
import org.folio.processor.error.RecordInfo;
import org.folio.processor.error.TranslationException;
import org.folio.rest.jaxrs.model.AffectedRecord;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.util.HelperUtils;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.util.ErrorCode.SOME_RECORDS_FAILED;
import static org.folio.util.ErrorCode.SOME_UUIDS_NOT_FOUND;
import static org.folio.util.HelperUtils.getErrorLogCriterionByJobExecutionIdAndReasons;

@Service
public class ErrorLogServiceImpl implements ErrorLogService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String COMMA_SEPARATOR = ", ";

  @Autowired
  private ErrorLogDao errorLogDao;

  @Autowired
  @Qualifier("affectedRecordBuilders")
  private Map<String, AffectedRecordBuilder> affectedRecordsBuilders;

  @Override
  public Future<ErrorLogCollection> get(String jobExecutionId, int offset, int limit, String tenantId) {
    return errorLogDao.get(jobExecutionId, offset, limit, tenantId);
  }

  @Override
  public Future<List<ErrorLog>> getByQuery(Criterion criterion, String tenantId) {
    return errorLogDao.getByQuery(criterion, tenantId);
  }

  @Override
  public Future<ErrorLog> save(ErrorLog errorLog, String tenantId) {
    if (errorLog.getId() == null) {
      errorLog.setId(UUID.randomUUID().toString());
    }
    errorLog.setCreatedDate(new Date());
    return errorLogDao.save(errorLog, tenantId);
  }

  @Override
  public Future<ErrorLog> update(ErrorLog errorLog, String tenantId) {
    return errorLogDao.update(errorLog, tenantId);
  }

  @Override
  public Future<Boolean> deleteById(String id, String tenantId) {
    return errorLogDao.deleteById(id, tenantId);
  }

  @Override
  public Future<ErrorLog> saveGeneralError(String reason, String jobExecutionId, String tenantId) {
    ErrorLog errorLog = new ErrorLog()
      .withReason(reason)
      .withLogLevel(ErrorLog.LogLevel.ERROR)
      .withJobExecutionId(jobExecutionId);
    return save(errorLog, tenantId);
  }

  @Override
  public Future<ErrorLog> saveWithAffectedRecord(JsonObject record, String reason, String jobExecutionId, TranslationException translationException, OkapiConnectionParams params) {
    AffectedRecord affectedRecord = new AffectedRecord();
    RecordInfo recordInfo = translationException.getRecordInfo();
    if (recordInfo.getType().isInstance()) {
      affectedRecord = affectedRecordsBuilders
        .get(AffectedRecordInstanceBuilder.class.getName())
        .build(record, jobExecutionId, recordInfo.getId(), true, params);
    } else if (recordInfo.getType().isHolding()) {
      affectedRecord = affectedRecordsBuilders
        .get(AffectedRecordHoldingBuilder.class.getName())
        .build(record, jobExecutionId, recordInfo.getId(), true, params);
    } else if (recordInfo.getType().isItem()){
      affectedRecord = affectedRecordsBuilders
        .get(AffectedRecordItemBuilder.class.getName())
        .build(record, jobExecutionId, recordInfo.getId(), true, params);
    }
    ErrorLog errorLog = new ErrorLog()
      .withAffectedRecord(affectedRecord)
      .withReason(reason)
      .withLogLevel(ErrorLog.LogLevel.ERROR)
      .withJobExecutionId(jobExecutionId);
    return save(errorLog, params.getTenantId());
  }

  @Override
  public void populateUUIDsNotFoundErrorLog(String jobExecutionId, Collection<String> notFoundUUIDs, String tenantId) {
    errorLogDao.getByQuery(HelperUtils.getErrorLogCriterionByJobExecutionIdAndReason(jobExecutionId, SOME_UUIDS_NOT_FOUND.getDescription()), tenantId)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          List<ErrorLog> errorLogs = ar.result();
          if (errorLogs.isEmpty()) {
            saveGeneralError(SOME_UUIDS_NOT_FOUND.getDescription() +
              StringUtils.joinWith(COMMA_SEPARATOR, notFoundUUIDs).replace("[", EMPTY).replace("]", EMPTY), jobExecutionId, tenantId);
          } else {
            ErrorLog errorLog = errorLogs.get(0);
            String reason = errorLog.getReason();
            errorLog.setReason(reason + COMMA_SEPARATOR + StringUtils.joinWith(COMMA_SEPARATOR, notFoundUUIDs).replace("[", EMPTY).replace("]", EMPTY));
            update(errorLog, tenantId);
          }
        } else {
          LOGGER.error("Failed to query error logs by jobExecutionId: {} and reason: {}", jobExecutionId, SOME_UUIDS_NOT_FOUND.getDescription());
        }
      });
  }

  @Override
  public void populateUUIDsNotFoundNumberErrorLog(String jobExecutionId, int numberOfNotFoundUUIDs, String tenantId) {
    errorLogDao.getByQuery(HelperUtils.getErrorLogCriterionByJobExecutionIdAndReason(jobExecutionId,SOME_RECORDS_FAILED.getDescription()), tenantId)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          List<ErrorLog> errorLogs = ar.result();
          if (errorLogs.isEmpty()) {
            saveGeneralError(SOME_RECORDS_FAILED.getDescription() + numberOfNotFoundUUIDs, jobExecutionId, tenantId);
          } else {
            ErrorLog errorLog = errorLogs.get(0);
            String reason = errorLog.getReason();
            int updatedNumberOfNotFoundUUIDs = Integer.parseInt(reason.replaceAll("\\D+", "")) + numberOfNotFoundUUIDs;
            errorLog.setReason(reason.replaceAll("\\d", EMPTY).trim() + SPACE + updatedNumberOfNotFoundUUIDs);
            update(errorLog, tenantId);
          }
        } else {
          LOGGER.error("Failed to query error logs by jobExecutionId: {} and reason: {}", jobExecutionId, SOME_RECORDS_FAILED.getDescription() + numberOfNotFoundUUIDs);
        }
      });
  }

  @Override
  public Future<Boolean> isErrorsByReasonPresent(
      List<String> reasons, String jobExecutionId, String tenantId) {
    Promise<Boolean> promise = Promise.promise();
    getByQuery(getErrorLogCriterionByJobExecutionIdAndReasons(jobExecutionId, reasons), tenantId)
        .onSuccess(errorLogList -> promise.complete(CollectionUtils.isNotEmpty(errorLogList)))
        .onFailure(ar -> promise.complete(false));

    return promise.future();
  }

}
