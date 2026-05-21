package org.folio.dataexp.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.util.Constants.DATE_PATTERN;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.client.SourceStorageClient;
import org.folio.dataexp.domain.dto.srsresponse.ExternalIdsHolder;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.srsresponse.Content;
import org.folio.dataexp.domain.dto.MarcRecordIdentifiersPayload;
import org.folio.dataexp.domain.dto.srsresponse.MarcRecordResponse;
import org.folio.dataexp.domain.dto.MarcRecordsIdentifiersResponse;
import org.folio.dataexp.domain.dto.srsresponse.MarcRecordsResponse;
import org.folio.dataexp.domain.dto.srsresponse.ParsedRecord;
import org.folio.dataexp.exception.export.ExportDeletedDateRangeException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

@ExtendWith(MockitoExtension.class)
class MarcDeletedIdsServiceTest {

  private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
  private static final Map<String, Collection<String>> headers = new HashMap<>();

  @Mock private SourceStorageClient sourceStorageClient;
  @Mock private FileDefinitionsService fileDefinitionsService;
  @Captor private ArgumentCaptor<MarcRecordIdentifiersPayload> payloadArgumentCaptor;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private ConsortiaService consortiaService;
  @Mock private FolioModuleMetadata folioModuleMetadata;
  @InjectMocks private MarcDeletedIdsService marcDeletedIdsService;
  @Captor private ArgumentCaptor<ByteArrayResource> resourceArgumentCaptor;

  @BeforeAll
  static void setUp() {
    headers.put(XOkapiHeaders.TENANT, List.of("TENANT"));
  }

