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
import org.folio.dataexp.service.CommonExportFails;
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
  public void exportAsynch(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest.IdTypeEnum idTypeEnum, CommonExportFails commonExportFails) {
    export(exportFilesEntity, idTypeEnum, commonExportFails);
  }

  public void export(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest.IdTypeEnum idType, CommonExportFails commonExportFails) {
    log.info("export:: Started export {} for job execution {}", exportFilesEntity.getFileLocation(), exportFilesEntity.getJobExecutionId());
    exportFilesEntity.setStatus(JobExecutionExportFilesStatus.ACTIVE);
    jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    var exportStrategy = exportStrategyFactory.getExportStrategy(idType);
    var exportStatistic = exportStrategy.saveMarcToRemoteStorage(exportFilesEntity);
    commonExportFails.addToNotExistUUIDAll(exportStatistic.getNotExistIds());
    updateJobExecutionStatusAndProgress(exportFilesEntity.getJobExecutionId(), exportStatistic, commonExportFails);
    log.info("export:: Complete export {} for job execution {}", exportFilesEntity.getFileLocation(), exportFilesEntity.getJobExecutionId());
  }

  private synchronized void updateJobExecutionStatusAndProgress(UUID jobExecutionId, ExportStrategyStatistic exportStatistic, CommonExportFails commonExportFails) {
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
      progress.setFailed(progress.getFailed() + commonExportFails.getDuplicatedUUIDAmount() + commonExportFails.getInvalidUUIDFormat().size());

      if (exports.size() == exportsCompleted && progress.getFailed() == 0) jobExecution.setStatus(JobExecution.StatusEnum.COMPLETED);
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
