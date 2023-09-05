package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.ExportExecutor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Log4j2
public class SingleFileProcessor {

  protected final ExportExecutor exportExecutor;
  private final JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  private final JobExecutionEntityRepository jobExecutionEntityRepository;

  public void exportBySingleFile(UUID jobExecutionId, ExportRequest.RecordTypeEnum recordType) {
    var exports = jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId);
    if (exports.isEmpty()) {
      log.warn("Nothing to export for job execution {}", jobExecutionId);
      var jobExecutionEntity = jobExecutionEntityRepository.getReferenceById(jobExecutionId);
      var jobExecution = jobExecutionEntity.getJobExecution();
      jobExecution.setLastUpdatedDate(new Date());
      jobExecution.setStatus(JobExecution.StatusEnum.FAIL);
      return;
    }
    var file = new File(exports.get(0).getFileLocation());
    var parent = file.getParentFile();
    if (!parent.exists()) {
      boolean isCreated = parent.mkdirs();
      if (isCreated) log.info("Create local directories {} to store exports for {} ", parent.getAbsolutePath(), jobExecutionId);
    }
    exports.forEach(export -> executeExport(export, recordType));
  }

  public void executeExport(JobExecutionExportFilesEntity export,  ExportRequest.RecordTypeEnum recordType) {
    exportExecutor.export(export, recordType);
  }
}
