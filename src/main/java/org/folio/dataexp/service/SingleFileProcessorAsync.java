package org.folio.dataexp.service;

import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.ExportExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SingleFileProcessorAsync extends SingleFileProcessor {

  @Autowired
  public SingleFileProcessorAsync(ExportExecutor exportExecutor, JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository) {
    super(exportExecutor, jobExecutionExportFilesEntityRepository);
  }

  @Override
  public void executeExport(JobExecutionExportFilesEntity export, ExportRequest.RecordTypeEnum recordType) {
    exportExecutor.exportAsynch(export, recordType);
  }
}
