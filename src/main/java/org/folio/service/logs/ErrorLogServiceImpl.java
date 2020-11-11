package org.folio.service.logs;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dao.ErrorLogDao;
import org.folio.rest.jaxrs.model.AffectedRecord;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.util.ErrorCode;
import org.folio.util.HelperUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.HOLDINGS;
import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.INSTANCE;
import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.ITEM;
import static org.folio.util.ErrorCode.SOME_RECORDS_FAILED;
import static org.folio.util.ErrorCode.SOME_UUIDS_NOT_FOUND;
import static org.folio.util.HelperUtils.getErrorLogCriterionByJobExecutionIdAndReason;

@Service
public class ErrorLogServiceImpl implements ErrorLogService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String HRID_KEY = "hrid";
  private static final String ID_KEY = "id";
  private static final String TITLE_KEY = "title";
  private static final String ITEMS = "items";
  private static final String HOLDINGS_RECORD_ID = "holdingsRecordId";
  private static final String COMMA_SEPARATOR = ", ";

  @Autowired
  private ErrorLogDao errorLogDao;

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
  public Future<ErrorLog> saveWithAffectedRecord(JsonObject record, String reason, String jobExecutionId, String tenantId) {
    JsonObject instance = record.getJsonObject(INSTANCE.toString().toLowerCase());
    AffectedRecord instanceRecord = new AffectedRecord();
    if (Objects.isNull(instance)) {
      instanceRecord.setRecordType(INSTANCE);
    } else {
      instanceRecord = getAffectedRecordFromJson(record.getJsonObject(INSTANCE.toString().toLowerCase()), INSTANCE);
      List<AffectedRecord> holdingsAndAssociatedItems = getRecordsForHoldingAndAssociatedItems(record);
      instanceRecord.setAffectedRecords(holdingsAndAssociatedItems);
    }
    ErrorLog errorLog = new ErrorLog()
      .withAffectedRecord(instanceRecord)
      .withReason(reason)
      .withLogLevel(ErrorLog.LogLevel.ERROR)
      .withJobExecutionId(jobExecutionId);
    return save(errorLog, tenantId);
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
      ErrorCode errorCode, String jobExecutionId, String tenantId) {
    Promise<Boolean> promise = Promise.promise();
    getByQuery(
            getErrorLogCriterionByJobExecutionIdAndReason(
                jobExecutionId, errorCode.getDescription()),
            tenantId)
        .onSuccess(errorLogList -> promise.complete(CollectionUtils.isNotEmpty(errorLogList)))
        .onFailure(ar -> promise.complete(false));

    return promise.future();
  }

  private List<AffectedRecord> getRecordsForHoldingAndAssociatedItems(JsonObject record) {
    List<AffectedRecord> holdingsAndAssociatedItems = new ArrayList<>();
    JsonArray holdings = record.getJsonArray(HOLDINGS.toString().toLowerCase());
    if (Objects.nonNull(holdings)) {
      for (Object holding : holdings) {
        JsonObject holdingJson = JsonObject.mapFrom(holding);
        AffectedRecord holdingRecord = getAffectedRecordFromJson(holdingJson, AffectedRecord.RecordType.HOLDINGS);
        holdingRecord.setAffectedRecords(getAssociatedItemRecords(holdingJson.getJsonArray(ITEMS)));
        holdingsAndAssociatedItems.add(holdingRecord);
      }
    }
    return holdingsAndAssociatedItems;
  }

  private List<AffectedRecord> getAssociatedItemRecords(JsonArray items) {
    List<AffectedRecord> associatedItemRecords = new ArrayList<>();
    if (Objects.nonNull(items)) {
      for (Object item : items) {
        associatedItemRecords.add(getAffectedRecordFromJson(JsonObject.mapFrom(item), ITEM));
      }
    }
    return associatedItemRecords;
  }

  private AffectedRecord getAffectedRecordFromJson(JsonObject recordJson, AffectedRecord.RecordType recordType) {
    AffectedRecord record = new AffectedRecord()
      .withRecordType(recordType);
    if (StringUtils.isNotBlank(recordJson.getString(HRID_KEY))) {
      record.setHrid(recordJson.getString(HRID_KEY));
    }
    if (StringUtils.isNotBlank(recordJson.getString(ID_KEY))) {
      record.setId(recordJson.getString(ID_KEY));
    }
    if (StringUtils.isNotBlank(recordJson.getString(TITLE_KEY))) {
      record.setTitle(recordJson.getString(TITLE_KEY));
    }
    return record;
  }

}
