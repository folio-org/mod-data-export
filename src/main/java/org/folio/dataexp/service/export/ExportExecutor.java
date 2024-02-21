package org.folio.dataexp.service.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionExportedFilesInner;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.exception.export.S3ExportsUploadException;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.CommonExportFails;
import org.folio.dataexp.service.export.strategies.ExportStrategyStatistic;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.dataexp.util.ErrorCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Log4j2
public class ExportExecutor {

  private final JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  private final JobExecutionEntityRepository jobExecutionEntityRepository;
  private final ExportStrategyFactory exportStrategyFactory;
  private final ErrorLogService errorLogService;
  private final ErrorLogEntityCqlRepository errorLogEntityCqlRepository;
  private final S3ExportsUploader s3Uploader;
  private final FileDefinitionEntityRepository fileDefinitionEntityRepository;

  @Async("singleExportFileTaskExecutor")
  public void exportAsynch(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, CommonExportFails commonExportFails, FolioExecutionContext folioExecutionContext) {
      try (var ctx = new FolioExecutionContextSetter(folioExecutionContext)) {
        export(exportFilesEntity, exportRequest, commonExportFails);
      }
  }

  public void export(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, CommonExportFails commonExportFails) {
    log.info("export:: Started export {} for job execution {}", exportFilesEntity.getFileLocation(), exportFilesEntity.getJobExecutionId());
    exportFilesEntity = jobExecutionExportFilesEntityRepository.getReferenceById(exportFilesEntity.getId());
    exportFilesEntity.setStatus(JobExecutionExportFilesStatus.ACTIVE);
    jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    var exportStrategy = exportStrategyFactory.getExportStrategy(exportRequest);
    var exportStatistic = exportStrategy.saveMarcToRemoteStorage(exportFilesEntity, exportRequest);
    commonExportFails.addToNotExistUUIDAll(exportStatistic.getNotExistIds());
    updateJobExecutionStatusAndProgress(exportFilesEntity.getJobExecutionId(), exportStatistic, commonExportFails, exportRequest);
    log.info("export:: Complete export {} for job execution {}", exportFilesEntity.getFileLocation(), exportFilesEntity.getJobExecutionId());
  }

  private synchronized void updateJobExecutionStatusAndProgress(UUID jobExecutionId, ExportStrategyStatistic exportStatistic, CommonExportFails commonExportFails, ExportRequest exportRequest) {
    var jobExecutionEntity = jobExecutionEntityRepository.getReferenceById(jobExecutionId);
    var jobExecution = jobExecutionEntity.getJobExecution();
    var progress = jobExecution.getProgress();
    progress.setExported(progress.getExported() + exportStatistic.getExported());
    progress.setFailed(progress.getFailed() + exportStatistic.getFailed());
    progress.setDuplicatedSrs(progress.getDuplicatedSrs() + exportStatistic.getDuplicatedSrs());
    var exports = jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId);
    long exportsCompleted = exports.stream().filter(e -> e.getStatus() == JobExecutionExportFilesStatus.COMPLETED).count();
    long exportsFailed = exports.stream().filter(e -> e.getStatus() == JobExecutionExportFilesStatus.FAILED).count();
    long exportsCompletedWithErrors = exports.stream().filter(e -> e.getStatus() == JobExecutionExportFilesStatus.COMPLETED_WITH_ERRORS).count();
    var currentDate = new Date();
    if (exportsCompleted + exportsFailed + exportsCompletedWithErrors == exports.size()) {
      if (Boolean.TRUE.equals(exportRequest.getAll())) {
        progress.setTotal(progress.getExported() - progress.getDuplicatedSrs() + progress.getFailed());
      }
      progress.setFailed(progress.getFailed() + commonExportFails.getDuplicatedUUIDAmount() + commonExportFails.getInvalidUUIDFormat().size());
      errorLogService.saveCommonExportFailsErrors(commonExportFails, progress.getFailed(), jobExecutionId);

      var errorCount = errorLogEntityCqlRepository.countByJobExecutionId(jobExecutionId);

      if (exports.size() == exportsCompleted && errorCount == 0) {
        jobExecution.setStatus(JobExecution.StatusEnum.COMPLETED);
      } else if (exports.size() == exportsFailed) {
        jobExecution.setStatus(JobExecution.StatusEnum.FAIL);
      } else {
        jobExecution.setStatus(JobExecution.StatusEnum.COMPLETED_WITH_ERRORS);
        log.error("export size: {}, errorCount: {}, exportsCompleted: {}, exportsCompletedWithErrors: {}, jobExecutionEntity: {}",
            exports.size(), errorCount, exportsCompleted, exportsCompletedWithErrors, jobExecutionEntity);
      }
      var filesForExport = exports.stream()
        .filter(e -> e.getStatus() == JobExecutionExportFilesStatus.COMPLETED || e.getStatus() == JobExecutionExportFilesStatus.COMPLETED_WITH_ERRORS).collect(Collectors.toList());
      var queryResult= fileDefinitionEntityRepository.getFileDefinitionByJobExecutionId(jobExecutionId.toString());
      var fileDefinition= queryResult.get(0).getFileDefinition();
      var initialFileName= FilenameUtils.getBaseName(fileDefinition.getFileName());
      var innerFileName = getDefaultFileName(fileDefinition, jobExecution);
      try {
        innerFileName = s3Uploader.upload(jobExecution, filesForExport, initialFileName);
      } catch (S3ExportsUploadException e) {
        jobExecution.setStatus(JobExecution.StatusEnum.FAIL);
        var errorMessage= String.format(ErrorCode.INVALID_EXPORT_FILE_DEFINITION_ID.getDescription(), fileDefinition.getId());
        errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.INVALID_EXPORT_FILE_DEFINITION_ID.getCode(), List.of(errorMessage), jobExecutionId);
        errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.NO_FILE_GENERATED.getCode(), List.of(ErrorCode.NO_FILE_GENERATED.getDescription()), jobExecutionId);
        log.error("updateJobExecutionStatusAndProgress:: error zip exports for jobExecutionId {} with exception {}", jobExecutionId, e.getMessage());
      }
      var innerFile = new JobExecutionExportedFilesInner().fileId(UUID.randomUUID())
        .fileName(FilenameUtils.getName(innerFileName));
      jobExecution.setExportedFiles(Set.of(innerFile));
      jobExecutionEntity.setStatus(jobExecution.getStatus());
      jobExecution.completedDate(currentDate);
      jobExecutionEntity.setCompletedDate(jobExecution.getCompletedDate());
    }
    jobExecution.setLastUpdatedDate(currentDate);
    jobExecutionEntityRepository.save(jobExecutionEntity);
    log.info("Job execution by id {} is updated with status {}", jobExecutionId, jobExecution.getStatus());
  }

  private String getDefaultFileName(FileDefinition fileDefinition, JobExecution jobExecution) {
    var initialFileName = FilenameUtils.getBaseName(fileDefinition.getFileName());
    return String.format("%s-%s.mrc", initialFileName, jobExecution.getHrId());
  }
}
