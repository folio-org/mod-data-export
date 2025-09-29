package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.util.Constants.DATE_PATTERN;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.client.SourceStorageClient;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.MarcRecordIdentifiersPayload;
import org.folio.dataexp.domain.dto.MarcRecordsIdentifiersResponse;
import org.folio.dataexp.exception.export.ExportDeletedDateRangeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

@ExtendWith(MockitoExtension.class)
class MarcDeletedIdsServiceTest {

  private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  @Mock
  private SourceStorageClient sourceStorageClient;
  @Mock
  private FileDefinitionsService fileDefinitionsService;
  @Captor
  private ArgumentCaptor<MarcRecordIdentifiersPayload> payloadArgumentCaptor;
  @InjectMocks
  private MarcDeletedIdsService marcDeletedIdsService;

  @Test
  void shouldReturnOneRecord() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
        .thenReturn(new MarcRecordsIdentifiersResponse().withRecords(List.of(UUID.randomUUID()
            .toString())).withTotalCount(1));
    var fileDefinition = new FileDefinition().size(1).id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class)))
        .thenReturn(fileDefinition);

    var res = marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(null, null);
    assertThat(res.getSize()).isEqualTo(1);
  }

  @Test
  @SneakyThrows
  void shouldHavePayloadWithDateRangeIfDateRangeIsUsed() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
        .thenReturn(new MarcRecordsIdentifiersResponse().withRecords(List.of(UUID.randomUUID()
            .toString())).withTotalCount(1));
    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class)))
        .thenReturn(fileDefinition);

    var date = new SimpleDateFormat(DATE_PATTERN);
    marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(date.parse("20240424"),
        date.parse("20240424"));
    verify(sourceStorageClient).getMarcRecordsIdentifiers(payloadArgumentCaptor.capture());

    var payload = payloadArgumentCaptor.getValue();
    assertThat(payload.getLeaderSearchExpression()).isEqualTo("p_05 = 'd'");
    assertThat(payload.getFieldsSearchExpression()).isEqualTo("005.date in '20240424-20240424'");
  }

  @Test
  @SneakyThrows
  void shouldHavePayloadWithFromDateIfDateFromIsUsed() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
        .thenReturn(new MarcRecordsIdentifiersResponse().withRecords(List.of(UUID.randomUUID()
            .toString())).withTotalCount(1));
    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class)))
        .thenReturn(fileDefinition);

    var date = new SimpleDateFormat(DATE_PATTERN);
    marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(date.parse("20240424"), null);
    verify(sourceStorageClient).getMarcRecordsIdentifiers(payloadArgumentCaptor.capture());

    var payload = payloadArgumentCaptor.getValue();
    assertThat(payload.getLeaderSearchExpression()).isEqualTo("p_05 = 'd'");
    assertThat(payload.getFieldsSearchExpression()).isEqualTo("005.date from '20240424'");
  }

  @Test
  @SneakyThrows
  void shouldHavePayloadWithToDateIfDateToIsUsed() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
        .thenReturn(new MarcRecordsIdentifiersResponse().withRecords(List.of(UUID.randomUUID()
            .toString())).withTotalCount(1));
    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class)))
        .thenReturn(fileDefinition);

    var date = new SimpleDateFormat(DATE_PATTERN);
    marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(null, date.parse("20240424"));
    verify(sourceStorageClient).getMarcRecordsIdentifiers(payloadArgumentCaptor.capture());

    var payload = payloadArgumentCaptor.getValue();
    assertThat(payload.getLeaderSearchExpression()).isEqualTo("p_05 = 'd'");
    assertThat(payload.getFieldsSearchExpression()).isEqualTo("005.date to '20240424'");
  }

  @Test
  void shouldHavePayloadWithPreviousDayIfDateIsNotUsed() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
        .thenReturn(new MarcRecordsIdentifiersResponse().withRecords(List.of(UUID.randomUUID()
            .toString())).withTotalCount(1));
    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class)))
        .thenReturn(fileDefinition);

    marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(null, null);
    verify(sourceStorageClient).getMarcRecordsIdentifiers(payloadArgumentCaptor.capture());

    var payload = payloadArgumentCaptor.getValue();
    assertThat(payload.getLeaderSearchExpression()).isEqualTo("p_05 = 'd'");
    var previousDay = LocalDate.now().minusDays(1).format(DateTimeFormatter
        .ofPattern(DATE_PATTERN));
    assertThat(payload.getFieldsSearchExpression()).isEqualTo("005.date in '"
        + previousDay + "-" + previousDay + "'");
  }

  @Test
  @SneakyThrows
  void shouldThrowExportDeletedDateRangeException_ifDateFromIsAfterDateTo() {
    var from = dateFormat.parse("2024-04-24");
    var to = dateFormat.parse("2024-04-23");

    assertThrows(ExportDeletedDateRangeException.class, () ->
        marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(from, to));
  }
}
