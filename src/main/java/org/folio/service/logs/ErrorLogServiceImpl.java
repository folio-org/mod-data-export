package org.folio.service.logs;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.dao.ErrorLogDao;
import org.folio.rest.jaxrs.model.AffectedRecord;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.INSTANCE;

@Service
public class ErrorLogServiceImpl implements ErrorLogService {
  private static final String HRID_KEY = "hrid";
  private static final String ID_KEY = "id";
  private static final String TITLE_KEY = "title";

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
    AffectedRecord instanceRecord = new AffectedRecord()
      .withRecordType(INSTANCE);
    if(StringUtils.isNotBlank(record.getString(HRID_KEY))) {
      instanceRecord.setHrid(record.getString(HRID_KEY));
    }
    if(StringUtils.isNotBlank(record.getString(ID_KEY))) {
      instanceRecord.setId(record.getString(ID_KEY));
    }
    if(StringUtils.isNotBlank(record.getString(TITLE_KEY))) {
      instanceRecord.setTitle(record.getString(TITLE_KEY));
    }
    ErrorLog errorLog = new ErrorLog()
      .withAffectedRecord(instanceRecord)
      .withReason(reason)
      .withLogLevel(ErrorLog.LogLevel.ERROR)
      .withJobExecutionId(jobExecutionId);
    return save(errorLog, tenantId);
  }


}
