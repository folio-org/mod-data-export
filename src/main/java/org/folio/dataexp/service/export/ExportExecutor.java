package org.folio.dataexp.service.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.storage.FolioS3ClientFactory;
import org.folio.dataexp.service.export.strategies.ExportStrategy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Log4j2
public class ExportExecutor {

  private final ExportStrategy instanceExportStrategy;
  private final FolioS3ClientFactory folioS3ClientFactory;
  private final JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  private final JobExecutionEntityRepository jobExecutionEntityRepository;

  public void export(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest.RecordTypeEnum recordType) {
    exportFilesEntity.setStatus(JobExecutionExportFilesStatus.ACTIVE);
    jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    var file = new File(exportFilesEntity.getFileLocation());
    try {
      boolean isCreated = file.createNewFile();
      if (isCreated) log.info("Create {} locally", exportFilesEntity.getFileLocation());
      instanceExportStrategy.saveMarc(exportFilesEntity, file);
      var s3Client = folioS3ClientFactory.getFolioS3Client();
      try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
        s3Client.write(exportFilesEntity.getFileLocation(), is);
        log.info("Create {} at remote storage", exportFilesEntity.getFileLocation());
      }
      exportFilesEntity.setStatus(JobExecutionExportFilesStatus.COMPLETED);
      jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    } catch (Exception e) {
      exportFilesEntity.setStatus(JobExecutionExportFilesStatus.FAILED);
      jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
      log.error("Error exporting {} for jobExecution {} : {}",
        exportFilesEntity.getFileLocation(), exportFilesEntity.getJobExecutionId(), e.getMessage());
    }
    updateJobExecutionStatus(exportFilesEntity.getJobExecutionId());
  }

  @Async("singleExportFileTaskExecutor")
  public void exportAsynch(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest.RecordTypeEnum recordType) {
    export(exportFilesEntity, recordType);
  }

  private synchronized void updateJobExecutionStatus(UUID jobExecutionId) {
    var jobExecutionEntity = jobExecutionEntityRepository.getReferenceById(jobExecutionId);
    var exports = jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId);
    long exportsCompleted = exports.stream().filter(e -> e.getStatus() == JobExecutionExportFilesStatus.COMPLETED).count();
    long exportsFailed = exports.stream().filter(e -> e.getStatus() == JobExecutionExportFilesStatus.FAILED).count();
    if (exportsCompleted + exportsFailed != exports.size()) return;

    var jobExecution = jobExecutionEntity.getJobExecution();
    if (exports.size() == exportsCompleted) jobExecution.setStatus(JobExecution.StatusEnum.COMPLETED);
    else if (exports.size() == exportsFailed) jobExecution.setStatus(JobExecution.StatusEnum.FAIL);
    else jobExecution.setStatus(JobExecution.StatusEnum.COMPLETED_WITH_ERRORS);
    jobExecutionEntityRepository.save(jobExecutionEntity);
  }
}
