package org.folio.dataexp.service.logs;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dataexp.service.ConfigurationService.INVENTORY_RECORD_LINK_KEY;
import static org.folio.dataexp.service.export.Constants.DELETED_KEY;
import static org.folio.dataexp.util.Constants.QUERY_CQL_ALL_RECORDS;
import static org.folio.dataexp.util.ErrorCode.SOME_RECORDS_FAILED;
import static org.folio.dataexp.util.ErrorCode.SOME_UUIDS_NOT_FOUND;
import static software.amazon.awssdk.utils.StringUtils.isEmpty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.folio.dataexp.domain.dto.AffectedRecord;
import org.folio.dataexp.domain.dto.ErrorLog;
import org.folio.dataexp.domain.dto.ErrorLogCollection;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.ErrorLogEntity;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.service.CommonExportStatistic;
import org.folio.dataexp.service.ConfigurationService;
import org.folio.dataexp.util.ErrorCode;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.marc4j.MarcException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorLogService {
  public static final String ID = "id";
  public static final String HRID = "hrid";
  public static final String TITLE = "title";
  private static final String COMMA_SEPARATOR = ", ";

  private final ErrorLogEntityCqlRepository errorLogEntityCqlRepository;
  private final FolioExecutionContext folioExecutionContext;
  private final ConfigurationService configurationService;

  public ErrorLogCollection getErrorLogsByQuery(String query, int offset, int limit) {
    query = isEmpty(query) ? QUERY_CQL_ALL_RECORDS : query;
    var errorLogEntityPage = errorLogEntityCqlRepository.findByCql(query, OffsetRequest.of(offset, limit));
    return new ErrorLogCollection()
      .errorLogs(errorLogEntityPage.stream().map(ErrorLogEntity::getErrorLog).toList())
      .totalRecords((int) errorLogEntityPage.getTotalElements());
  }

  public List<ErrorLog> getByQuery(String query) {
    return errorLogEntityCqlRepository.findByCql(query, OffsetRequest.of(0, Integer.MAX_VALUE)).stream()
      .map(ErrorLogEntity::getErrorLog)
      .toList();
  }

  public ErrorLog save(ErrorLog errorLog) {
    if (errorLog.getId() == null) {
      errorLog.setId(UUID.randomUUID());
    }
    errorLog.setCreatedDate(new Date());
    return errorLogEntityCqlRepository.save(ErrorLogEntity.builder()
        .id(errorLog.getId())
        .errorLog(errorLog)
        .creationDate(errorLog.getCreatedDate())
        .createdBy(folioExecutionContext.getUserId().toString())
        .jobExecutionId(errorLog.getJobExecutionId()).build())
      .getErrorLog();
  }

  public ErrorLog update(ErrorLog errorLog) {
    return save(errorLog);
  }

  public Boolean deleteById(UUID id) {
    errorLogEntityCqlRepository.deleteById(id);
    return true;
  }

  public ErrorLog saveGeneralError(String errorMessageCode, UUID jobExecutionId) {
    return save(getGeneralErrorLog(errorMessageCode, jobExecutionId));
  }

  public ErrorLog saveGeneralErrorWithMessageValues(String errorMessageCode, List<String> errorMessageValues, UUID jobExecutionId) {
    var errorLog = getGeneralErrorLog(errorMessageCode, jobExecutionId)
      .errorMessageValues(errorMessageValues);
    return save(errorLog);
  }

  public void saveCommonExportFailsErrors(CommonExportStatistic commonExportStatistic, int totalErrors, UUID jobExecutionId) {
    if (!commonExportStatistic.getInvalidUUIDFormat().isEmpty()) {
      var errorLog = new ErrorLog();
      errorLog.setId(UUID.randomUUID());
      errorLog.createdDate(new Date());
      errorLog.setJobExecutionId(jobExecutionId);
      var message = String.join(",", commonExportStatistic.getInvalidUUIDFormat());
      errorLog.setErrorMessageValues(List.of(message));
      errorLog.setErrorMessageCode(ErrorCode.INVALID_UUID_FORMAT.getCode());
      this.save(errorLog);
    }

    if (!commonExportStatistic.getNotExistUUID().isEmpty()) {
      var errorLog = new ErrorLog();
      errorLog.setId(UUID.randomUUID());
      errorLog.createdDate(new Date());
      errorLog.setJobExecutionId(jobExecutionId);
      var message = String.join(", ", commonExportStatistic.getNotExistUUID());
      errorLog.setErrorMessageValues(List.of(message));
      errorLog.setErrorMessageCode(ErrorCode.SOME_UUIDS_NOT_FOUND.getCode());
      this.save(errorLog);
    }

    if (totalErrors > 0) {
      var errorLog = new ErrorLog();
      errorLog.setId(UUID.randomUUID());
      errorLog.createdDate(new Date());
      errorLog.setJobExecutionId(jobExecutionId);
      errorLog.setErrorMessageValues(List.of(String.valueOf(totalErrors)));
      errorLog.setErrorMessageCode(ErrorCode.SOME_RECORDS_FAILED.getCode());
      this.save(errorLog);
    }
  }

  public void saveFailedToReadInputFileError(UUID jobExecutionId) {
    var errorLog = new ErrorLog();
    errorLog.setId(UUID.randomUUID());
    errorLog.createdDate(new Date());
    errorLog.setJobExecutionId(jobExecutionId);
    errorLog.setErrorMessageValues(new ArrayList<>());
    errorLog.setErrorMessageCode(ErrorCode.ERROR_READING_FROM_INPUT_FILE.getCode());
    this.save(errorLog);
  }

  public ErrorLog saveWithAffectedRecord(JSONObject instance, String errorMessageCode, UUID jobExecutionId, MarcException marcException) {
    String instId = instance.getAsString(ID);
    String hrId = instance.getAsString(HRID);
    String title = instance.getAsString(TITLE);
    String inventoryLink = (boolean)instance.get(DELETED_KEY) ? EMPTY : getInventoryRecordLink() + instId;
    AffectedRecord affectedRecord = new AffectedRecord()
      .id(instId)
      .hrid(hrId)
      .title(title)
      .recordType(RecordTypes.INSTANCE)
      .inventoryRecordLink(inventoryLink);
    if (instId == null) {
      affectedRecord.setId("UUID cannot be determined because record is invalid: field '999' or subfield 'i' not found");
    }
    if (hrId == null) {
      affectedRecord.setHrid("HRID cannot be determined because record is invalid: UUID not found");
    }
    if (title == null) {
      affectedRecord.setTitle("Title cannot be determined because record is invalid: UUID not found");
    }
    var errorLog = new ErrorLog()
      .errorMessageCode(errorMessageCode)
      .errorMessageValues(Collections.singletonList(marcException.getMessage()))
      .logLevel(ErrorLog.LogLevelEnum.ERROR)
      .jobExecutionId(jobExecutionId)
      .affectedRecord(affectedRecord)
      .createdDate(new Date());
    return save(errorLog);
  }

  public ErrorLog saveWithAffectedRecord(JSONObject instance, String errorMessage, String errorMessageCode, UUID jobExecutionId) {
    String instId = instance.getAsString(ID);
    String hrId = instance.getAsString(HRID);
    String title = instance.getAsString(TITLE);
    String generalEndOfErrorMsg = " cannot be determined because instance record is not found or invalid, but still contains more than 1 SRS record";
    String inventoryLink = (boolean)instance.get(DELETED_KEY) ? EMPTY : getInventoryRecordLink() + instId;
    if (instId == null) {
      instId = "UUID" + generalEndOfErrorMsg;
    }
    if (hrId == null) {
      hrId = "HRID" + generalEndOfErrorMsg;
    }
    if (title == null) {
      title = "Title" + generalEndOfErrorMsg;
    }
    var affectedRecord = new AffectedRecord()
      .id(instId)
      .hrid(hrId)
      .title(title)
      .recordType(RecordTypes.INSTANCE)
      .inventoryRecordLink(inventoryLink);
    var errorLog = new ErrorLog()
      .errorMessageCode(errorMessageCode)
      .errorMessageValues(Collections.singletonList(errorMessage))
      .logLevel(ErrorLog.LogLevelEnum.ERROR)
      .jobExecutionId(jobExecutionId)
      .affectedRecord(affectedRecord)
      .createdDate(new Date());
    return save(errorLog);
  }

  public void populateUUIDsNotFoundErrorLog(UUID jobExecutionId, Collection<String> notFoundUUIDs) {
    var errorLogs = errorLogEntityCqlRepository.getByJobExecutionIdAndErrorCode(jobExecutionId, SOME_UUIDS_NOT_FOUND.getCode());
    var newUUIDs = Collections.singletonList(String.join(COMMA_SEPARATOR, notFoundUUIDs).replace("[", EMPTY).replace("]", EMPTY));
    if (errorLogs.isEmpty()) {
      saveGeneralErrorWithMessageValues(SOME_UUIDS_NOT_FOUND.getCode(), newUUIDs, jobExecutionId);
    } else {
      var errorLog = errorLogs.get(0).getErrorLog();
      var savedUUIDs = errorLog.getErrorMessageValues().get(0);
      errorLog.setErrorMessageValues(Collections.singletonList(savedUUIDs + COMMA_SEPARATOR + newUUIDs));
      update(errorLog);
    }
  }

  public void populateUUIDsNotFoundNumberErrorLog(UUID jobExecutionId, int numberOfNotFoundUUIDs) {
    var errorLogs = errorLogEntityCqlRepository.getByJobExecutionIdAndErrorCode(jobExecutionId, SOME_UUIDS_NOT_FOUND.getCode());
    if (errorLogs.isEmpty()) {
      saveGeneralErrorWithMessageValues(SOME_RECORDS_FAILED.getCode(), Collections.singletonList(String.valueOf(numberOfNotFoundUUIDs)), jobExecutionId);
    } else {
      var errorLog = errorLogs.get(0).getErrorLog();
      var errorMessageValues = errorLog.getErrorMessageValues();
      //get values form errorMessageValues to increase and save to appropriate field
      int updatedNumberOfNotFoundUUIDs = Integer.parseInt(errorMessageValues.get(0)) + numberOfNotFoundUUIDs;
      errorLog.setErrorMessageValues(Collections.singletonList(String.valueOf(updatedNumberOfNotFoundUUIDs)));
      update(errorLog);
    }
  }

  public Boolean isErrorsByErrorCodePresent(List<String> errorCodes, UUID jobExecutionId) {
    var errorCodesString = errorCodes.size() > 1 ?
      "%(" + String.join("|", errorCodes) + ")%" :
      "%" + errorCodes.get(0) + "%";
    return isNotEmpty(errorLogEntityCqlRepository.getByJobExecutionIdAndErrorCodes(jobExecutionId, errorCodesString));
  }

  private ErrorLog getGeneralErrorLog(String errorMessageCode, UUID jobExecutionId) {
    return new ErrorLog()
      .errorMessageCode(errorMessageCode)
      .logLevel(ErrorLog.LogLevelEnum.ERROR)
      .jobExecutionId(jobExecutionId);
  }

  private String getInventoryRecordLink() {
    return configurationService.getValue(INVENTORY_RECORD_LINK_KEY);
  }
}
