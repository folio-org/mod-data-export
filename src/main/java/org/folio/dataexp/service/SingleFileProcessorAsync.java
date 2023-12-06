package org.folio.dataexp.service;

import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.ExportExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SingleFileProcessorAsync extends SingleFileProcessor {


  @Autowired
  public SingleFileProcessorAsync(ExportExecutor exportExecutor, JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository, JobExecutionEntityRepository jobExecutionEntityRepository) {
    super(exportExecutor, jobExecutionExportFilesEntityRepository, jobExecutionEntityRepository);
  }

  @Override
  public void executeExport(JobExecutionExportFilesEntity export, ExportRequest.IdTypeEnum idType, CommonExportFails commonExportFails) {
    exportExecutor.exportAsynch(export, idType, commonExportFails);
  }
}
