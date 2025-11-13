package org.folio.dataexp.service.export;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionExportedFilesInner;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.exception.export.S3ExportsUploadException;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.CommonExportStatistic;
import org.folio.dataexp.service.JobExecutionService;
import org.folio.dataexp.service.StorageCleanUpService;
import org.folio.dataexp.service.export.strategies.ExportStrategyStatistic;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.ErrorCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Executes export operations for job executions and handles status/progress updates.
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class ExportExecutor {

  private final JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  private final JobExecutionService jobExecutionService;
  private final ExportStrategyFactory exportStrategyFactory;
  private final ErrorLogService errorLogService;
  private final ErrorLogEntityCqlRepository errorLogEntityCqlRepository;
  private final S3ExportsUploader s3Uploader;
  private final FileDefinitionEntityRepository fileDefinitionEntityRepository;
  private final StorageCleanUpService storageCleanUpService;

  /**
   * Executes export asynchronously for a job execution file entity.
   *
   * @param exportFilesEntity the export file entity
   * @param exportRequest the export request
   * @param commonExportStatistic export statistics
   */
  @Async("singleExportFileTaskExecutor")
  public void exportAsynch(JobExecutionExportFilesEntity exportFilesEntity,
      ExportRequest exportRequest, CommonExportStatistic commonExportStatistic) {
    export(exportFilesEntity, exportRequest, commonExportStatistic);
  }

  /**
   * Executes export for a job execution file entity.
   *
   * @param exportFilesEntity the export file entity
   * @param exportRequest the export request
   * @param commonExportStatistic export statistics
   */
  public void export(JobExecutionExportFilesEntity exportFilesEntity,
      ExportRequest exportRequest, CommonExportStatistic commonExportStatistic) {
    log.info("export:: Started export {} for job execution {}",
        exportFilesEntity.getFileLocation(), exportFilesEntity.getJobExecutionId());
    exportFilesEntity = jobExecutionExportFilesEntityRepository.getReferenceById(
        exportFilesEntity.getId());
    exportFilesEntity.setStatus(JobExecutionExportFilesStatus.ACTIVE);
    jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    var exportStrategy = exportStrategyFactory.getExportStrategy(exportRequest);
    var exportStatistic = exportStrategy.saveOutputToLocalStorage(
        exportFilesEntity, exportRequest, commonExportStatistic.getExportedRecordsListener());
    commonExportStatistic.addToNotExistUuidAll(exportStatistic.getNotExistIds());
    synchronized (this) {
      exportStrategy.setStatusBaseExportStatistic(exportFilesEntity, exportStatistic);
      jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
      log.info("export:: Complete export {} for job execution {}",
          exportFilesEntity.getFileLocation(), exportFilesEntity.getJobExecutionId());
      updateJobExecutionStatusAndProgress(
          exportFilesEntity.getJobExecutionId(), exportStatistic, commonExportStatistic,
          exportRequest);
    }
  }

  /**
   * Updates job execution status and progress after export.
   *
   * @param jobExecutionId the job execution ID
   * @param exportStatistic export statistics
   * @param commonExportStatistic common export statistics
   * @param exportRequest the export request
   */
  private void updateJobExecutionStatusAndProgress(UUID jobExecutionId,
      ExportStrategyStatistic exportStatistic, CommonExportStatistic commonExportStatistic,
      ExportRequest exportRequest) {
    var jobExecution = jobExecutionService.getById(jobExecutionId);
    var progress = jobExecution.getProgress();
    progress.setFailed(progress.getFailed() + exportStatistic.getFailed());
    progress.setDuplicatedSrs(progress.getDuplicatedSrs() + exportStatistic.getDuplicatedSrs());
    var exports = jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId);
    long exportsCompleted = exports.stream()
        .filter(e -> e.getStatus() == JobExecutionExportFilesStatus.COMPLETED)
        .count();
    long exportsFailed = exports.stream()
        .filter(e -> e.getStatus() == JobExecutionExportFilesStatus.FAILED)
        .count();
    long exportsCompletedWithErrors = exports.stream()
        .filter(e -> e.getStatus() == JobExecutionExportFilesStatus.COMPLETED_WITH_ERRORS)
        .count();
    var currentDate = new Date();
    if (exportsCompleted + exportsFailed + exportsCompletedWithErrors == exports.size()) {
      progress.setExported(
          commonExportStatistic.getExportedRecordsListener().getExportedCount().get());
      if (Boolean.TRUE.equals(exportRequest.getAll())) {
        progress.setTotal(
            progress.getExported() - progress.getDuplicatedSrs() + progress.getFailed());
      }
      progress.setFailed(getFailedNumber(jobExecution.getProgress(), commonExportStatistic));
      errorLogService.saveCommonExportFailsErrors(
          commonExportStatistic, progress.getFailed(), jobExecutionId);

      var errorCount = errorLogEntityCqlRepository.countByJobExecutionId(jobExecutionId);

      if (exports.size() == exportsCompleted && errorCount == 0) {
        jobExecution.setStatus(JobExecution.StatusEnum.COMPLETED);
      } else if (exports.size() == exportsFailed) {
        jobExecution.setStatus(JobExecution.StatusEnum.FAIL);
      } else {
        jobExecution.setStatus(JobExecution.StatusEnum.COMPLETED_WITH_ERRORS);
        log.error(
            "export size: {}, errorCount: {}, exportsCompleted: {}, "
                + "exportsCompletedWithErrors: {}, jobExecution: {}",
            exports.size(), errorCount, exportsCompleted, exportsCompletedWithErrors,
            jobExecution);
      }
      var filesForExport = exports.stream()
          .filter(e -> e.getStatus() == JobExecutionExportFilesStatus.COMPLETED
              || e.getStatus() == JobExecutionExportFilesStatus.COMPLETED_WITH_ERRORS)
          .toList();
      var queryResult = fileDefinitionEntityRepository.getFileDefinitionByJobExecutionId(
          jobExecutionId.toString());
      var fileDefinition = queryResult.getFirst().getFileDefinition();
      var initialFileName = FilenameUtils.getBaseName(fileDefinition.getFileName());
      try {
        var innerFileName = s3Uploader.upload(jobExecution, filesForExport, initialFileName);
        var innerFile = new JobExecutionExportedFilesInner()
            .fileId(UUID.randomUUID())
            .fileName(FilenameUtils.getName(innerFileName));
        jobExecution.setExportedFiles(Set.of(innerFile));
      } catch (S3ExportsUploadException e) {
        jobExecution.setStatus(JobExecution.StatusEnum.FAIL);
        errorLogService.saveGeneralErrorWithMessageValues(
            ErrorCode.INVALID_EXPORT_FILE_DEFINITION_ID.getCode(),
            List.of(fileDefinition.getId().toString()), jobExecutionId);
        errorLogService.saveGeneralErrorWithMessageValues(
            ErrorCode.NO_FILE_GENERATED.getCode(),
            List.of(ErrorCode.NO_FILE_GENERATED.getDescription()), jobExecutionId);
        log.error(
            "updateJobExecutionStatusAndProgress:: error zip exports for jobExecutionId {} "
                + "with exception {}",
            jobExecutionId, e.getMessage());
      }
      jobExecution.completedDate(currentDate);
      storageCleanUpService.cleanExportIdEntities(jobExecutionId);
    }
    jobExecution.setLastUpdatedDate(currentDate);
    jobExecutionService.save(jobExecution);
    log.info(
        "Job execution by id {} is updated with status {}",
        jobExecutionId, jobExecution.getStatus());
  }

  /**
   * Calculates the number of failed records.
   *
   * @param progress job execution progress
   * @param commonExportStatistic common export statistics
   * @return number of failed records
   */
  private int getFailedNumber(JobExecutionProgress progress,
      CommonExportStatistic commonExportStatistic) {
    return progress.getFailed()
        + commonExportStatistic.getDuplicatedUuidAmount()
        + commonExportStatistic.getInvalidUuidFormat().size();
  }
}
