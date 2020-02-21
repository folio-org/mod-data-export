package org.folio.service.inputdatamanager.reader;

import org.folio.rest.jaxrs.model.FileDefinition;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Component
public class LocalStorageCsvSourceStreamReader implements SourceStreamReader {

  @Override
  public Iterable<String> getSourceStream(FileDefinition fileDefinition, int batchSize) throws IOException {
    return Files.lines(Paths.get(fileDefinition.getSourcePath()));
  }
}
