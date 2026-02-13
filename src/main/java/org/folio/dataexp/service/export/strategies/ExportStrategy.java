package org.folio.dataexp.service.export.strategies;

import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;

/** Interface for export strategies. */
public interface ExportStrategy {

  /**
   * Saves MARC records to local storage for the given export file entity.
   *
   * @param exportFilesEntity the export file entity
   * @param exportRequest the export request
   * @param exportedRecordsListener the listener for exported records
   * @return ExportStrategyStatistic containing export statistics
   */
  ExportStrategyStatistic saveOutputToLocalStorage(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportRequest exportRequest,
      ExportedRecordsListener exportedRecordsListener);

  /**
   * Sets the status of the export file entity based on export statistics.
   *
   * @param exportFilesEntity the export file entity
   * @param exportStatistic the export statistics
   */
  void setStatusBaseExportStatistic(
      JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic);
}
