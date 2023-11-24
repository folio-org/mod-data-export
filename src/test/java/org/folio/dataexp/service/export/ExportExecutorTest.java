package org.folio.dataexp.service.export;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ExportExecutorTest extends BaseDataExportInitializer {
  @MockBean
  private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  @MockBean
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  @Autowired
  private FolioS3Client s3Client;
  @Autowired
  private ExportExecutor exportExecutor;

  @Test
  @SneakyThrows
  void exportTest() {
    var jobExecutionId = UUID.randomUUID();
    var jobExecution = new JobExecution();
    jobExecution.setProgress(new JobExecutionProgress());
    jobExecution.setId(jobExecutionId);
    var jobExecutionEntity = JobExecutionEntity.builder().jobExecution(jobExecution).build();

    var fileLocation = String.format("mod-data-export/download/%s/download.mrc", jobExecutionId);
    var exportEntity = JobExecutionExportFilesEntity.builder()
      .id(UUID.randomUUID())
      .jobExecutionId(jobExecutionId)
      .fileLocation(fileLocation).build();

    when(jobExecutionEntityRepository.getReferenceById(jobExecutionId)).thenReturn(jobExecutionEntity);
    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId)).thenReturn(List.of(exportEntity));

    exportExecutor.export(exportEntity, ExportRequest.IdTypeEnum.INSTANCE);

    long size = s3Client.getSize(fileLocation);
    assertTrue(size > 0);

    assertEquals(JobExecutionExportFilesStatus.COMPLETED, exportEntity.getStatus());
    assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
  }
}
