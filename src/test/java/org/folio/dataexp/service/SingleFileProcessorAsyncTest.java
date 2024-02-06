package org.folio.dataexp.service;


import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.service.export.ExportExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SingleFileProcessorAsyncTest {

  @Mock
  private ExportExecutor exportExecutor;
  @InjectMocks
  private SingleFileProcessorAsync singleFileProcessorAsync;

  @Test
  void executeExportTest() {
    var fileLocation = "mod-data-export/download/download.mrc";
    var exportEntity = JobExecutionExportFilesEntity.builder()
      .id(UUID.randomUUID())
      .fileLocation(fileLocation).build();
    var commonFails = new CommonExportFails();

    singleFileProcessorAsync.executeExport(exportEntity, new ExportRequest(), commonFails, false);

    verify(exportExecutor).exportAsynch(exportEntity, new ExportRequest(), commonFails, false);
  }
}
