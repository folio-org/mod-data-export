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
  private ExportStrategyStatisticListener exportStrategyStatisticListener;

  public ExportStrategyStatistic(ExportStrategyStatisticListener exportStrategyStatisticListener) {
    this.exportStrategyStatisticListener = exportStrategyStatisticListener;
  }

  public void incrementExported() {
    this.exported = this.exported + 1;
    exportStrategyStatisticListener.incrementExported();
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
