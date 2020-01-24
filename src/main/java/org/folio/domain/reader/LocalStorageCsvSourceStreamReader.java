package org.folio.domain.reader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class LocalStorageCsvSourceStreamReader implements SourceStreamReader<String> {

  private final String filePath;

  public LocalStorageCsvSourceStreamReader(String filePath) {
    this.filePath = filePath;
  }

  @Override
  public Stream<String> getSourceStream() throws IOException {
    return Files.lines(Paths.get(filePath));
  }
}
