package org.folio.dataexp.service;

import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.ExportExecutor;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.springframework.stereotype.Component;

/**
 * Asynchronous processor for exporting records by single file.
 */
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
  public SingleFileProcessorAsync(ExportExecutor exportExecutor,
      JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository,
      JobExecutionEntityRepository jobExecutionEntityRepository,
      JobExecutionService jobExecutionService, ErrorLogService errorLogService) {
    super(exportExecutor, jobExecutionExportFilesEntityRepository, jobExecutionEntityRepository,
        jobExecutionService, errorLogService);
  }

  /**
   * Executes the export asynchronously for a single file.
   *
   * @param export The JobExecutionExportFilesEntity.
   * @param exportRequest The export request.
   * @param commonExportStatistic Export statistics.
   */
  @Override
  public void executeExport(JobExecutionExportFilesEntity export, ExportRequest exportRequest,
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
    return ExportRequest.builder()
      .fileDefinitionId(exportRequest.getFileDefinitionId())
      .jobProfileId(exportRequest.getJobProfileId())
      .recordType(exportRequest.getRecordType())
      .idType(exportRequest.getIdType())
      .all(exportRequest.getAll())
      .quick(exportRequest.getQuick())
      .deletedRecords(exportRequest.getDeletedRecords())
      .suppressedFromDiscovery(exportRequest.getSuppressedFromDiscovery())
      .lastSlice(exportRequest.getLastSlice())
      .lastExport(exportRequest.getLastExport())
      .metadata(exportRequest.getMetadata()).build();
  }
}
