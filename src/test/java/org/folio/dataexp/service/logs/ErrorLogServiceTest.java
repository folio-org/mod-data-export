package org.folio.dataexp.service.logs;

import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import net.minidev.json.JSONObject;
import org.folio.dataexp.domain.dto.ErrorLog;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.ErrorLogEntity;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.service.CommonExportStatistic;
import org.folio.dataexp.service.ConfigurationService;
import org.folio.dataexp.service.JobExecutionService;
import org.folio.dataexp.service.JobProfileService;
import org.folio.dataexp.util.ErrorCode;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marc4j.MarcException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class ErrorLogServiceTest {

  private static final String LONG_MARC_RECORD_MESSAGE =
      "Record is too long to be a valid MARC binary record";
  @Mock private ErrorLogEntityCqlRepository errorLogEntityCqlRepository;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private ConfigurationService configurationService;
  @Mock private ObjectMapper objectMapper;
  @Mock private JobExecutionService jobExecutionService;
  @Mock private JobProfileService jobProfileService;
  @InjectMocks private ErrorLogService errorLogService;

  @Test
  void getErrorLogsByQueryTest() {
    var errorId = UUID.randomUUID();
    var errorLogEntity = new ErrorLogEntity();
    var errorLog = new ErrorLog();
    errorLog.setId(errorId);
    errorLogEntity.setErrorLog(errorLog);
    var page = new PageImpl<>(List.of(errorLogEntity));
    var query = "query";

    when(errorLogEntityCqlRepository.findByCql(eq(query), isA(OffsetRequest.class)))
        .thenReturn(page);

    var collection = errorLogService.getErrorLogsByQuery(query, 0, 1);

    assertEquals(1, collection.getTotalRecords());
    var errorLogs = collection.getErrorLogs();

    assertEquals(errorLog.getId(), errorLogs.get(0).getId());
  }

  @Test
  void getByQueryTest() {
    var errorId = UUID.randomUUID();
    var errorLogEntity = new ErrorLogEntity();
    var errorLog = new ErrorLog();
    errorLog.setId(errorId);
    errorLogEntity.setErrorLog(errorLog);
    var page = new PageImpl<>(List.of(errorLogEntity));
    var query = "query";

    when(errorLogEntityCqlRepository.findByCql(eq(query), isA(OffsetRequest.class)))
        .thenReturn(page);

    var errors = errorLogService.getByQuery(query);

    assertEquals(errorLog.getId(), errors.get(0).getId());
  }

  @Test
  void saveTest() {
    var errorLog = new ErrorLog();

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(jobExecutionService.getById(any())).thenReturn(new JobExecution());
    errorLogService.save(errorLog);

    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            isA(UUID.class), any(), isA(java.util.Date.class), isA(String.class), any(), any());
  }

  @Test
  void updateTest() {
    var errorLog = new ErrorLog();

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(jobExecutionService.getById(any())).thenReturn(new JobExecution());
    errorLogService.save(errorLog);

    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            isA(UUID.class), any(), isA(java.util.Date.class), isA(String.class), any(), any());
  }

  @Test
  void deleteByIdTest() {
    errorLogService.deleteById(UUID.randomUUID());
    verify(errorLogEntityCqlRepository).deleteById(isA(UUID.class));
  }

  @Test
  void saveGeneralErrorTest() {

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(jobExecutionService.getById(any())).thenReturn(new JobExecution());
    errorLogService.saveGeneralError("errorCode", UUID.randomUUID());

    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            isA(UUID.class),
            any(),
            isA(java.util.Date.class),
            isA(String.class),
            isA(UUID.class),
            any());
  }

  @Test
  @SneakyThrows
  void saveCommonExportFailsErrorsTest() {
    var commonFails = new CommonExportStatistic();
    var notExistUuid = UUID.randomUUID();
    commonFails.incrementDuplicatedUuid();
    commonFails.addToInvalidUuidFormat("abs");
    commonFails.addToNotExistUuidAll(List.of(notExistUuid));
    var jobExecutionId = UUID.randomUUID();

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(objectMapper.writeValueAsString(isA(ErrorLog.class))).thenReturn("jsonString");
    when(jobExecutionService.getById(jobExecutionId))
        .thenReturn(new JobExecution().id(jobExecutionId).jobProfileId(UUID.randomUUID()));

    errorLogService.saveCommonExportFailsErrors(commonFails, 3, jobExecutionId);
    verify(errorLogEntityCqlRepository, times(3))
        .insertIfNotExists(
            isA(UUID.class),
            isA(String.class),
            isA(java.util.Date.class),
            isA(String.class),
            isA(UUID.class),
            any());
  }

  @Test
  @SneakyThrows
  void saveFailedToReadInputFileErrorTest() {

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    // Mock objectMapper.readValue to return a non-null ErrorLog
    var expectedErrorLog = new ErrorLog();
    expectedErrorLog.setErrorMessageCode(ErrorCode.ERROR_READING_FROM_INPUT_FILE.getCode());
    when(objectMapper.readValue(any(String.class), eq(ErrorLog.class)))
        .thenReturn(expectedErrorLog);
    var jobExecutionId = UUID.randomUUID();
    when(objectMapper.writeValueAsString(isA(ErrorLog.class))).thenReturn("jsonString");
    when(jobExecutionService.getById(jobExecutionId))
        .thenReturn(new JobExecution().id(jobExecutionId).jobProfileId(UUID.randomUUID()));

    errorLogService.saveFailedToReadInputFileError(jobExecutionId);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            isA(UUID.class),
            captor.capture(),
            isA(java.util.Date.class),
            isA(String.class),
            isA(UUID.class),
            any());

    var errorLog = objectMapper.readValue(captor.getValue(), ErrorLog.class);
    assertEquals(ErrorCode.ERROR_READING_FROM_INPUT_FILE.getCode(), errorLog.getErrorMessageCode());
  }

  @Test
  @SneakyThrows
  void saveWithAffectedRecordMarcExceptionErrorTest() {

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    // Mock objectMapper.readValue to return a non-null ErrorLog
    var expectedErrorLog = new ErrorLog();
    expectedErrorLog.setErrorMessageCode(ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode());
    when(objectMapper.readValue(any(String.class), eq(ErrorLog.class)))
        .thenReturn(expectedErrorLog);
    var jobExecutionId = UUID.randomUUID();
    when(objectMapper.writeValueAsString(isA(ErrorLog.class))).thenReturn("jsonString");
    when(jobExecutionService.getById(jobExecutionId))
        .thenReturn(new JobExecution().id(jobExecutionId).jobProfileId(UUID.randomUUID()));

    errorLogService.saveWithAffectedRecord(
        new JSONObject(),
        ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(),
        jobExecutionId,
        new MarcException());
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            isA(UUID.class),
            captor.capture(),
            isA(java.util.Date.class),
            isA(String.class),
            isA(UUID.class),
            any());

    var errorLog = objectMapper.readValue(captor.getValue(), ErrorLog.class);
    assertEquals(
        ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(),
        errorLog.getErrorMessageCode());
  }

  @Test
  @SneakyThrows
  void saveWithAffectedRecordErrorTest() {

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    // Mock objectMapper.readValue to return a non-null ErrorLog
    var expectedErrorLog = new ErrorLog();
    expectedErrorLog.setErrorMessageCode(ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode());
    expectedErrorLog.setErrorMessageValues(List.of(LONG_MARC_RECORD_MESSAGE));
    var jobExecutionId = UUID.randomUUID();
    expectedErrorLog.setJobExecutionId(jobExecutionId);
    when(objectMapper.readValue(any(String.class), eq(ErrorLog.class)))
        .thenReturn(expectedErrorLog);
    when(objectMapper.writeValueAsString(isA(ErrorLog.class))).thenReturn("jsonString");
    when(jobExecutionService.getById(jobExecutionId))
        .thenReturn(new JobExecution().id(jobExecutionId).jobProfileId(UUID.randomUUID()));

    errorLogService.saveWithAffectedRecord(
        new JSONObject(),
        LONG_MARC_RECORD_MESSAGE,
        ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(),
        jobExecutionId);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            isA(UUID.class),
            captor.capture(),
            isA(java.util.Date.class),
            isA(String.class),
            isA(UUID.class),
            any());

    var errorLog = objectMapper.readValue(captor.getValue(), ErrorLog.class);
    assertEquals(
        ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(),
        errorLog.getErrorMessageCode());
    assertEquals(LONG_MARC_RECORD_MESSAGE, errorLog.getErrorMessageValues().getFirst());
  }
}
