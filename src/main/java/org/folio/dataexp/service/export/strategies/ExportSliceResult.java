package org.folio.dataexp.service.export.strategies;

import java.io.BufferedReader;
import java.nio.file.Path;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;

/** For multithreaded exports, report each thread's independent work back to the main thread. */
@Data
@AllArgsConstructor
public class ExportSliceResult {
  private Path outputFile;
  private Optional<BufferedReader> reader;
  private ExportStrategyStatistic statistic;
}
