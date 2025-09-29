package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;
import lombok.SneakyThrows;
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

  @Mock
  private MarcDeletedIdsService marcDeletedIdsService;
  @Mock
  private DataExportService dataExportService;

  @InjectMocks
  private ExportDeletedMarcIdsService exportDeletedMarcIdsService;

  @Captor
  private ArgumentCaptor<ExportRequest> exportRequestArgumentCaptor;

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
}
