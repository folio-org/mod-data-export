package org.folio.dataexp.service;

import lombok.SneakyThrows;
import org.folio.dataexp.client.SourceStorageClient;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.MarcRecordIdentifiersPayload;
import org.folio.dataexp.domain.dto.MarcRecordsIdentifiersResponse;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.util.Constants.DATE_PATTERN;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarcDeletedIdsServiceTest {

  @Mock
  private SourceStorageClient sourceStorageClient;
  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private FolioModuleMetadata folioModuleMetadata;
  @Mock
  private FileDefinitionsService fileDefinitionsService;
  @Captor
  private ArgumentCaptor<MarcRecordIdentifiersPayload> payloadArgumentCaptor;
  @InjectMocks
  private MarcDeletedIdsService marcDeletedIdsService;

  @Test
  void shouldReturnOneRecord_IfLocalTenantIsCentral() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
      .thenReturn(new MarcRecordsIdentifiersResponse().withRecords(List.of(UUID.randomUUID().toString())).withTotalCount(1));
    when(consortiaService.getCentralTenantId()).thenReturn("central");
    when(folioExecutionContext.getTenantId()).thenReturn("central");
    var fileDefinition = new FileDefinition().size(1).id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class))).thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class))).thenReturn(fileDefinition);

    var res = marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(null, null);
    assertThat(res.getSize()).isEqualTo(1);
  }

  @Test
  void shouldReturnTwoRecords_IfLocalTenantIsMember() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
      .thenReturn(new MarcRecordsIdentifiersResponse().withRecords(List.of(UUID.randomUUID().toString())).withTotalCount(1));
    when(consortiaService.getCentralTenantId()).thenReturn("central");
    when(folioExecutionContext.getTenantId()).thenReturn("member");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.of(XOkapiHeaders.TENANT, List.of("member")));
    var fileDefinition = new FileDefinition().size(2).id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class))).thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class))).thenReturn(fileDefinition);

    var res = marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(null, null);
    assertThat(res.getSize()).isEqualTo(2);
  }

  @Test
  @SneakyThrows
  void shouldHavePayloadWithDateRange_IfDateRangeIsUsed() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
      .thenReturn(new MarcRecordsIdentifiersResponse().withRecords(List.of(UUID.randomUUID().toString())).withTotalCount(1));
    when(consortiaService.getCentralTenantId()).thenReturn("central");
    when(folioExecutionContext.getTenantId()).thenReturn("member");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.of(XOkapiHeaders.TENANT, List.of("member")));
    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class))).thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class))).thenReturn(fileDefinition);

    var date = new SimpleDateFormat(DATE_PATTERN);
    marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(date.parse("20240424"), date.parse("20240424"));
    verify(sourceStorageClient, times(2)).getMarcRecordsIdentifiers(payloadArgumentCaptor.capture());

    var payload = payloadArgumentCaptor.getValue();
    assertThat(payload.getLeaderSearchExpression()).isEqualTo("p_05 = 'd'");
    assertThat(payload.getFieldsSearchExpression()).isEqualTo("005.date in '20240424-20240424'");
  }

  @Test
  @SneakyThrows
  void shouldHavePayloadWithFromDate_IfDateFromIsUsed() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
      .thenReturn(new MarcRecordsIdentifiersResponse().withRecords(List.of(UUID.randomUUID().toString())).withTotalCount(1));
    when(consortiaService.getCentralTenantId()).thenReturn("central");
    when(folioExecutionContext.getTenantId()).thenReturn("member");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.of(XOkapiHeaders.TENANT, List.of("member")));
    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class))).thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class))).thenReturn(fileDefinition);

    var date = new SimpleDateFormat(DATE_PATTERN);
    marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(date.parse("20240424"), null);
    verify(sourceStorageClient, times(2)).getMarcRecordsIdentifiers(payloadArgumentCaptor.capture());

    var payload = payloadArgumentCaptor.getValue();
    assertThat(payload.getLeaderSearchExpression()).isEqualTo("p_05 = 'd'");
    assertThat(payload.getFieldsSearchExpression()).isEqualTo("005.date from '20240424'");
  }

  @Test
  @SneakyThrows
  void shouldHavePayloadWithToDate_IfDateToIsUsed() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
      .thenReturn(new MarcRecordsIdentifiersResponse().withRecords(List.of(UUID.randomUUID().toString())).withTotalCount(1));
    when(consortiaService.getCentralTenantId()).thenReturn("central");
    when(folioExecutionContext.getTenantId()).thenReturn("member");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.of(XOkapiHeaders.TENANT, List.of("member")));
    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class))).thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class))).thenReturn(fileDefinition);

    var date = new SimpleDateFormat(DATE_PATTERN);
    marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(null, date.parse("20240424"));
    verify(sourceStorageClient, times(2)).getMarcRecordsIdentifiers(payloadArgumentCaptor.capture());

    var payload = payloadArgumentCaptor.getValue();
    assertThat(payload.getLeaderSearchExpression()).isEqualTo("p_05 = 'd'");
    assertThat(payload.getFieldsSearchExpression()).isEqualTo("005.date to '20240424'");
  }

  @Test
  void shouldHavePayloadWithNoDate_IfDateIsNotUsed() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
      .thenReturn(new MarcRecordsIdentifiersResponse().withRecords(List.of(UUID.randomUUID().toString())).withTotalCount(1));
    when(consortiaService.getCentralTenantId()).thenReturn("central");
    when(folioExecutionContext.getTenantId()).thenReturn("member");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.of(XOkapiHeaders.TENANT, List.of("member")));
    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class))).thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class))).thenReturn(fileDefinition);

    marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(null, null);
    verify(sourceStorageClient, times(2)).getMarcRecordsIdentifiers(payloadArgumentCaptor.capture());

    var payload = payloadArgumentCaptor.getValue();
    assertThat(payload.getLeaderSearchExpression()).isEqualTo("p_05 = 'd'");
    assertThat(payload.getFieldsSearchExpression()).isNull();
  }
}
