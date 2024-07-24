package org.folio.dataexp.service.logs;

import net.minidev.json.JSONObject;
import org.folio.dataexp.domain.dto.ErrorLog;
import org.folio.dataexp.domain.entity.ErrorLogEntity;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.service.CommonExportStatistic;
import org.folio.dataexp.service.ConfigurationService;
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

import java.util.List;
import java.util.UUID;

import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorLogServiceTest {

  private static final String LONG_MARC_RECORD_MESSAGE = "Record is too long to be a valid MARC binary record";
  @Mock
  private ErrorLogEntityCqlRepository errorLogEntityCqlRepository;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private ConfigurationService configurationService;
  @InjectMocks
  private ErrorLogService errorLogService;

  @Test
  void getErrorLogsByQueryTest() {
    var errorId = UUID.randomUUID();
    var errorLogEntity = new ErrorLogEntity();
    var errorLog = new ErrorLog();
    errorLog.setId(errorId);
    errorLogEntity.setErrorLog(errorLog);
    var page = new PageImpl<>(List.of(errorLogEntity));
    var query = "query";

    when(errorLogEntityCqlRepository.findByCql(eq(query), isA(OffsetRequest.class))).thenReturn(page);

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

    when(errorLogEntityCqlRepository.findByCql(eq(query), isA(OffsetRequest.class))).thenReturn(page);

    var errors = errorLogService.getByQuery(query);

    assertEquals(errorLog.getId(), errors.get(0).getId());
  }

  @Test
  void saveTest() {
    var errorLog = new ErrorLog();

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(errorLogEntityCqlRepository.save(isA(ErrorLogEntity.class))).thenReturn(new ErrorLogEntity());
    errorLogService.save(errorLog);

    verify(errorLogEntityCqlRepository).save(isA(ErrorLogEntity.class));
  }

  @Test
  void updateTest() {
    var errorLog = new ErrorLog();

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(errorLogEntityCqlRepository.save(isA(ErrorLogEntity.class))).thenReturn(new ErrorLogEntity());
    errorLogService.save(errorLog);

    verify(errorLogEntityCqlRepository).save(isA(ErrorLogEntity.class));
  }

  @Test
  void deleteByIdTest() {
    errorLogService.deleteById(UUID.randomUUID());
    verify(errorLogEntityCqlRepository).deleteById(isA(UUID.class));
  }

  @Test
  void saveGeneralErrorTest() {

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(errorLogEntityCqlRepository.save(isA(ErrorLogEntity.class))).thenReturn(new ErrorLogEntity());
    errorLogService.saveGeneralError("errorCode", UUID.randomUUID());

    verify(errorLogEntityCqlRepository).save(isA(ErrorLogEntity.class));
  }

  @Test
  void saveCommonExportFailsErrorsTest() {
    var jobExecutionId = UUID.randomUUID();
    var commonFails = new CommonExportStatistic();
    var notExistUUID = UUID.randomUUID();
    commonFails.incrementDuplicatedUUID();
    commonFails.addToInvalidUUIDFormat("abs");
    commonFails.addToNotExistUUIDAll(List.of(notExistUUID));

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(errorLogEntityCqlRepository.save(isA(ErrorLogEntity.class))).thenReturn(new ErrorLogEntity());

    errorLogService.saveCommonExportFailsErrors(commonFails, 3, jobExecutionId);
    verify(errorLogEntityCqlRepository, times(3)).save(isA(ErrorLogEntity.class));
  }

  @Test
  void saveFailedToReadInputFileErrorTest() {
    var jobExecutionId = UUID.randomUUID();
    ArgumentCaptor<ErrorLogEntity> captor = ArgumentCaptor.forClass(ErrorLogEntity.class);

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(errorLogEntityCqlRepository.save(isA(ErrorLogEntity.class))).thenReturn(new ErrorLogEntity());

    errorLogService.saveFailedToReadInputFileError(jobExecutionId);
    verify(errorLogEntityCqlRepository).save(captor.capture());

    var errorLogEntity = captor.getValue();
    var errorLog = errorLogEntity.getErrorLog();
    assertEquals(ErrorCode.ERROR_READING_FROM_INPUT_FILE.getCode(), errorLog.getErrorMessageCode());
  }

  @Test
  void saveWithAffectedRecordMarcExceptionErrorTest() {
    var jobExecutionId = UUID.randomUUID();
    ArgumentCaptor<ErrorLogEntity> captor = ArgumentCaptor.forClass(ErrorLogEntity.class);

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(errorLogEntityCqlRepository.save(isA(ErrorLogEntity.class))).thenReturn(new ErrorLogEntity());

    errorLogService.saveWithAffectedRecord(new JSONObject(), ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(), jobExecutionId, new MarcException());
    verify(errorLogEntityCqlRepository).save(captor.capture());

    var errorLogEntity = captor.getValue();
    var errorLog = errorLogEntity.getErrorLog();
    assertEquals(ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(), errorLog.getErrorMessageCode());
  }

  @Test
  void saveWithAffectedRecordErrorTest() {
    var jobExecutionId = UUID.randomUUID();
    ArgumentCaptor<ErrorLogEntity> captor = ArgumentCaptor.forClass(ErrorLogEntity.class);

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(errorLogEntityCqlRepository.save(isA(ErrorLogEntity.class))).thenReturn(new ErrorLogEntity());

    errorLogService.saveWithAffectedRecord(new JSONObject(), LONG_MARC_RECORD_MESSAGE, ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(), jobExecutionId);
    verify(errorLogEntityCqlRepository).save(captor.capture());

    var errorLogEntity = captor.getValue();
    var errorLog = errorLogEntity.getErrorLog();
    assertEquals(ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(), errorLog.getErrorMessageCode());
    assertEquals(LONG_MARC_RECORD_MESSAGE, errorLog.getErrorMessageValues().get(0));
  }
}
