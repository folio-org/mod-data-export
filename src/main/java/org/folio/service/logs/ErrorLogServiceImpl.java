package org.folio.service.logs;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.clients.ConfigurationsClient;
import org.folio.dao.ErrorLogDao;
import org.folio.processor.error.RecordInfo;
import org.folio.processor.error.TranslationException;
import org.folio.rest.jaxrs.model.AffectedRecord;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.util.HelperUtils;
import org.folio.util.OkapiConnectionParams;
import org.marc4j.MarcException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.util.ErrorCode.SOME_RECORDS_FAILED;
import static org.folio.util.ErrorCode.SOME_UUIDS_NOT_FOUND;
import static org.folio.util.HelperUtils.getErrorLogCriterionByJobExecutionIdAndErrorCodes;

@Service
public class ErrorLogServiceImpl implements ErrorLogService {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
  private static final String COMMA_SEPARATOR = ", ";

  @Autowired
  private ErrorLogDao errorLogDao;
  @Autowired
  private ConfigurationsClient configurationsClient;
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
  public Future<ErrorLog> saveGeneralError(String errorMessageCode, String jobExecutionId, String tenantId) {
    return save(getGeneralErrorLog(errorMessageCode, jobExecutionId), tenantId);
  }

  @Override
  public Future<ErrorLog> saveGeneralErrorWithMessageValues(String errorMessageCode, List<String> errorMessageValues, String jobExecutionId, String tenantId) {
    ErrorLog errorLog = getGeneralErrorLog(errorMessageCode, jobExecutionId)
      .withErrorMessageValues(errorMessageValues);
    return save(errorLog, tenantId);
  }

  @Override
  public Future<ErrorLog> saveWithAffectedRecord(JsonObject record, String errorMessageCode, String jobExecutionId, TranslationException translationException, OkapiConnectionParams params) {
    AffectedRecord affectedRecord = new AffectedRecord();
    RecordInfo recordInfo = translationException.getRecordInfo();
    List<String> errorMessageValues = Arrays.asList(recordInfo.getFieldName(), recordInfo.getFieldValue());
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
      .withErrorMessageCode(errorMessageCode)
      .withErrorMessageValues(errorMessageValues)
      .withLogLevel(ErrorLog.LogLevel.ERROR)
      .withJobExecutionId(jobExecutionId);
    return save(errorLog, params.getTenantId());
  }

  public Future<ErrorLog> saveWithAffectedRecord(JsonObject instance, String errorMessageCode, String jobExecutionId, MarcException marcException, OkapiConnectionParams params) {
    String instId = instance.getString("id");
    String hrId = instance.getString("hrid");
    String title = instance.getString("title");
    AffectedRecord affectedRecord = new AffectedRecord()
      .withId(instId)
      .withHrid(hrId)
      .withTitle(title)
      .withRecordType(AffectedRecord.RecordType.INSTANCE)
      .withInventoryRecordLink(configurationsClient.getInventoryRecordLink(instId, jobExecutionId, params));
    if (instId == null) {
      affectedRecord.setId("UUID cannot be determined because record is invalid: field '999' or subfield 'i' not found");
    }
    if (hrId == null) {
      affectedRecord.setHrid("HRID cannot be determined because record is invalid: UUID not found");
    }
    if (title == null) {
      affectedRecord.setTitle("Title cannot be determined because record is invalid: UUID not found");
    }
    ErrorLog errorLog = new ErrorLog()
      .withErrorMessageCode(errorMessageCode)
      .withErrorMessageValues(Collections.singletonList(marcException.getMessage()))
      .withLogLevel(ErrorLog.LogLevel.ERROR)
      .withJobExecutionId(jobExecutionId)
      .withAffectedRecord(affectedRecord)
      .withCreatedDate(new Date());
    return save(errorLog, params.getTenantId());
  }

  @Override
  public void populateUUIDsNotFoundErrorLog(String jobExecutionId, Collection<String> notFoundUUIDs, String tenantId) {
    errorLogDao.getByQuery(HelperUtils.getErrorLogCriterionByJobExecutionIdAndErrorMessageCode(jobExecutionId, SOME_UUIDS_NOT_FOUND.getCode()), tenantId)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          List<ErrorLog> errorLogs = ar.result();
          List<String> newUUIDs = Arrays.asList(StringUtils.joinWith(COMMA_SEPARATOR, notFoundUUIDs).replace("[", EMPTY).replace("]", EMPTY));
          if (errorLogs.isEmpty()) {
            saveGeneralErrorWithMessageValues(SOME_UUIDS_NOT_FOUND.getCode(), newUUIDs, jobExecutionId, tenantId);
          } else {
            ErrorLog errorLog = errorLogs.get(0);
            String savedUUIDs = errorLog.getErrorMessageValues().get(0);
            errorLog.setErrorMessageValues(Arrays.asList(savedUUIDs + COMMA_SEPARATOR + newUUIDs));
            update(errorLog, tenantId);
          }
        } else {
          LOGGER.error("Failed to query error logs by jobExecutionId: {} and reason: {}", jobExecutionId, SOME_UUIDS_NOT_FOUND.getDescription());
        }
      });
  }

  @Override
  public void populateUUIDsNotFoundNumberErrorLog(String jobExecutionId, int numberOfNotFoundUUIDs, String tenantId) {
    errorLogDao.getByQuery(HelperUtils.getErrorLogCriterionByJobExecutionIdAndErrorMessageCode(jobExecutionId,SOME_RECORDS_FAILED.getCode()), tenantId)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          List<ErrorLog> errorLogs = ar.result();
          if (errorLogs.isEmpty()) {
            //replace message with code
            //split values from code
            saveGeneralErrorWithMessageValues(SOME_RECORDS_FAILED.getCode(), Arrays.asList(String.valueOf(numberOfNotFoundUUIDs)), jobExecutionId, tenantId);
          } else {
            ErrorLog errorLog = errorLogs.get(0);
            List<String> errorMessageValues = errorLog.getErrorMessageValues();
            //get values form errorMessageValues to increase and save to appropriate field
            int updatedNumberOfNotFoundUUIDs = Integer.parseInt(errorMessageValues.get(0)) + numberOfNotFoundUUIDs;
            errorLog.setErrorMessageValues(Arrays.asList(String.valueOf(updatedNumberOfNotFoundUUIDs)));
            update(errorLog, tenantId);
          }
        } else {
          LOGGER.error("Failed to query error logs by jobExecutionId: {} and reason: {}", jobExecutionId, SOME_RECORDS_FAILED.getDescription() + numberOfNotFoundUUIDs);
        }
      });
  }

  @Override
  public Future<Boolean> isErrorsByErrorCodePresent(
      List<String> errorCodes, String jobExecutionId, String tenantId) {
    Promise<Boolean> promise = Promise.promise();
    getByQuery(getErrorLogCriterionByJobExecutionIdAndErrorCodes(jobExecutionId, errorCodes), tenantId)
        .onSuccess(errorLogList -> promise.complete(CollectionUtils.isNotEmpty(errorLogList)))
        .onFailure(ar -> promise.complete(false));

    return promise.future();
  }

  private ErrorLog getGeneralErrorLog(String errorMessageCode, String jobExecutionId) {
    return new ErrorLog()
      .withErrorMessageCode(errorMessageCode)
      .withLogLevel(ErrorLog.LogLevel.ERROR)
      .withJobExecutionId(jobExecutionId);
  }

}
