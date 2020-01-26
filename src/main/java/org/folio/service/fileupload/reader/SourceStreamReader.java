package org.folio.service.fileupload.reader;

import org.folio.rest.jaxrs.model.FileDefinition;

import java.util.List;
import java.util.stream.Stream;

public interface SourceStreamReader {
  default Stream<List<String>> getSourceStream(FileDefinition fileDefinition, int batchSize) {
    return Stream.empty();
  }
}
