package org.folio.dataexp.service.export.strategies;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * For multithreaded exports, report each thread's indpendent work back to the main thread.
 */
@Data
@AllArgsConstructor
public class ExportSliceResult {
  private Path outputFile;
  private ExportStrategyStatistic statistic;
}