  @Test
  void shouldReturnOneRecord() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
        .thenReturn(
            new MarcRecordsIdentifiersResponse()
                .withRecords(List.of(UUID.randomUUID().toString()))
                .withTotalCount(1));
    var fileDefinition = new FileDefinition().size(1).id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class)))
        .thenReturn(fileDefinition);
    when(consortiaService.getCentralTenantId(null)).thenReturn("central");
    when(consortiaService.isCurrentTenantCentralTenant(null)).thenReturn(true);

    var res = marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(null, null);
    assertThat(res.getSize()).isEqualTo(1);
  }

  @Test
  @SneakyThrows
  void shouldHavePayloadWithDateRangeIfDateRangeIsUsed() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
        .thenReturn(
            new MarcRecordsIdentifiersResponse()
                .withRecords(List.of(UUID.randomUUID().toString()))
                .withTotalCount(1));
    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class)))
        .thenReturn(fileDefinition);
    when(consortiaService.getCentralTenantId(null)).thenReturn("central");
    when(consortiaService.isCurrentTenantCentralTenant(null)).thenReturn(true);

    var date = new SimpleDateFormat(DATE_PATTERN);
    marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(
        date.parse("20240424"), date.parse("20240424"));
    verify(sourceStorageClient).getMarcRecordsIdentifiers(payloadArgumentCaptor.capture());

    var payload = payloadArgumentCaptor.getValue();
    assertThat(payload.getLeaderSearchExpression()).isEqualTo("p_05 = 'd'");
    assertThat(payload.getFieldsSearchExpression()).isEqualTo("005.date in '20240424-20240424'");
  }

  @Test
  @SneakyThrows
  void shouldHavePayloadWithFromDateIfDateFromIsUsed() {
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
        .thenReturn(
            new MarcRecordsIdentifiersResponse()
                .withRecords(List.of(UUID.randomUUID().toString()))
                .withTotalCount(1));
    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class)))
        .thenReturn(fileDefinition);
    when(consortiaService.getCentralTenantId(null)).thenReturn("central");
    when(consortiaService.isCurrentTenantCentralTenant(null)).thenReturn(true);

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
        .thenReturn(
            new MarcRecordsIdentifiersResponse()
                .withRecords(List.of(UUID.randomUUID().toString()))
                .withTotalCount(1));
    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class)))
        .thenReturn(fileDefinition);
    when(consortiaService.getCentralTenantId(null)).thenReturn("central");
    when(consortiaService.isCurrentTenantCentralTenant(null)).thenReturn(true);

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
        .thenReturn(
            new MarcRecordsIdentifiersResponse()
                .withRecords(List.of(UUID.randomUUID().toString()))
                .withTotalCount(1));
    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class)))
        .thenReturn(fileDefinition);
    when(consortiaService.getCentralTenantId(null)).thenReturn("central");
    when(consortiaService.isCurrentTenantCentralTenant(null)).thenReturn(true);

    marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(null, null);
    verify(sourceStorageClient).getMarcRecordsIdentifiers(payloadArgumentCaptor.capture());

    var payload = payloadArgumentCaptor.getValue();
    assertThat(payload.getLeaderSearchExpression()).isEqualTo("p_05 = 'd'");
    var previousDay =
        LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern(DATE_PATTERN));
    assertThat(payload.getFieldsSearchExpression())
        .isEqualTo("005.date in '" + previousDay + "-" + previousDay + "'");
  }

  @Test
  @SneakyThrows
  void shouldThrowExportDeletedDateRangeException_ifDateFromIsAfterDateTo() {
    var from = dateFormat.parse("2024-04-24");
    var to = dateFormat.parse("2024-04-23");

    assertThrows(
        ExportDeletedDateRangeException.class,
        () -> marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(from, to));
  }

  @Test
  @SneakyThrows
  void shouldExcludeSharedNonDeletedIds() {
    var id1 = UUID.randomUUID().toString();
    var id2 = UUID.randomUUID().toString();
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
        .thenReturn(
            new MarcRecordsIdentifiersResponse().withRecords(List.of(id1, id2)).withTotalCount(2));
    // id2 exists in central tenant and is NOT deleted (leader[5] != 'd') -> should be removed
    var nonDeletedRec = buildMarcRecord(id2, "00000nam a2200000 i 4500");
    var centralResponse = new MarcRecordsResponse();
    centralResponse.setSourceRecords(List.of(nonDeletedRec));
    centralResponse.setTotalRecords(1);
    when(sourceStorageClient.getMarcRecordsByExternalIds(isA(List.class)))
        .thenReturn(centralResponse);
    when(consortiaService.isCurrentTenantCentralTenant("member")).thenReturn(false);
    when(consortiaService.getCentralTenantId("member")).thenReturn("central");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(folioExecutionContext.getTenantId()).thenReturn("member");
    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class)))
        .thenReturn(fileDefinition);

    marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(null, null);

    verify(fileDefinitionsService).uploadFile(isA(UUID.class), resourceArgumentCaptor.capture());
    assertThat(id1).isEqualTo(resourceArgumentCaptor.getValue().getContentAsString(UTF_8));
  }

  @Test
  @SneakyThrows
  void shouldNotCheckCentralTenantWhenNonEcs() {
    var id1 = UUID.randomUUID().toString();
    var id2 = UUID.randomUUID().toString();
    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
        .thenReturn(
            new MarcRecordsIdentifiersResponse().withRecords(List.of(id1, id2)).withTotalCount(2));
    when(folioExecutionContext.getTenantId()).thenReturn("member");
    when(consortiaService.getCentralTenantId("member")).thenReturn("");
    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);

    marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(null, null);

    verify(consortiaService, never()).isCurrentTenantCentralTenant("member");
    verify(fileDefinitionsService).uploadFile(isA(UUID.class), resourceArgumentCaptor.capture());
    assertThat(resourceArgumentCaptor.getValue().getContentAsString(UTF_8))
        .isEqualTo(String.join(System.lineSeparator(), List.of(id1, id2)));
  }

  // ---- tests for lines 86-98 (getMarcRecordsByExternalIds filtering) ----

  /**
   * When the central tenant returns a record whose leader position 5 is NOT 'd' (not deleted), that
   * ID must be removed from the final list.
   */
  @Test
  @SneakyThrows
  void shouldRemoveNonDeletedSharedIdFromCentralTenant() {
    var deletedId = UUID.randomUUID().toString();
    var nonDeletedId = UUID.randomUUID().toString();

    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
        .thenReturn(
            new MarcRecordsIdentifiersResponse()
                .withRecords(List.of(deletedId, nonDeletedId))
                .withTotalCount(2));

    // Central tenant returns one record where leader[5] == 'n' (not deleted)
    var nonDeletedRecord = buildMarcRecord(nonDeletedId, "00000nam a2200000 i 4500");
    var centralResponse = new MarcRecordsResponse();
    centralResponse.setSourceRecords(List.of(nonDeletedRecord));
    centralResponse.setTotalRecords(1);

    when(consortiaService.getCentralTenantId("member")).thenReturn("central");
    when(consortiaService.isCurrentTenantCentralTenant("member")).thenReturn(false);
    when(folioExecutionContext.getTenantId()).thenReturn("member");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(sourceStorageClient.getMarcRecordsByExternalIds(isA(List.class)))
        .thenReturn(centralResponse);

    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class)))
        .thenReturn(fileDefinition);

    marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(null, null);

    verify(fileDefinitionsService).uploadFile(isA(UUID.class), resourceArgumentCaptor.capture());
    var resultContent = resourceArgumentCaptor.getValue().getContentAsString(UTF_8);
    assertThat(resultContent).contains(deletedId).doesNotContain(nonDeletedId);
  }

  /**
   * When the central tenant returns a record whose leader position 5 IS 'd' (deleted), that ID must
   * be kept in the final list.
   */
  @Test
  @SneakyThrows
  void shouldKeepAllIdsWhenAllCentralTenantRecordsAreDeleted() {
    var id1 = UUID.randomUUID().toString();
    var id2 = UUID.randomUUID().toString();

    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
        .thenReturn(
            new MarcRecordsIdentifiersResponse().withRecords(List.of(id1, id2)).withTotalCount(2));

    // Both records found in central tenant have leader[5] == 'd' (deleted)
    var rec1 = buildMarcRecord(id1, "00000dam a2200000 i 4500");
    var rec2 = buildMarcRecord(id2, "00000dam a2200000 i 4500");
    var centralResponse = new MarcRecordsResponse();
    centralResponse.setSourceRecords(List.of(rec1, rec2));
    centralResponse.setTotalRecords(2);

    when(consortiaService.getCentralTenantId("member")).thenReturn("central");
    when(consortiaService.isCurrentTenantCentralTenant("member")).thenReturn(false);
    when(folioExecutionContext.getTenantId()).thenReturn("member");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(sourceStorageClient.getMarcRecordsByExternalIds(isA(List.class)))
        .thenReturn(centralResponse);

    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class)))
        .thenReturn(fileDefinition);

    marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(null, null);

    verify(fileDefinitionsService).uploadFile(isA(UUID.class), resourceArgumentCaptor.capture());
    var resultContent = resourceArgumentCaptor.getValue().getContentAsString(UTF_8);
    assertThat(resultContent).contains(id1).contains(id2);
  }

  /** When the central tenant returns an empty list, all local deleted IDs are preserved. */
  @Test
  @SneakyThrows
  void shouldKeepAllIdsWhenCentralTenantReturnsEmptyList() {
    var id1 = UUID.randomUUID().toString();
    var id2 = UUID.randomUUID().toString();

    when(sourceStorageClient.getMarcRecordsIdentifiers(isA(MarcRecordIdentifiersPayload.class)))
        .thenReturn(
            new MarcRecordsIdentifiersResponse().withRecords(List.of(id1, id2)).withTotalCount(2));

    var centralResponse = new MarcRecordsResponse();
    centralResponse.setSourceRecords(List.of());
    centralResponse.setTotalRecords(0);

    when(consortiaService.getCentralTenantId("member")).thenReturn("central");
    when(consortiaService.isCurrentTenantCentralTenant("member")).thenReturn(false);
    when(folioExecutionContext.getTenantId()).thenReturn("member");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(sourceStorageClient.getMarcRecordsByExternalIds(isA(List.class)))
        .thenReturn(centralResponse);

    var fileDefinition = new FileDefinition().id(UUID.randomUUID());
    when(fileDefinitionsService.postFileDefinition(isA(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(isA(UUID.class), isA(Resource.class)))
        .thenReturn(fileDefinition);

    marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(null, null);

    verify(fileDefinitionsService).uploadFile(isA(UUID.class), resourceArgumentCaptor.capture());
    var resultContent = resourceArgumentCaptor.getValue().getContentAsString(UTF_8);
    assertThat(resultContent).contains(id1).contains(id2);
  }

  // Helper to build a MarcRecordResponse with given instanceId and MARC leader string
  private MarcRecordResponse buildMarcRecord(String instanceId, String leader) {
    var marcContent = new Content();
    marcContent.setLeader(leader);
    var parsedRecord = new ParsedRecord();
    parsedRecord.setContent(marcContent);
    var externalIdsHolder = new ExternalIdsHolder();
    externalIdsHolder.setInstanceId(instanceId);
    var marcRecordResponse = new MarcRecordResponse();
    marcRecordResponse.setParsedRecord(parsedRecord);
    marcRecordResponse.setExternalIdsHolder(externalIdsHolder);
    return marcRecordResponse;
  }
}
