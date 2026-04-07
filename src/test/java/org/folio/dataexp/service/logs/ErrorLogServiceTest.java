package org.folio.dataexp.service.logs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.service.ConfigurationService.INVENTORY_RECORD_LINK_KEY;
import static org.folio.dataexp.service.export.Constants.DELETED_KEY;
import static org.folio.dataexp.service.export.Constants.HRID_KEY;
import static org.folio.dataexp.service.export.Constants.ID_KEY;
import static org.folio.dataexp.service.export.Constants.TITLE_KEY;
import static org.folio.dataexp.util.Constants.QUERY_CQL_ALL_RECORDS;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC;
import static org.folio.dataexp.util.ErrorCode.SOME_RECORDS_FAILED;
import static org.folio.dataexp.util.ErrorCode.SOME_UUIDS_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import net.minidev.json.JSONObject;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.ErrorLog;
import org.folio.dataexp.domain.dto.ErrorLogCollection;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.RecordTypes;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.marc4j.MarcException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

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

  @Captor private ArgumentCaptor<OffsetRequest> offsetRequestCaptor;

  @Captor private ArgumentCaptor<ErrorLog> errorLogCaptor;

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

  @Test
  @TestMate(name = "TestMate-8f00eb0c345d8b7cac8469c9264c5fa3")
  @SneakyThrows
  void saveWithAffectedRecordShouldSetEmptyInventoryLinkForDeletedRecord() {
    // Given
    var instanceId = UUID.fromString("b890b134-736f-4e5a-8351-9c608f3a3a59");
    var instanceHrid = "hrid_1";
    var instanceTitle = "title_1";
    var instance = new JSONObject();
    instance.put(ID_KEY, instanceId.toString());
    instance.put(HRID_KEY, instanceHrid);
    instance.put(TITLE_KEY, instanceTitle);
    instance.put(DELETED_KEY, true);
    var jobExecutionId = UUID.fromString("a890b134-736f-4e5a-8351-9c608f3a3a58");
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(jobExecutionService.getById(jobExecutionId))
        .thenReturn(new JobExecution().id(jobExecutionId).jobProfileId(UUID.randomUUID()));
    // Use a real ObjectMapper to serialize the captured object, ensuring valid JSON.
    when(objectMapper.writeValueAsString(errorLogCaptor.capture()))
        .thenAnswer(invocation -> new ObjectMapper().writeValueAsString(invocation.getArgument(0)));
    // When
    var errorMessageCode = "some_error_code";
    var marcExceptionMessage = "marc_exception_message";
    var marcException = new MarcException(marcExceptionMessage);
    errorLogService.saveWithAffectedRecord(
        instance, errorMessageCode, jobExecutionId, marcException);
    // Then
    // We only need to verify the repository call happened; the object is captured for assertions.
    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            isA(UUID.class),
            anyString(),
            isA(Date.class),
            isA(String.class),
            isA(UUID.class),
            any());
    var actualErrorLog = errorLogCaptor.getValue();
    var affectedRecord = actualErrorLog.getAffectedRecord();
    assertEquals("", affectedRecord.getInventoryRecordLink());
    assertEquals(instanceId.toString(), affectedRecord.getId());
    assertEquals(instanceHrid, affectedRecord.getHrid());
    assertEquals(instanceTitle, affectedRecord.getTitle());
    verify(configurationService, never()).getValue(anyString());
  }

  @Test
  @TestMate(name = "TestMate-8df560da94786686fc4858ef57a9e5ec")
  @SneakyThrows
  void saveGeneralErrorWithMessageValuesTest() {
    // Given
    var jobExecutionId = UUID.fromString("a890b134-736f-4e5a-8351-9c608f3a3a58");
    var userId = UUID.fromString("b890b134-736f-4e5a-8351-9c608f3a3a59");
    var jobProfileId = UUID.fromString("c890b134-736f-4e5a-8351-9c608f3a3a50");
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(jobExecutionService.getById(jobExecutionId))
        .thenReturn(new JobExecution().id(jobExecutionId).jobProfileId(jobProfileId));
    when(jobProfileService.jobProfileExists(jobProfileId)).thenReturn(true);
    when(objectMapper.writeValueAsString(errorLogCaptor.capture()))
        .thenAnswer(invocation -> new ObjectMapper().writeValueAsString(invocation.getArgument(0)));
    var errorMessageCode = ErrorCode.SOME_UUIDS_NOT_FOUND.getCode();
    var errorMessageValues = List.of("uuid1", "uuid2");
    // When
    errorLogService.saveGeneralErrorWithMessageValues(
        errorMessageCode, errorMessageValues, jobExecutionId);
    // Then
    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            isA(UUID.class),
            anyString(),
            isA(Date.class),
            eq(userId.toString()),
            eq(jobExecutionId),
            eq(jobProfileId));
    var capturedErrorLog = errorLogCaptor.getValue();
    assertEquals(jobExecutionId, capturedErrorLog.getJobExecutionId());
    assertEquals(errorMessageCode, capturedErrorLog.getErrorMessageCode());
    assertEquals(errorMessageValues, capturedErrorLog.getErrorMessageValues());
    assertEquals(ErrorLog.LogLevelEnum.ERROR, capturedErrorLog.getLogLevel());
  }

  @Test
  @TestMate(name = "TestMate-1da6299af83636050ba587663941e69d")
  @SneakyThrows
  void populateUuidsNotFoundErrorLogShouldCorrectlyFormatSingleUuid() {
    // Given
    var jobExecutionId = UUID.fromString("a890b134-736f-4e5a-8351-9c608f3a3a58");
    var userId = UUID.fromString("b890b134-736f-4e5a-8351-9c608f3a3a59");
    var jobProfileId = UUID.fromString("c890b134-736f-4e5a-8351-9c608f3a3a50");
    when(errorLogEntityCqlRepository.getByJobExecutionIdAndErrorCode(
            jobExecutionId, SOME_UUIDS_NOT_FOUND.getCode()))
        .thenReturn(new ArrayList<>());
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(jobExecutionService.getById(jobExecutionId))
        .thenReturn(new JobExecution().id(jobExecutionId).jobProfileId(jobProfileId));
    when(jobProfileService.jobProfileExists(jobProfileId)).thenReturn(true);
    when(objectMapper.writeValueAsString(any(ErrorLog.class)))
        .thenAnswer(
            invocation -> {
              var realObjectMapper = new ObjectMapper();
              return realObjectMapper.writeValueAsString(invocation.getArgument(0));
            });
    var notFoundUuid = "a1b2c3d4-e5f6-7890-1234-567890abcdef";
    var notFoundUuids = List.of(notFoundUuid);
    var errorLogCaptorLocal = ArgumentCaptor.forClass(String.class);
    // When
    errorLogService.populateUuidsNotFoundErrorLog(jobExecutionId, notFoundUuids);
    // Then
    verify(errorLogEntityCqlRepository)
        .getByJobExecutionIdAndErrorCode(jobExecutionId, SOME_UUIDS_NOT_FOUND.getCode());
    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            isA(UUID.class),
            errorLogCaptorLocal.capture(),
            isA(Date.class),
            eq(userId.toString()),
            eq(jobExecutionId),
            eq(jobProfileId));
    var realObjectMapper = new ObjectMapper();
    var capturedErrorLog =
        realObjectMapper.readValue(errorLogCaptorLocal.getValue(), ErrorLog.class);
    assertEquals(SOME_UUIDS_NOT_FOUND.getCode(), capturedErrorLog.getErrorMessageCode());
    assertEquals(jobExecutionId, capturedErrorLog.getJobExecutionId());
    assertEquals(1, capturedErrorLog.getErrorMessageValues().size());
    assertEquals(notFoundUuid, capturedErrorLog.getErrorMessageValues().get(0));
  }

  @Test
  @TestMate(name = "TestMate-38d9d6eaaa9bb23496731f9cc7ef7a12")
  @SneakyThrows
  void populateUuidsNotFoundErrorLogShouldHandleMultipleNewUuids() {
    // Given
    var jobExecutionId = UUID.fromString("a890b134-736f-4e5a-8351-9c608f3a3a58");
    var userId = UUID.fromString("b890b134-736f-4e5a-8351-9c608f3a3a59");
    var jobProfileId = UUID.fromString("c890b134-736f-4e5a-8351-9c608f3a3a50");
    when(errorLogEntityCqlRepository.getByJobExecutionIdAndErrorCode(
            jobExecutionId, SOME_UUIDS_NOT_FOUND.getCode()))
        .thenReturn(new ArrayList<>());
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(jobExecutionService.getById(jobExecutionId))
        .thenReturn(new JobExecution().id(jobExecutionId).jobProfileId(jobProfileId));
    when(jobProfileService.jobProfileExists(jobProfileId)).thenReturn(true);
    var uuid1 = "d1b2c3d4-e5f6-7890-1234-567890abcdef";
    var uuid2 = "e1b2c3d4-e5f6-7890-1234-567890fedcba";
    var notFoundUuids = List.of(uuid1, uuid2);
    when(objectMapper.writeValueAsString(errorLogCaptor.capture())).thenReturn("jsonString");
    // When
    errorLogService.populateUuidsNotFoundErrorLog(jobExecutionId, notFoundUuids);
    // Then
    verify(errorLogEntityCqlRepository)
        .getByJobExecutionIdAndErrorCode(jobExecutionId, SOME_UUIDS_NOT_FOUND.getCode());
    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            isA(UUID.class),
            eq("jsonString"),
            isA(Date.class),
            eq(userId.toString()),
            eq(jobExecutionId),
            eq(jobProfileId));
    var capturedErrorLog = errorLogCaptor.getValue();
    assertEquals(SOME_UUIDS_NOT_FOUND.getCode(), capturedErrorLog.getErrorMessageCode());
    assertEquals(jobExecutionId, capturedErrorLog.getJobExecutionId());
    assertEquals(ErrorLog.LogLevelEnum.ERROR, capturedErrorLog.getLogLevel());

    var messageValues = capturedErrorLog.getErrorMessageValues();
    assertEquals(1, messageValues.size());

    var formattedUuids = messageValues.get(0);
    assertEquals(uuid1 + ", " + uuid2, formattedUuids);
    assertFalse(formattedUuids.contains("["));
    assertFalse(formattedUuids.contains("]"));
  }

  @Test
  @TestMate(name = "TestMate-da84ac9509d5a813a5f37509d1876f9f")
  @SneakyThrows
  void populateUuidsNotFoundNumberErrorLogShouldUpdateExistingErrorLogWhenFound() {
    // Given
    var jobExecutionId = UUID.fromString("a890b134-736f-4e5a-8351-9c608f3a3a58");
    var userId = UUID.fromString("b890b134-736f-4e5a-8351-9c608f3a3a59");
    var initialCount = "5";
    var errorLog =
        new ErrorLog()
            .errorMessageValues(Collections.singletonList(initialCount))
            .jobExecutionId(jobExecutionId)
            .errorMessageCode(SOME_UUIDS_NOT_FOUND.getCode());
    var errorLogEntity = new ErrorLogEntity();
    errorLogEntity.setErrorLog(errorLog);
    when(errorLogEntityCqlRepository.getByJobExecutionIdAndErrorCode(
            jobExecutionId, SOME_UUIDS_NOT_FOUND.getCode()))
        .thenReturn(Collections.singletonList(errorLogEntity));
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    var jobProfileId = UUID.fromString("c890b134-736f-4e5a-8351-9c608f3a3a50");
    when(jobExecutionService.getById(jobExecutionId))
        .thenReturn(new JobExecution().id(jobExecutionId).jobProfileId(jobProfileId));
    when(jobProfileService.jobProfileExists(jobProfileId)).thenReturn(true);

    when(objectMapper.writeValueAsString(errorLogCaptor.capture())).thenReturn("jsonString");
    var incrementValue = 10;
    // When
    errorLogService.populateUuidsNotFoundNumberErrorLog(jobExecutionId, incrementValue);
    // Then
    verify(errorLogEntityCqlRepository)
        .getByJobExecutionIdAndErrorCode(jobExecutionId, SOME_UUIDS_NOT_FOUND.getCode());
    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            isA(UUID.class),
            eq("jsonString"),
            isA(Date.class),
            eq(userId.toString()),
            eq(jobExecutionId),
            eq(jobProfileId));
    var capturedErrorLog = errorLogCaptor.getValue();
    var expectedTotal = "15";
    assertEquals(expectedTotal, capturedErrorLog.getErrorMessageValues().get(0));
  }

  @Test
  @TestMate(name = "TestMate-45e2650784d528d5504331f8b75cb393")
  void isErrorsByErrorCodePresentShouldReturnTrueWhenSingleErrorCodeExists() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var errorCode = "error.uuidsNotFound";
    var errorCodes = List.of(errorCode);
    var expectedPattern = "%" + errorCode + "%";
    var errorLogEntity = new ErrorLogEntity();
    when(errorLogEntityCqlRepository.getByJobExecutionIdAndErrorCodes(
            jobExecutionId, expectedPattern))
        .thenReturn(List.of(errorLogEntity));
    // When
    var result = errorLogService.isErrorsByErrorCodePresent(errorCodes, jobExecutionId);
    // Then
    assertTrue(result);
    verify(errorLogEntityCqlRepository)
        .getByJobExecutionIdAndErrorCodes(jobExecutionId, expectedPattern);
  }

  @Test
  @TestMate(name = "TestMate-bde556279710d3e8ae8f4f446eb3feb2")
  void isErrorsByErrorCodePresentShouldReturnTrueWhenMultipleErrorCodesExist() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var errorCodes = List.of("ERR_1", "ERR_2");
    var expectedPattern = "%(ERR_1|ERR_2)%";
    var errorLogEntity = new ErrorLogEntity();

    when(errorLogEntityCqlRepository.getByJobExecutionIdAndErrorCodes(
            jobExecutionId, expectedPattern))
        .thenReturn(List.of(errorLogEntity));
    // When
    var result = errorLogService.isErrorsByErrorCodePresent(errorCodes, jobExecutionId);
    // Then
    assertTrue(result);
    verify(errorLogEntityCqlRepository)
        .getByJobExecutionIdAndErrorCodes(jobExecutionId, expectedPattern);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void getErrorLogsByQueryWhenQueryIsEmptyShouldUseAllRecordsConst(String query) {
    // TestMate-08ee9a7528ddf18f0a322d5db9a6b61a
    // Given
    int offset = 0;
    int limit = 10;
    var offsetRequest = OffsetRequest.of(offset, limit);
    when(errorLogEntityCqlRepository.findByCql(QUERY_CQL_ALL_RECORDS, offsetRequest))
        .thenReturn(Page.empty(offsetRequest));
    // When
    ErrorLogCollection result = errorLogService.getErrorLogsByQuery(query, offset, limit);
    // Then
    verify(errorLogEntityCqlRepository).findByCql(QUERY_CQL_ALL_RECORDS, offsetRequest);
    assertThat(result.getErrorLogs()).isEmpty();
    assertThat(result.getTotalRecords()).isZero();
  }

  @Test
  @TestMate(name = "TestMate-a8762a41e0a8a8572d08fe8c5af25670")
  void getErrorLogsByQueryShouldCorrectlyHandlePagination() {
    // Given
    var query = "errorCode==SOME_CODE";
    int offset = 20;
    int limit = 10;
    var expectedOffsetRequest = OffsetRequest.of(offset, limit);
    when(errorLogEntityCqlRepository.findByCql(query, expectedOffsetRequest))
        .thenReturn(Page.empty(expectedOffsetRequest));
    // When
    ErrorLogCollection result = errorLogService.getErrorLogsByQuery(query, offset, limit);
    // Then
    verify(errorLogEntityCqlRepository).findByCql(eq(query), offsetRequestCaptor.capture());
    var capturedRequest = offsetRequestCaptor.getValue();
    assertThat(capturedRequest.getOffset()).isEqualTo(offset);
    assertThat(capturedRequest.getPageSize()).isEqualTo(limit);
    assertThat(result.getErrorLogs()).isEmpty();
    assertThat(result.getTotalRecords()).isZero();
  }

  @Test
  @TestMate(name = "TestMate-4e4e8f0953b84039d742a9feff53342a")
  void saveShouldPreserveExistingId() throws Exception {
    // Given
    var existingId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var errorLog = new ErrorLog();
    errorLog.setId(existingId);
    var jsonString = "{}";
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(objectMapper.writeValueAsString(any(ErrorLog.class))).thenReturn(jsonString);
    // When
    var result = errorLogService.save(errorLog);
    // Then
    assertThat(result.getId()).isEqualTo(existingId);
    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            eq(existingId), eq(jsonString), isA(Date.class), eq(userId.toString()), any(), any());
  }

  @Test
  @TestMate(name = "TestMate-3c8fbfd82a8e898ea55dfc3787222920")
  void saveShouldPopulateJobProfileIdWhenJobProfileExists() throws JacksonException {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobProfileId = UUID.fromString("00000000-0000-0000-0000-000000000002");

    var errorLog = new ErrorLog();
    errorLog.setJobExecutionId(jobExecutionId);

    var jobExecution = new JobExecution();
    jobExecution.setId(jobExecutionId);
    jobExecution.setJobProfileId(jobProfileId);

    var userId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    when(jobProfileService.jobProfileExists(jobProfileId)).thenReturn(true);
    when(objectMapper.writeValueAsString(any(ErrorLog.class))).thenReturn("{}");
    // When
    var result = errorLogService.save(errorLog);
    // Then
    verify(jobExecutionService).getById(jobExecutionId);
    verify(jobProfileService).jobProfileExists(jobProfileId);
    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            isA(UUID.class),
            anyString(),
            isA(Date.class),
            eq(userId.toString()),
            eq(jobExecutionId),
            eq(jobProfileId));
    assertThat(result.getJobExecutionId()).isEqualTo(jobExecutionId);
  }

  @Test
  @TestMate(name = "TestMate-2f65cdc76c6dd64705a2abf92c614c1e")
  void saveShouldSetJobProfileIdToNullWhenProfileDoesNotExist() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobProfileId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var errorLog = new ErrorLog();
    errorLog.setJobExecutionId(jobExecutionId);
    var jobExecution = new JobExecution();
    jobExecution.setId(jobExecutionId);
    jobExecution.setJobProfileId(jobProfileId);
    var jsonString = "{}";
    var userId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    when(jobProfileService.jobProfileExists(jobProfileId)).thenReturn(false);
    when(objectMapper.writeValueAsString(any(ErrorLog.class))).thenReturn(jsonString);
    // When
    var result = errorLogService.save(errorLog);
    // Then
    verify(jobExecutionService).getById(jobExecutionId);
    verify(jobProfileService).jobProfileExists(jobProfileId);
    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            isA(UUID.class),
            eq(jsonString),
            isA(Date.class),
            eq(userId.toString()),
            eq(jobExecutionId),
            eq(null));
    assertThat(result.getJobExecutionId()).isEqualTo(jobExecutionId);
  }

  @Test
  @TestMate(name = "TestMate-a4b379a312ca773eb2bb2b3f3746f64b")
  void testSaveWithAffectedRecordShouldConstructInventoryLinkUsingConfiguration() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobProfileId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    var instanceId = "00000000-0000-0000-0000-000000000004";
    var hrid = "inst001";
    var title = "Test Title";
    var instance = new JSONObject();
    instance.put(ID_KEY, instanceId);
    instance.put(HRID_KEY, hrid);
    instance.put(TITLE_KEY, title);
    var jobExecution = new JobExecution().id(jobExecutionId).jobProfileId(jobProfileId);
    var userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    var baseUrl = "https://folio-testing.edu/inventory/view/";
    when(configurationService.getValue(INVENTORY_RECORD_LINK_KEY)).thenReturn(baseUrl);
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    when(jobProfileService.jobProfileExists(jobProfileId)).thenReturn(true);
    when(objectMapper.writeValueAsString(errorLogCaptor.capture())).thenReturn("{}");
    var errorMessage = "Sample error message";
    var errorCode = "SAMPLE_ERROR_CODE";
    // When
    errorLogService.saveWithAffectedRecord(instance, errorMessage, errorCode, jobExecutionId);
    // Then
    verify(configurationService).getValue(INVENTORY_RECORD_LINK_KEY);
    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            isA(UUID.class),
            anyString(),
            isA(Date.class),
            eq(userId.toString()),
            eq(jobExecutionId),
            eq(jobProfileId));
    var capturedErrorLog = errorLogCaptor.getValue();
    var affectedRecord = capturedErrorLog.getAffectedRecord();
    assertEquals(baseUrl + instanceId, affectedRecord.getInventoryRecordLink());
    assertEquals(instanceId, affectedRecord.getId());
    assertEquals(hrid, affectedRecord.getHrid());
    assertEquals(title, affectedRecord.getTitle());
    assertEquals(RecordTypes.INSTANCE, affectedRecord.getRecordType());
    assertEquals(errorCode, capturedErrorLog.getErrorMessageCode());
    assertEquals(List.of(errorMessage), capturedErrorLog.getErrorMessageValues());
  }

  @Test
  @TestMate(name = "TestMate-9f3d41b44a8e1ead6b08dc11717af598")
  @SneakyThrows
  void populateUuidsNotFoundNumberErrorLogShouldCreateNewErrorLogWhenNoExistingFound() {
    // Given
    var jobExecutionId = UUID.fromString("a890b134-736f-4e5a-8351-9c608f3a3a58");
    var userId = UUID.fromString("b890b134-736f-4e5a-8351-9c608f3a3a59");
    var jobProfileId = UUID.fromString("c890b134-736f-4e5a-8351-9c608f3a3a50");
    when(errorLogEntityCqlRepository.getByJobExecutionIdAndErrorCode(
            jobExecutionId, SOME_UUIDS_NOT_FOUND.getCode()))
        .thenReturn(new ArrayList<>());
    when(folioExecutionContext.getUserId()).thenReturn(userId);

    var jobExecution = new JobExecution().id(jobExecutionId).jobProfileId(jobProfileId);
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    when(jobProfileService.jobProfileExists(jobProfileId)).thenReturn(true);
    when(objectMapper.writeValueAsString(errorLogCaptor.capture())).thenReturn("{}");
    var numberOfNotFoundUuids = 10;
    // When
    errorLogService.populateUuidsNotFoundNumberErrorLog(jobExecutionId, numberOfNotFoundUuids);
    // Then
    verify(errorLogEntityCqlRepository)
        .getByJobExecutionIdAndErrorCode(jobExecutionId, SOME_UUIDS_NOT_FOUND.getCode());
    verify(errorLogEntityCqlRepository)
        .insertIfNotExists(
            isA(UUID.class),
            anyString(),
            isA(Date.class),
            eq(userId.toString()),
            eq(jobExecutionId),
            eq(jobProfileId));
    var capturedErrorLog = errorLogCaptor.getValue();
    assertEquals(SOME_RECORDS_FAILED.getCode(), capturedErrorLog.getErrorMessageCode());
    assertEquals(jobExecutionId, capturedErrorLog.getJobExecutionId());
    assertEquals(1, capturedErrorLog.getErrorMessageValues().size());
    assertEquals("10", capturedErrorLog.getErrorMessageValues().get(0));
  }

    @Test
  void saveShouldSetJobProfileIdToNullWhenJobExecutionIdIsNull() throws JacksonException {
    // TestMate-bf633de4dddf640cff5c1f4474b0118f
    // Given
    var userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var errorLogId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var dummyJson = "{}";
    var errorLog = new ErrorLog();
    errorLog.setId(errorLogId);
    errorLog.setJobExecutionId(null);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(objectMapper.writeValueAsString(any(ErrorLog.class))).thenReturn(dummyJson);
    // When
    var result = errorLogService.save(errorLog);
    // Then
    verify(errorLogEntityCqlRepository).insertIfNotExists(
        eq(errorLogId),
        eq(dummyJson),
        isA(Date.class),
        eq(userId.toString()),
        eq(null),
        eq(null)
    );
    verify(jobExecutionService, never()).getById(any(UUID.class));
    verify(jobProfileService, never()).jobProfileExists(any(UUID.class));
    assertThat(result.getId()).isEqualTo(errorLogId);
    assertThat(result.getJobExecutionId()).isNull();
  }
}
