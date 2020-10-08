package org.folio.service.logs;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.dao.ErrorLogDao;
import org.folio.rest.jaxrs.model.AffectedRecord;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.HOLDINGS;
import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.INSTANCE;
import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.ITEM;

@Service
public class ErrorLogServiceImpl implements ErrorLogService {
  private static final String HRID_KEY = "hrid";
  private static final String ID_KEY = "id";
  private static final String TITLE_KEY = "title";
  private static final String ITEMS = "items";
  private static final String HOLDINGS_RECORD_ID = "holdingsRecordId";

  @Autowired
  private ErrorLogDao errorLogDao;

  @Override
  public Future<ErrorLogCollection> getByJobExecutionId(String query, int offset, int limit, String tenantId) {
    return errorLogDao.getByJobExecutionId(query, offset, limit, tenantId);
  }

  @Override
  public Future<ErrorLog> save(ErrorLog errorLog, String tenantId) {
    if (errorLog.getId() == null) {
      errorLog.setId(UUID.randomUUID().toString());
    }
    errorLog.setCreatedData(new Date());
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

  private List<AffectedRecord> getRecordsForHoldingAndAssociatedItems(JsonObject record) {
    List<AffectedRecord> holdingsAndAssociatedItems = new ArrayList<>();
    JsonArray holdings = record.getJsonArray(HOLDINGS.toString().toLowerCase());
    if (Objects.nonNull(holdings)) {
      for (Object holding : holdings) {
        JsonObject holdingJson = JsonObject.mapFrom(holding);
        AffectedRecord holdingRecord = getAffectedRecordFromJson(holdingJson, AffectedRecord.RecordType.HOLDINGS);
        holdingRecord.setAffectedRecords(getAssociatedItemRecords(record.getJsonArray(ITEMS), holdingJson.getString(ID_KEY)));
        holdingsAndAssociatedItems.add(holdingRecord);
      }
    }
    return holdingsAndAssociatedItems;
  }

  private List<AffectedRecord> getAssociatedItemRecords(JsonArray items, String holdingId) {
    List<AffectedRecord> associatedItemRecords = new ArrayList<>();
    if (Objects.nonNull(items)) {
      for (Object item : items) {
        JsonObject itemJson = JsonObject.mapFrom(item);
        if (StringUtils.isNotEmpty(holdingId) && holdingId.equals(itemJson.getString(HOLDINGS_RECORD_ID))) {
          associatedItemRecords.add(getAffectedRecordFromJson(itemJson, ITEM));
        }
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
