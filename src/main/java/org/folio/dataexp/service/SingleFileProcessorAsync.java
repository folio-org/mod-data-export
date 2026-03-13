package org.folio.dataexp.service;

import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.ExportExecutor;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.springframework.stereotype.Component;

/** Asynchronous processor for exporting records by single file. */
@Component
public class SingleFileProcessorAsync extends SingleFileProcessor {

  /**
   * Constructs an asynchronous single file processor.
   *
   * @param exportExecutor The export executor.
   * @param jobExecutionExportFilesEntityRepository Repository for export files.
   * @param jobExecutionEntityRepository Repository for job executions.
   * @param jobExecutionService Service for job executions.
   * @param errorLogService Service for error logs.
   */
  public SingleFileProcessorAsync(
      ExportExecutor exportExecutor,
      JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository,
      JobExecutionEntityRepository jobExecutionEntityRepository,
      JobExecutionService jobExecutionService,
      ErrorLogService errorLogService) {
    super(
        exportExecutor,
        jobExecutionExportFilesEntityRepository,
        jobExecutionEntityRepository,
        jobExecutionService,
        errorLogService);
  }

  /**
   * Executes the export asynchronously for a single file.
   *
   * @param export The JobExecutionExportFilesEntity.
   * @param exportRequest The export request.
   * @param commonExportStatistic Export statistics.
   */
  @Override
  public void executeExport(
      JobExecutionExportFilesEntity export,
      ExportRequest exportRequest,
      CommonExportStatistic commonExportStatistic) {
    var exportRequestCopy = getExportRequestCopy(exportRequest);
    exportExecutor.exportAsynch(export, exportRequestCopy, commonExportStatistic);
  }

  /**
   * Creates a copy of the export request.
   *
   * @param exportRequest The export request.
   * @return A copy of ExportRequest.
   */
  private ExportRequest getExportRequestCopy(ExportRequest exportRequest) {
    var exportRequestCopy = new ExportRequest();
    exportRequestCopy.setFileDefinitionId(exportRequest.getFileDefinitionId());
    exportRequestCopy.setJobProfileId(exportRequest.getJobProfileId());
    exportRequestCopy.setRecordType(exportRequest.getRecordType());
    exportRequestCopy.setIdType(exportRequest.getIdType());
    exportRequestCopy.setAll(exportRequest.getAll());
    exportRequestCopy.setQuick(exportRequest.getQuick());
    exportRequestCopy.setDeletedRecords(exportRequest.getDeletedRecords());
    exportRequestCopy.setSuppressedFromDiscovery(exportRequest.getSuppressedFromDiscovery());
    exportRequestCopy.setLastSlice(exportRequest.getLastSlice());
    exportRequestCopy.setLastExport(exportRequest.getLastExport());
    exportRequestCopy.setMetadata(exportRequest.getMetadata());
    return exportRequestCopy;
  }
}
