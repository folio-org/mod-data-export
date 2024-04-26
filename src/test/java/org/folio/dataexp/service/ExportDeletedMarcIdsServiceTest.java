package org.folio.dataexp.service;

import lombok.SneakyThrows;
import org.folio.dataexp.client.SourceStorageClient;
import org.folio.dataexp.domain.dto.ExportDeletedMarcIdsRequest;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportDeletedMarcIdsServiceTest {

  private final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  @Mock
  private SourceStorageClient sourceStorageClient;
  @Mock
  private FileDefinitionsService fileDefinitionsService;
  @Mock
  private MarcDeletedIdsService marcDeletedIdsService;
  @Mock
  private JobProfileEntityRepository jobProfileEntityRepository;
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
    var from = DATE_FORMAT.parse("2024-04-24");
    var to = DATE_FORMAT.parse("2024-04-24");
    var request = new ExportDeletedMarcIdsRequest();
    request.setFrom(from);
    request.setTo(to);

    when(jobProfileEntityRepository.findIdOfDefaultJobProfileByName(ExportRequest.IdTypeEnum.INSTANCE.getValue()))
      .thenReturn(List.of(UUID.randomUUID()));
    when(marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(from, to)).thenReturn(fileDefinition);

    exportDeletedMarcIdsService.postExportDeletedMarcIds(request);

    verify(dataExportService).postDataExport(exportRequestArgumentCaptor.capture());
    var exportRequest = exportRequestArgumentCaptor.getValue();

    assertThat(exportRequest.getFileDefinitionId()).isInstanceOf(UUID.class);
    assertThat(exportRequest.getJobProfileId()).isInstanceOf(UUID.class);
  }
}
