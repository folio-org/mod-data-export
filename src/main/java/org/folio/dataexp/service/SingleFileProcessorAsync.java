package org.folio.dataexp.service;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.ExportExecutor;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;

@Component
public class SingleFileProcessorAsync extends SingleFileProcessor {

  private FolioExecutionContext folioExecutionContext;
  private FolioModuleMetadata folioModuleMetadata;

  @Autowired
  public SingleFileProcessorAsync(ExportExecutor exportExecutor, JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository,
                                  JobExecutionEntityRepository jobExecutionEntityRepository, ErrorLogService errorLogService) {
    super(exportExecutor, jobExecutionExportFilesEntityRepository, jobExecutionEntityRepository, errorLogService);
  }

  @Autowired
  protected void setFolioExecutionContext(FolioExecutionContext folioExecutionContext) {
    this.folioExecutionContext = folioExecutionContext;
  }

  @Autowired
  protected void setFolioModuleMetadata(FolioModuleMetadata folioModuleMetadata) {
    this.folioModuleMetadata = folioModuleMetadata;
  }

  @Override
  public void executeExport(JobExecutionExportFilesEntity export, ExportRequest exportRequest, CommonExportFails commonExportFails, boolean lastExport) {
    var folioContextCopy = createFolioContextCopy(folioExecutionContext, folioModuleMetadata);
    exportExecutor.exportAsync(export, exportRequest, commonExportFails, lastExport, folioContextCopy);
  }

  private FolioExecutionContext createFolioContextCopy(FolioExecutionContext context, FolioModuleMetadata folioModuleMetadata) {
    if (MapUtils.isNotEmpty(context.getOkapiHeaders())) {
      var headersCopy = SerializationUtils.clone((HashMap<String, Collection<String>>) context.getAllHeaders());
      return new DefaultFolioExecutionContext(folioModuleMetadata, headersCopy);
    }
    throw new IllegalStateException("Okapi headers not provided");
  }
}
