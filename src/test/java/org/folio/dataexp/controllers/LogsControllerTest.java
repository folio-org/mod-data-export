package org.folio.dataexp.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.util.Constants.QUERY_CQL_ALL_RECORDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.ErrorLog;
import org.folio.dataexp.domain.dto.ErrorLogCollection;
import org.folio.dataexp.domain.entity.ErrorLogEntity;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class LogsControllerTest {

  @Mock private ErrorLogEntityCqlRepository errorLogEntityCqlRepository;

  @InjectMocks private LogsController logsController;

  @Test
  @TestMate(name = "TestMate-66a7bccb716533ddb8e51a25916a2874")
  void getErrorLogsByQueryWhenQueryIsNotEmptyShouldUseProvidedQuery() {
    // Given
    var query = "errorMessageCode==ERROR_CODE_1";
    var offset = 0;
    var limit = 10;
    var errorLogId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var offsetRequest = OffsetRequest.of(offset, limit);
    var errorLog = new ErrorLog();
    errorLog.setId(errorLogId);
    var errorLogEntity = ErrorLogEntity.builder().id(errorLogId).errorLog(errorLog).build();
    var page = new PageImpl<>(List.of(errorLogEntity));
    when(errorLogEntityCqlRepository.findByCql(query, offsetRequest)).thenReturn(page);
    // When
    ResponseEntity<ErrorLogCollection> response =
        logsController.getErrorLogsByQuery(query, offset, limit);
    // Then
    verify(errorLogEntityCqlRepository).findByCql(query, offsetRequest);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    ErrorLogCollection collection = response.getBody();
    assertThat(collection).isNotNull();
    assertThat(collection.getTotalRecords()).isEqualTo(1);
    assertThat(collection.getErrorLogs()).hasSize(1);
    assertThat(collection.getErrorLogs().get(0).getId()).isEqualTo(errorLogId);
  }

  @ParameterizedTest
  @TestMate(name = "TestMate-e91d01caaab58528218a3f091ae95c07")
  @NullAndEmptySource
  void getErrorLogsByQueryWhenQueryIsEmptyShouldUseAllRecordsQuery(String query) {
    // Given
    var offset = 0;
    var limit = 20;
    var errorLogId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var offsetRequest = OffsetRequest.of(offset, limit);
    var errorLog = new ErrorLog();
    errorLog.setId(errorLogId);
    var errorLogEntity = ErrorLogEntity.builder().id(errorLogId).errorLog(errorLog).build();
    var page = new PageImpl<>(List.of(errorLogEntity));
    when(errorLogEntityCqlRepository.findByCql(QUERY_CQL_ALL_RECORDS, offsetRequest))
        .thenReturn(page);
    // When
    ResponseEntity<ErrorLogCollection> response =
        logsController.getErrorLogsByQuery(query, offset, limit);
    // Then
    verify(errorLogEntityCqlRepository).findByCql(QUERY_CQL_ALL_RECORDS, offsetRequest);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    ErrorLogCollection collection = response.getBody();
    assertThat(collection.getTotalRecords()).isEqualTo(1);
    assertThat(collection.getErrorLogs()).hasSize(1);
    assertThat(collection.getErrorLogs().get(0).getId()).isEqualTo(errorLogId);
  }

  @ParameterizedTest
  @TestMate(name = "TestMate-ad18ad2d00c0661267badbfe7d9e7264")
  @CsvSource({"50, 10", "0, 100"})
  void getErrorLogsByQueryShouldCorrectlyHandlePaginationParameters(int offset, int limit) {
    // Given
    var query = "errorMessageCode==ERROR_001";
    var errorLogId = UUID.fromString("00000000-0000-0000-0000-000000000005");
    var errorLog = new ErrorLog();
    errorLog.setId(errorLogId);
    var errorLogEntity = ErrorLogEntity.builder().id(errorLogId).errorLog(errorLog).build();
    var page = new PageImpl<>(Collections.singletonList(errorLogEntity));
    when(errorLogEntityCqlRepository.findByCql(eq(query), any(OffsetRequest.class)))
        .thenReturn(page);
    var offsetRequestCaptor = ArgumentCaptor.forClass(OffsetRequest.class);
    // When
    ResponseEntity<ErrorLogCollection> response =
        logsController.getErrorLogsByQuery(query, offset, limit);
    // Then
    verify(errorLogEntityCqlRepository).findByCql(eq(query), offsetRequestCaptor.capture());
    var capturedRequest = offsetRequestCaptor.getValue();
    assertThat(capturedRequest.getOffset()).isEqualTo(offset);
    assertThat(capturedRequest.getPageSize()).isEqualTo(limit);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getErrorLogs()).hasSize(1);
  }

  @Test
  @TestMate(name = "TestMate-2317fb6f7791c13a56ce63358500986f")
  void getErrorLogsByQueryWhenRepositoryReturnsEmptyPageShouldReturnEmptyCollection() {
    // Given
    var query = "errorMessageCode==NON_EXISTENT";
    var offset = 0;
    var limit = 10;
    var offsetRequest = OffsetRequest.of(offset, limit);
    when(errorLogEntityCqlRepository.findByCql(query, offsetRequest))
        .thenReturn(Page.empty(offsetRequest));
    // When
    ResponseEntity<ErrorLogCollection> response =
        logsController.getErrorLogsByQuery(query, offset, limit);
    // Then
    verify(errorLogEntityCqlRepository).findByCql(query, offsetRequest);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    var collection = response.getBody();
    assertThat(collection.getErrorLogs()).isEmpty();
    assertThat(collection.getTotalRecords()).isZero();
  }
}
