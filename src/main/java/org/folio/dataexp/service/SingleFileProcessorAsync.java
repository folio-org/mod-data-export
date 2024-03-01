package org.folio.dataexp.service;

import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.ExportExecutor;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class SingleFileProcessorAsync extends SingleFileProcessor {

  @Autowired
  public SingleFileProcessorAsync(ExportExecutor exportExecutor, JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository,
                                  JobExecutionEntityRepository jobExecutionEntityRepository, ErrorLogService errorLogService) {
    super(exportExecutor, jobExecutionExportFilesEntityRepository, jobExecutionEntityRepository, errorLogService);
  }

  @Override
  public void executeExport(JobExecutionExportFilesEntity export, ExportRequest exportRequest, CommonExportStatistic commonExportStatistic) {
    var exportRequestCopy = getExportRequestCopy(exportRequest);
    exportExecutor.exportAsynch(export, exportRequestCopy, commonExportStatistic);
  }

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
