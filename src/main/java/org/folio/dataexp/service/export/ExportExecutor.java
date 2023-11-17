package org.folio.dataexp.service.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionExportedFilesInner;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.strategies.ExportStrategy;
import org.folio.dataexp.service.export.strategies.ExportStrategyStatistic;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Log4j2
public class ExportExecutor {

  private final JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  private final JobExecutionEntityRepository jobExecutionEntityRepository;
  private final ExportStrategyFactory exportStrategyFactory;

  @Async("singleExportFileTaskExecutor")
  public void exportAsynch(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest.IdTypeEnum idTypeEnum) {
    export(exportFilesEntity, idTypeEnum);
  }

  public void export(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest.IdTypeEnum idType) {
    log.info("Started export {} for job execution {}", exportFilesEntity.getFileLocation(), exportFilesEntity.getJobExecutionId());
    exportFilesEntity.setStatus(JobExecutionExportFilesStatus.ACTIVE);
    jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    var exportStrategy = exportStrategyFactory.getExportStrategy(idType);
    var exportStatistic = exportStrategy.saveMarcToRemoteStorage(exportFilesEntity);
    updateJobExecutionStatusAndProgress(exportFilesEntity.getJobExecutionId(), exportStatistic);
    log.info("Complete export {} for job execution {}", exportFilesEntity.getFileLocation(), exportFilesEntity.getJobExecutionId());
  }

  private synchronized void updateJobExecutionStatusAndProgress(UUID jobExecutionId, ExportStrategyStatistic exportStatistic) {
    var jobExecutionEntity = jobExecutionEntityRepository.getReferenceById(jobExecutionId);
    var jobExecution = jobExecutionEntity.getJobExecution();
    var progress = jobExecution.getProgress();
    progress.setExported(progress.getExported() + exportStatistic.getExported());
    progress.setFailed(progress.getFailed() + exportStatistic.getFailed());
    progress.setDuplicatedSrs(progress.getDuplicatedSrs() + progress.getDuplicatedSrs());
    var exports = jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId);
    long exportsCompleted = exports.stream().filter(e -> e.getStatus() == JobExecutionExportFilesStatus.COMPLETED).count();
    long exportsFailed = exports.stream().filter(e -> e.getStatus() == JobExecutionExportFilesStatus.FAILED).count();
    long exportsCompletedWithErrors = exports.stream().filter(e -> e.getStatus() == JobExecutionExportFilesStatus.COMPLETED_WITH_ERRORS).count();
    var currentDate = new Date();
    if (exportsCompleted + exportsFailed + exportsCompletedWithErrors == exports.size()) {
      if (exports.size() == exportsCompleted) jobExecution.setStatus(JobExecution.StatusEnum.COMPLETED);
      else if (exports.size() == exportsFailed) jobExecution.setStatus(JobExecution.StatusEnum.FAIL);
      else jobExecution.setStatus(JobExecution.StatusEnum.COMPLETED_WITH_ERRORS);

      var filesForExport = exports.stream()
        .filter(e -> e.getStatus() == JobExecutionExportFilesStatus.COMPLETED || e.getStatus() == JobExecutionExportFilesStatus.COMPLETED_WITH_ERRORS)
        .map(e -> new JobExecutionExportedFilesInner().fileId(e.getId()).fileName(FilenameUtils.getName(e.getFileLocation()))).collect(Collectors.toSet());

      jobExecution.setExportedFiles(filesForExport);

      jobExecution.completedDate(currentDate);
      jobExecutionEntity.setStatus(jobExecution.getStatus());
      jobExecutionEntity.setCompletedDate(jobExecution.getCompletedDate());
    }
    jobExecution.setLastUpdatedDate(currentDate);
    jobExecutionEntityRepository.save(jobExecutionEntity);
    log.info("Job execution by id {} is updated with status {}", jobExecutionId, jobExecution.getStatus());
  }
}
