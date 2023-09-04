package org.folio.dataexp.service;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseTest;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.ExportExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SingleFileProcessorTest extends BaseTest {

  @MockBean
  private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  @MockBean
  private ExportExecutor exportExecutor;

  @Autowired
  private SingleFileProcessor singleFileProcessor;

  @Test
  @SneakyThrows
  void exportBySingleFile() {
    var jobExecutionId = UUID.randomUUID();
    var parent = String.format("mod-data-export/download/%s/", jobExecutionId);
    var fileLocation = parent + "download.mrc";
    var exportEntity = JobExecutionExportFilesEntity.builder()
      .id(UUID.randomUUID())
      .fileLocation(fileLocation).build();

    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId)).thenReturn(List.of(exportEntity));

    singleFileProcessor.exportBySingleFile(jobExecutionId, ExportRequest.RecordTypeEnum.INSTANCE);

    var path = Paths.get(parent);
    assertTrue(Files.exists(path));

    verify(exportExecutor).export(exportEntity,  ExportRequest.RecordTypeEnum.INSTANCE);
  }
}
