package org.folio.dataexp.service;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.ExportExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SingleFileProcessorTest extends BaseDataExportInitializer {

  @MockBean
  private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  @MockBean
  private ExportExecutor exportExecutor;
  @MockBean
  private JobExecutionEntityRepository jobExecutionEntityRepository;

  @Autowired
  private SingleFileProcessor singleFileProcessor;

  @Test
  @SneakyThrows
  void exportBySingleFileTest() {
    var jobExecutionId = UUID.randomUUID();
    var parent = String.format("mod-data-export/download/%s/", jobExecutionId);
    var fileLocation = parent + "download.mrc";
    var exportEntity = JobExecutionExportFilesEntity.builder()
      .id(UUID.randomUUID())
      .fileLocation(fileLocation).build();

    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId)).thenReturn(List.of(exportEntity));

    singleFileProcessor.exportBySingleFile(jobExecutionId, ExportRequest.IdTypeEnum.INSTANCE);

    verify(exportExecutor).export(exportEntity, ExportRequest.IdTypeEnum.INSTANCE);
  }

  @Test
  @SneakyThrows
  void exportBySingleFileIfExportEntitiesEmptyTest() {
    var jobExecutionId = UUID.randomUUID();
    var jobExecution = new JobExecution().id(jobExecutionId);
    var jobExecutionEntity = JobExecutionEntity.builder().jobExecution(jobExecution).id(jobExecutionId).build();

    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId)).thenReturn(Collections.EMPTY_LIST);
    when(jobExecutionEntityRepository.getReferenceById(jobExecutionId)).thenReturn(jobExecutionEntity);

    singleFileProcessor.exportBySingleFile(jobExecutionId, ExportRequest.IdTypeEnum.INSTANCE);

    verify(exportExecutor, times(0)).export(any(), any());
    verify(jobExecutionEntityRepository).save(isA(JobExecutionEntity.class));
    assertEquals(JobExecution.StatusEnum.FAIL, jobExecution.getStatus());
  }
}
