package org.folio.dataexp.service.export.strategies;

import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;


public interface ExportStrategy {

  ExportStrategyStatistic saveMarcToLocalStorage(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, ExportedMarcListener exportedMarcListener);

  void setStatusBaseExportStatistic(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic);
}
