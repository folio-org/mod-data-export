package org.folio.dataexp.service.export.strategies;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class ExportStrategyStatistic {
  private int exported;
  private int failed;
  private int duplicatedSrs;
  private final List<UUID> notExistIds = new ArrayList<>();
  private ExportedMarcListener exportedMarcListener;

  public ExportStrategyStatistic(ExportedMarcListener exportedMarcListener) {
    this.exportedMarcListener = exportedMarcListener;
  }

  public void incrementExported() {
    this.exported = this.exported + 1;
    exportedMarcListener.incrementExported();
  }

  public void removeExported() {
    this.exported = 0;
    exportedMarcListener.removeExported(exported);
  }

  public void incrementFailed() {
    this.failed = this.failed + 1;
  }

  public void incrementDuplicatedSrs() {
    this.duplicatedSrs = this.duplicatedSrs + 1;
  }

  public void addNotExistIdsAll(List<UUID> ids) {
    notExistIds.addAll(ids);
  }
}
