package org.folio.dataexp.service;

import org.folio.dataexp.client.SourceStorageClient;
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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MarcDeletedIdsServiceTest {

  @Mock
  private SourceStorageClient sourceStorageClient;
  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private FolioModuleMetadata folioModuleMetadata;
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

    var res = marcDeletedIdsService.getMarcDeletedIds(null, null, null, null);
    assertThat(res.getTotalRecords()).isEqualTo(1);
  }

  @Test
  void shouldReturnTwoRecords_IfLocalTenantIsMember() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
      .thenReturn(new MarcRecordsIdentifiersResponse().withRecords(List.of(UUID.randomUUID().toString())).withTotalCount(1));
    when(consortiaService.getCentralTenantId()).thenReturn("central");
    when(folioExecutionContext.getTenantId()).thenReturn("member");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.of(XOkapiHeaders.TENANT, List.of("member")));

    var res = marcDeletedIdsService.getMarcDeletedIds(null, null, null, null);
    assertThat(res.getTotalRecords()).isEqualTo(2);
  }

  @Test
  void shouldHavePayloadWith952Presence_IfLocalTenantIsMember() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
      .thenReturn(new MarcRecordsIdentifiersResponse().withRecords(List.of(UUID.randomUUID().toString())).withTotalCount(1));
    when(consortiaService.getCentralTenantId()).thenReturn("central");
    when(folioExecutionContext.getTenantId()).thenReturn("member");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.of(XOkapiHeaders.TENANT, List.of("member")));

    var res = marcDeletedIdsService.getMarcDeletedIds(new Date(), new Date(), null, null);
    verify(sourceStorageClient, times(2)).getMarcRecordsIdentifiers(payloadArgumentCaptor.capture());

    var payload = payloadArgumentCaptor.getValue();
    assertThat(payload.getLeaderSearchExpression()).isEqualTo("p_05 = 'd'");
    assertThat(payload.getFieldsSearchExpression()).isEqualTo("(005.date in '20240424-20240424') and (952.value is 'present')");
  }

  @Test
  void shouldHavePayloadWithout952Presence_IfLocalTenantIsCentral() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
      .thenReturn(new MarcRecordsIdentifiersResponse().withRecords(List.of(UUID.randomUUID().toString())).withTotalCount(1));
    when(consortiaService.getCentralTenantId()).thenReturn("central");
    when(folioExecutionContext.getTenantId()).thenReturn("central");

    var res = marcDeletedIdsService.getMarcDeletedIds(new Date(), new Date(), null, null);
    verify(sourceStorageClient, times(1)).getMarcRecordsIdentifiers(payloadArgumentCaptor.capture());

    var payload = payloadArgumentCaptor.getValue();
    assertThat(payload.getLeaderSearchExpression()).isEqualTo("p_05 = 'd'");
    assertThat(payload.getFieldsSearchExpression()).isEqualTo("(005.date in '20240424-20240424')");
  }
}
