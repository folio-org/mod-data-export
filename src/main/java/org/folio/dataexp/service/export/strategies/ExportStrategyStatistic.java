package org.folio.dataexp.service.export.strategies;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;

/**
 * Holds statistics for export operations, such as exported, failed, and duplicated records.
 */
@Data
public class ExportStrategyStatistic {
  private int exported;
  private int failed;
  private int duplicatedSrs;
  private final List<UUID> notExistIds = new ArrayList<>();
  private ExportedRecordsListener exportedRecordsListener;

  /**
   * Constructs an ExportStrategyStatistic.
   *
   * @param exportedRecordsListener the listener for exported records
   */
  public ExportStrategyStatistic(ExportedRecordsListener exportedRecordsListener) {
    this.exportedRecordsListener = exportedRecordsListener;
  }

  /**
   * Increments the exported count and updates the listener.
   */
  public void incrementExported() {
    this.exported = this.exported + 1;
    exportedRecordsListener.incrementExported();
  }

  /**
   * Removes exported count and updates the listener.
   */
  public void removeExported() {
    this.exported = 0;
    exportedRecordsListener.removeExported(exported);
  }

  /**
   * Increments the failed count.
   */
  public void incrementFailed() {
    this.failed = this.failed + 1;
  }

  /**
   * Increments the duplicated SRS count.
   */
  public void incrementDuplicatedSrs() {
    this.duplicatedSrs = this.duplicatedSrs + 1;
  }

  /**
   * Adds all not existing IDs to the list.
   *
   * @param ids list of UUIDs not found
   */
  public void addNotExistIdsAll(List<UUID> ids) {
    notExistIds.addAll(ids);
  }
}
