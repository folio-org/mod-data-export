package org.folio.dataexp.service;

import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.ExportExecutor;
import org.folio.dataexp.service.export.ExportContext;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SingleFileProcessorAsync extends SingleFileProcessor {


  @Autowired
  public SingleFileProcessorAsync(ExportExecutor exportExecutor, JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository,
                                  JobExecutionEntityRepository jobExecutionEntityRepository, ErrorLogService errorLogService,
                                  ExportContext exportContext) {
    super(exportExecutor, jobExecutionExportFilesEntityRepository, jobExecutionEntityRepository, errorLogService, exportContext);
  }

  @Override
  public void executeExport(JobExecutionExportFilesEntity export, ExportRequest exportRequest, CommonExportFails commonExportFails, boolean lastExport) {
    exportExecutor.exportAsynch(export, exportRequest, commonExportFails, lastExport);
  }
}
