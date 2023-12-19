package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.ExportExecutor;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Log4j2
public class SingleFileProcessor {

  protected final ExportExecutor exportExecutor;
  private final JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  private final JobExecutionEntityRepository jobExecutionEntityRepository;
  private final ErrorLogService errorLogService;

  public void exportBySingleFile(UUID jobExecutionId, ExportRequest.IdTypeEnum idType, CommonExportFails commonExportFails) {
    var exports = jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId);
    if (exports.isEmpty()) {
      log.error("Nothing to export for job execution {}", jobExecutionId);
      var jobExecutionEntity = jobExecutionEntityRepository.getReferenceById(jobExecutionId);
      var jobExecution = jobExecutionEntity.getJobExecution();
      var currentDate = new Date();
      jobExecution.setLastUpdatedDate(currentDate);
      jobExecution.setStatus(JobExecution.StatusEnum.FAIL);
      jobExecution.setCompletedDate(currentDate);

      jobExecutionEntity.setStatus(jobExecution.getStatus());
      jobExecutionEntity.setCompletedDate(jobExecution.getCompletedDate());
      var totalFailed = commonExportFails.getInvalidUUIDFormat().size();
      var progress = jobExecution.getProgress();
      progress.setFailed(totalFailed);
      progress.setExported(0);
      jobExecutionEntityRepository.save(jobExecutionEntity);
      errorLogService.saveCommonExportFailsErrors(commonExportFails, totalFailed, jobExecutionId);
      return;

    }
    exports.forEach(export -> executeExport(export, idType, commonExportFails));
  }

  public void executeExport(JobExecutionExportFilesEntity export, ExportRequest.IdTypeEnum idType, CommonExportFails commonExportFails) {
    exportExecutor.export(export, idType, commonExportFails);
  }
}
