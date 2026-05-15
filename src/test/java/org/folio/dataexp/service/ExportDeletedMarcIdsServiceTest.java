package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.ExportDeletedMarcIdsRequest;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportDeletedMarcIdsServiceTest {

  private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  @Mock private MarcDeletedIdsService marcDeletedIdsService;
  @Mock private DataExportService dataExportService;

  @InjectMocks private ExportDeletedMarcIdsService exportDeletedMarcIdsService;

  @Captor private ArgumentCaptor<ExportRequest> exportRequestArgumentCaptor;

  @Test
  @SneakyThrows
  void postExportDeletedMarcIdsTest() {
    var fileDefinition = new FileDefinition().size(1).id(UUID.randomUUID());
    UUID jobExecutionId = UUID.randomUUID();
    fileDefinition.setJobExecutionId(jobExecutionId);
    var from = dateFormat.parse("2024-04-24");
    var to = dateFormat.parse("2024-04-24");
    var request = new ExportDeletedMarcIdsRequest();
    request.setFrom(from);
    request.setTo(to);

    when(marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(from, to))
        .thenReturn(fileDefinition);

    var response = exportDeletedMarcIdsService.postExportDeletedMarcIds(request);
    assertEquals(jobExecutionId, response.getJobExecutionId());
    verify(dataExportService).postDataExport(exportRequestArgumentCaptor.capture());
    var exportRequest = exportRequestArgumentCaptor.getValue();

    assertThat(exportRequest.getFileDefinitionId()).isInstanceOf(UUID.class);
    assertThat(exportRequest.getJobProfileId()).isInstanceOf(UUID.class);
  }

  @Test
  @TestMate(name = "TestMate-ac8197c64029dbb2bb834f4eecee6f2f")
  void testPostExportDeletedMarcIdsWhenRequestIsNullShouldCallDependenciesWithNullDates() {
    // Given
    var fileDefinitionId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
    var jobExecutionId = UUID.fromString("f0e9d8c7-b6a5-4321-fedc-ba9876543210");
    var fileDefinition = new FileDefinition().id(fileDefinitionId).jobExecutionId(jobExecutionId);
    when(marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(null, null))
        .thenReturn(fileDefinition);
    // When
    var response = exportDeletedMarcIdsService.postExportDeletedMarcIds(null);
    // Then
    assertEquals(jobExecutionId, response.getJobExecutionId());
    verify(marcDeletedIdsService).getFileDefinitionForMarcDeletedIds(null, null);
    verify(dataExportService).postDataExport(exportRequestArgumentCaptor.capture());
    var capturedRequest = exportRequestArgumentCaptor.getValue();
    assertThat(capturedRequest.getFileDefinitionId()).isEqualTo(fileDefinitionId);
    assertThat(capturedRequest.getJobProfileId())
        .isEqualTo(UUID.fromString("6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a"));
    assertThat(capturedRequest.getAll()).isFalse();
    assertThat(capturedRequest.getQuick()).isFalse();
  }
}
