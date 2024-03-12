package org.folio.dataexp.service;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
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
import static org.mockito.ArgumentMatchers.eq;
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
  private JobExecutionService jobExecutionService;

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

    singleFileProcessor.exportBySingleFile(jobExecutionId, new ExportRequest(), new CommonExportStatistic());

    verify(exportExecutor).export(eq(exportEntity), isA(ExportRequest.class), isA(CommonExportStatistic.class));
  }

  @Test
  @SneakyThrows
  void exportBySingleFileIfExportEntitiesEmptyTest() {
    var jobExecutionId = UUID.randomUUID();
    var progress = new JobExecutionProgress();
    var jobExecution = new JobExecution().id(jobExecutionId).progress(progress);
    var commonExportFails = new CommonExportStatistic();
    commonExportFails.setFailedToReadInputFile(false);

    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId)).thenReturn(Collections.EMPTY_LIST);
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);

    singleFileProcessor.exportBySingleFile(jobExecutionId, new ExportRequest(), commonExportFails);

    verify(exportExecutor, times(0)).export(any(), any(), any());
    verify(jobExecutionService).save(isA(JobExecution.class));
    assertEquals(JobExecution.StatusEnum.FAIL, jobExecution.getStatus());
  }
}
