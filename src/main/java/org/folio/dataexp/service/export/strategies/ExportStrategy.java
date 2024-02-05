package org.folio.dataexp.service.export.strategies;

import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;


public interface ExportStrategy {

  ExportStrategyStatistic saveMarcToRemoteStorage(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, boolean lastExport);
}
