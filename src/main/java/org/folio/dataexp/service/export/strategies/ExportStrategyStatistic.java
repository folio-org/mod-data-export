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
  private ExportedMarcListener exportedMarcListener;

  /**
   * Constructs an ExportStrategyStatistic.
   *
   * @param exportedMarcListener the listener for exported MARC records
   */
  public ExportStrategyStatistic(ExportedMarcListener exportedMarcListener) {
    this.exportedMarcListener = exportedMarcListener;
  }

  /**
   * Increments the exported count and updates the listener.
   */
  public void incrementExported() {
    this.exported = this.exported + 1;
    exportedMarcListener.incrementExported();
  }

  /**
   * Removes exported count and updates the listener.
   */
  public void removeExported() {
    exportedMarcListener.removeExported(exported);
    this.exported = 0;
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

  /**
   * Aggregate the statistics and missing results from one slice's processing into a total summary.
   */
  public void aggregate(ExportStrategyStatistic sliceStatistic) {
    this.exported = this.exported + sliceStatistic.getExported();
    this.failed = this.failed + sliceStatistic.getFailed();
    this.duplicatedSrs = this.duplicatedSrs + sliceStatistic.getDuplicatedSrs();
    addNotExistIdsAll(sliceStatistic.getNotExistIds());
  }

  /**
   * Likely due to I/O errors, records could not be added to the final file, so consider them all
   * failed instead.
   */
  public void failAll() {
    var total = this.exported + this.duplicatedSrs + this.failed;
    this.setDuplicatedSrs(0);
    this.removeExported();
    this.setFailed(total);
  }
}
