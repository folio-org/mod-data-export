package org.folio.dataexp.service.logs;

import org.folio.dataexp.domain.dto.ErrorLog;
import org.folio.dataexp.domain.entity.ErrorLogEntity;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorLogServiceTest {

  @Mock
  private ErrorLogEntityCqlRepository errorLogEntityCqlRepository;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @InjectMocks
  private ErrorLogService errorLogService;

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
    var errorLog = new ErrorLog();

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(errorLogEntityCqlRepository.save(isA(ErrorLogEntity.class))).thenReturn(new ErrorLogEntity());
    errorLogService.saveGeneralError("errorCode", UUID.randomUUID());

    verify(errorLogEntityCqlRepository).save(isA(ErrorLogEntity.class));
  }
}
