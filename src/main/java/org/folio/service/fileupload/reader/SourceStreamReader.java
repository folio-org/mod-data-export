package org.folio.service.fileupload.reader;

import org.folio.rest.jaxrs.model.FileDefinition;

import java.util.List;
import java.util.stream.Stream;

/**
 * The root interface for source readers.
 */
public interface SourceStreamReader {
  /**
   * Returns pre-initialized stream for the given file of configured by the given batch size
   *
   * @param fileDefinition file definition
   * @param batchSize      size batch for one stream iteration
   * @return
   */
  default Stream<List<String>> getSourceStream(FileDefinition fileDefinition, int batchSize) {
    return Stream.empty();
  }
}
