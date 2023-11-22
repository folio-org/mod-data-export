package org.folio.dataexp.service.export.strategies;

import lombok.Data;

@Data
public class ExportStrategyStatistic {
  private int exported;
  private int failed;
  private int duplicatedSrs;

  public void incrementExported() {
    this.exported = this.exported + 1;
  }

  public void incrementFailed() {
    this.failed = this.failed + 1;
  }

  public void incrementDuplicatedSrs() {
    this.duplicatedSrs = this.duplicatedSrs + 1;
  }
}
