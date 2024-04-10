package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.exception.export.DataExportException;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.ExportExecutor;
import org.folio.dataexp.service.export.strategies.ExportedMarcListener;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.S3FilePathUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Log4j2
public class SingleFileProcessor {

  private static final int MIN_PROGRESS_EXPORT_STEP = 500;
  private static final int PROGRESS_EXPORT_ALL_STEP_INCREMENT = 10;
  protected final ExportExecutor exportExecutor;
  private final JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  protected final JobExecutionEntityRepository jobExecutionEntityRepository;
  private final JobExecutionService jobExecutionService;
  private final ErrorLogService errorLogService;
  private int exportIdsBatch;
  private String exportTmpStorage;

  @Value("#{ T(Integer).parseInt('${application.export-ids-batch}')}")
  protected void setExportIdsBatch(int exportIdsBatch) {
    this.exportIdsBatch = exportIdsBatch;
  }

  @Value("${application.export-tmp-storage}")
  protected void setExportTmpStorage(String exportTmpStorage) {
    this.exportTmpStorage = exportTmpStorage;
  }

  public void exportBySingleFile(UUID jobExecutionId, ExportRequest exportRequest, CommonExportStatistic commonExportStatistic) {
    var exports = jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId);
    if (exports.isEmpty()) {
      log.error("Nothing to export for job execution {}", jobExecutionId);
      var jobExecution = jobExecutionService.getById(jobExecutionId);
      var currentDate = new Date();
      jobExecution.setLastUpdatedDate(currentDate);
      jobExecution.setStatus(JobExecution.StatusEnum.FAIL);
      jobExecution.setCompletedDate(currentDate);

      var totalFailed = commonExportStatistic.getInvalidUUIDFormat().size();
      var progress = jobExecution.getProgress();
      progress.setFailed(totalFailed);
      progress.setExported(0);
      jobExecutionService.save(jobExecution);
      if (commonExportStatistic.isFailedToReadInputFile()) {
        errorLogService.saveFailedToReadInputFileError(jobExecutionId);
      } else {
        errorLogService.saveCommonExportFailsErrors(commonExportStatistic, totalFailed, jobExecutionId);
      }
      return;
    }
    try {
      Files.createDirectories(Path.of(S3FilePathUtils.getTempDirForJobExecutionId(exportTmpStorage, jobExecutionId)));
    } catch (IOException e) {
      throw new DataExportException("Can not create temp directory for job execution " + jobExecutionId);
    }
    var exportIterator = exports.iterator();

    var exportStrategyStatisticListener = new ExportedMarcListener(jobExecutionEntityRepository, getProgressExportUpdateStep(exportRequest), jobExecutionId);
    commonExportStatistic.setExportedMarcListener(exportStrategyStatisticListener);
    while (exportIterator.hasNext()) {
      var export = exportIterator.next();
      exportRequest.setLastExport(!exportIterator.hasNext());
      executeExport(export, exportRequest, commonExportStatistic);
    }
  }

  private int getProgressExportUpdateStep(ExportRequest exportRequest) {
    if (exportRequest.getAll()) {
      return Math.max(exportIdsBatch * PROGRESS_EXPORT_ALL_STEP_INCREMENT, MIN_PROGRESS_EXPORT_STEP);
    }
    return Math.max(exportIdsBatch, MIN_PROGRESS_EXPORT_STEP);
  }

  public void executeExport(JobExecutionExportFilesEntity export, ExportRequest exportRequest, CommonExportStatistic commonExportStatistic) {
    exportExecutor.export(export, exportRequest, commonExportStatistic);
  }
}
