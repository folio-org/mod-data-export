package org.folio.dataexp.service.export.strategies;

import lombok.Data;

@Data
public class ExportStrategyStatistic {
  private int exported;
  private int failed;
  private int duplicatedSrs;
}
