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

  @Autowired
  public SingleFileProcessorAsync(ExportExecutor exportExecutor, JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository,
                                  JobExecutionEntityRepository jobExecutionEntityRepository, ErrorLogService errorLogService,
                                  FolioExecutionContext folioExecutionContext, FolioModuleMetadata folioModuleMetadata) {
    super(exportExecutor, folioExecutionContext, folioModuleMetadata, jobExecutionExportFilesEntityRepository, jobExecutionEntityRepository, errorLogService);
  }

  @Override
  public void executeExport(JobExecutionExportFilesEntity export, ExportRequest exportRequest, CommonExportFails commonExportFails) {
    var folioContextCopy = getFolioContextCopy(folioExecutionContext, folioModuleMetadata);
    var exportRequestCopy = getExportRequestCopy(exportRequest);
    exportExecutor.exportAsynch(export, exportRequestCopy, commonExportFails, folioContextCopy);
  }

  private FolioExecutionContext getFolioContextCopy(FolioExecutionContext context, FolioModuleMetadata folioModuleMetadata) {
    if (MapUtils.isNotEmpty(context.getOkapiHeaders())) {
      var headersCopy = SerializationUtils.clone((HashMap<String, Collection<String>>) context.getAllHeaders());
      return new DefaultFolioExecutionContext(folioModuleMetadata, headersCopy);
    }
    throw new IllegalStateException("Okapi headers not provided");
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
