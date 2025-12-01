package org.folio.dataexp.service.logs;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dataexp.service.ConfigurationService.INVENTORY_RECORD_LINK_KEY;
import static org.folio.dataexp.service.export.Constants.DELETED_KEY;
import static org.folio.dataexp.util.Constants.QUERY_CQL_ALL_RECORDS;
import static org.folio.dataexp.util.ErrorCode.SOME_RECORDS_FAILED;
import static org.folio.dataexp.util.ErrorCode.SOME_UUIDS_NOT_FOUND;
import static software.amazon.awssdk.utils.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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

/** Service for managing error logs, including saving, updating, and retrieving error logs. */
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

  /**
   * Retrieves error logs by CQL query, offset, and limit.
   *
   * @param query the CQL query
   * @param offset the offset
   * @param limit the limit
   * @return ErrorLogCollection containing error logs
   */
  public ErrorLogCollection getErrorLogsByQuery(String query, int offset, int limit) {
    query = isEmpty(query) ? QUERY_CQL_ALL_RECORDS : query;
    var errorLogEntityPage = errorLogEntityCqlRepository.findByCql(
        query,
        OffsetRequest.of(offset, limit)
    );
    return new ErrorLogCollection()
        .errorLogs(
            errorLogEntityPage.stream()
                .map(ErrorLogEntity::getErrorLog)
                .toList()
        )
        .totalRecords((int) errorLogEntityPage.getTotalElements());
  }

  /**
   * Retrieves error logs by CQL query.
   *
   * @param query the CQL query
   * @return list of ErrorLog
   */
  public List<ErrorLog> getByQuery(String query) {
    return errorLogEntityCqlRepository.findByCql(
            query,
            OffsetRequest.of(0, Integer.MAX_VALUE)
        ).stream()
        .map(ErrorLogEntity::getErrorLog)
        .toList();
  }

  /**
   * Saves an error log.
   *
   * @param errorLog the error log to save
   * @return the saved ErrorLog
   */
  public ErrorLog save(ErrorLog errorLog) {
    if (errorLog.getId() == null) {
      errorLog.setId(UUID.randomUUID());
    }
    errorLog.setCreatedDate(new Date());
    return errorLogEntityCqlRepository.save(
            ErrorLogEntity.builder()
                .id(errorLog.getId())
                .errorLog(errorLog)
                .creationDate(errorLog.getCreatedDate())
                .createdBy(folioExecutionContext.getUserId().toString())
                .jobExecutionId(errorLog.getJobExecutionId())
                .build()
        )
        .getErrorLog();
  }

  /**
   * Updates an error log.
   *
   * @param errorLog the error log to update
   * @return the updated ErrorLog
   */
  public ErrorLog update(ErrorLog errorLog) {
    return save(errorLog);
  }

  /**
   * Deletes an error log by ID.
   *
   * @param id the error log UUID
   * @return true if deleted
   */
  public Boolean deleteById(UUID id) {
    errorLogEntityCqlRepository.deleteById(id);
    return true;
  }

  /**
   * Saves a general error log with the given error message code and job execution ID.
   *
   * @param errorMessageCode the error message code
   * @param jobExecutionId the job execution UUID
   * @return the saved ErrorLog
   */
  public ErrorLog saveGeneralError(String errorMessageCode, UUID jobExecutionId) {
    return save(getGeneralErrorLog(errorMessageCode, jobExecutionId));
  }

  /**
   * Saves a general error log with message values.
   *
   * @param errorMessageCode the error message code
   * @param errorMessageValues the error message values
   * @param jobExecutionId the job execution UUID
   * @return the saved ErrorLog
   */
  public ErrorLog saveGeneralErrorWithMessageValues(
      String errorMessageCode,
      List<String> errorMessageValues,
      UUID jobExecutionId
  ) {
    var errorLog = getGeneralErrorLog(errorMessageCode, jobExecutionId)
        .errorMessageValues(errorMessageValues);
    return save(errorLog);
  }

  /**
   * Saves common export failure errors.
   *
   * @param commonExportStatistic the export statistics
   * @param totalErrors total number of errors
   * @param jobExecutionId the job execution UUID
   */
  public void saveCommonExportFailsErrors(
      CommonExportStatistic commonExportStatistic,
      int totalErrors,
      UUID jobExecutionId
  ) {
    if (!commonExportStatistic.getInvalidUuidFormat().isEmpty()) {
      var errorLog = new ErrorLog();
      errorLog.setId(UUID.randomUUID());
      errorLog.createdDate(new Date());
      errorLog.setJobExecutionId(jobExecutionId);
      var message = String.join(",", commonExportStatistic.getInvalidUuidFormat());
      errorLog.setErrorMessageValues(List.of(message));
      errorLog.setErrorMessageCode(ErrorCode.INVALID_UUID_FORMAT.getCode());
      this.save(errorLog);
    }

    if (!commonExportStatistic.getNotExistUuid().isEmpty()) {
      var errorLog = new ErrorLog();
      errorLog.setId(UUID.randomUUID());
      errorLog.createdDate(new Date());
      errorLog.setJobExecutionId(jobExecutionId);
      var message = String.join(", ", commonExportStatistic.getNotExistUuid());
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

  /**
   * Saves an error log for failed input file reading.
   *
   * @param jobExecutionId the job execution UUID
   */
  public void saveFailedToReadInputFileError(UUID jobExecutionId) {
    var errorLog = new ErrorLog();
    errorLog.setId(UUID.randomUUID());
    errorLog.createdDate(new Date());
    errorLog.setJobExecutionId(jobExecutionId);
    errorLog.setErrorMessageValues(new ArrayList<>());
    errorLog.setErrorMessageCode(ErrorCode.ERROR_READING_FROM_INPUT_FILE.getCode());
    this.save(errorLog);
  }

  /**
   * Saves an error log with affected record and MarcException.
   *
   * @param instance the instance JSON object
   * @param errorMessageCode the error message code
   * @param jobExecutionId the job execution UUID
   * @param marcException the MarcException
   * @return the saved ErrorLog
   */
  public ErrorLog saveWithAffectedRecord(
      JSONObject instance,
      String errorMessageCode,
      UUID jobExecutionId,
      MarcException marcException
  ) {
    String instId = instance.getAsString(ID);
    String hrId = instance.getAsString(HRID);
    String title = instance.getAsString(TITLE);
    String inventoryLink =
        instance.containsKey(DELETED_KEY) && (boolean) instance.get(DELETED_KEY)
            ? EMPTY
            : getInventoryRecordLink() + instId;
    AffectedRecord affectedRecord = new AffectedRecord()
        .id(instId)
        .hrid(hrId)
        .title(title)
        .recordType(RecordTypes.INSTANCE)
        .inventoryRecordLink(inventoryLink);
    if (instId == null) {
      affectedRecord.setId(
          "UUID cannot be determined because record is invalid: field '999' or subfield 'i'"
          + " not found"
      );
    }
    if (hrId == null) {
      affectedRecord.setHrid(
          "HRID cannot be determined because record is invalid: UUID not found"
      );
    }
    if (title == null) {
      affectedRecord.setTitle(
          "Title cannot be determined because record is invalid: UUID not found"
      );
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

  /**
   * Saves an error log with affected record and error message.
   *
   * @param instance the instance JSON object
   * @param errorMessage the error message
   * @param errorMessageCode the error message code
   * @param jobExecutionId the job execution UUID
   * @return the saved ErrorLog
   */
  public ErrorLog saveWithAffectedRecord(
      JSONObject instance,
      String errorMessage,
      String errorMessageCode,
      UUID jobExecutionId
  ) {
    String instId = instance.getAsString(ID);
    String hrId = instance.getAsString(HRID);
    String title = instance.getAsString(TITLE);
    String generalEndOfErrorMsg =
        " cannot be determined because instance record is not found or invalid, but still"
        + " contains more than 1 SRS record";
    if (instId == null) {
      instId = "UUID" + generalEndOfErrorMsg;
    }
    if (hrId == null) {
      hrId = "HRID" + generalEndOfErrorMsg;
    }
    if (title == null) {
      title = "Title" + generalEndOfErrorMsg;
    }
    String inventoryLink =
        instance.containsKey(DELETED_KEY) && (boolean) instance.get(DELETED_KEY)
          ? EMPTY
          : getInventoryRecordLink() + instId;
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

  /**
   * Populates error log for UUIDs not found.
   *
   * @param jobExecutionId the job execution UUID
   * @param notFoundUuids collection of not found UUIDs
   */
  public void populateUuidsNotFoundErrorLog(
      UUID jobExecutionId,
      Collection<String> notFoundUuids
  ) {
    var errorLogs = errorLogEntityCqlRepository.getByJobExecutionIdAndErrorCode(
        jobExecutionId,
        SOME_UUIDS_NOT_FOUND.getCode()
    );
    var newUuids = Collections.singletonList(
        String.join(COMMA_SEPARATOR, notFoundUuids)
            .replace("[", EMPTY)
            .replace("]", EMPTY)
    );
    if (errorLogs.isEmpty()) {
      saveGeneralErrorWithMessageValues(
          SOME_UUIDS_NOT_FOUND.getCode(),
          newUuids,
          jobExecutionId
      );
    } else {
      var errorLog = errorLogs.get(0).getErrorLog();
      var savedUuids = errorLog.getErrorMessageValues().get(0);
      errorLog.setErrorMessageValues(
          Collections.singletonList(savedUuids + COMMA_SEPARATOR + newUuids)
      );
      update(errorLog);
    }
  }

  /**
   * Populates error log for number of UUIDs not found.
   *
   * @param jobExecutionId the job execution UUID
   * @param numberOfNotFoundUuids number of not found UUIDs
   */
  public void populateUuidsNotFoundNumberErrorLog(
      UUID jobExecutionId,
      int numberOfNotFoundUuids
  ) {
    var errorLogs = errorLogEntityCqlRepository.getByJobExecutionIdAndErrorCode(
        jobExecutionId,
        SOME_UUIDS_NOT_FOUND.getCode()
    );
    if (errorLogs.isEmpty()) {
      saveGeneralErrorWithMessageValues(
          SOME_RECORDS_FAILED.getCode(),
          Collections.singletonList(String.valueOf(numberOfNotFoundUuids)),
          jobExecutionId
      );
    } else {
      var errorLog = errorLogs.get(0).getErrorLog();
      var errorMessageValues = errorLog.getErrorMessageValues();
      // get values form errorMessageValues to increase and save to appropriate field
      int updatedNumberOfNotFoundUuids =
          Integer.parseInt(errorMessageValues.get(0)) + numberOfNotFoundUuids;
      errorLog.setErrorMessageValues(
          Collections.singletonList(String.valueOf(updatedNumberOfNotFoundUuids))
      );
      update(errorLog);
    }
  }

  /**
   * Checks if errors by error code are present for a job execution.
   *
   * @param errorCodes list of error codes
   * @param jobExecutionId the job execution UUID
   * @return true if errors are present, false otherwise
   */
  public Boolean isErrorsByErrorCodePresent(
      List<String> errorCodes,
      UUID jobExecutionId
  ) {
    var errorCodesString = errorCodes.size() > 1
        ? "%(" + String.join("|", errorCodes) + ")%"
        : "%" + errorCodes.get(0) + "%";
    return isNotEmpty(
        errorLogEntityCqlRepository.getByJobExecutionIdAndErrorCodes(
            jobExecutionId,
            errorCodesString
        )
    );
  }

  /**
   * Builds a general error log.
   *
   * @param errorMessageCode the error message code
   * @param jobExecutionId the job execution UUID
   * @return ErrorLog
   */
  private ErrorLog getGeneralErrorLog(String errorMessageCode, UUID jobExecutionId) {
    return new ErrorLog()
        .errorMessageCode(errorMessageCode)
        .logLevel(ErrorLog.LogLevelEnum.ERROR)
        .jobExecutionId(jobExecutionId);
  }

  /**
   * Gets the inventory record link from configuration.
   *
   * @return inventory record link string
   */
  private String getInventoryRecordLink() {
    return configurationService.getValue(INVENTORY_RECORD_LINK_KEY);
  }
}
