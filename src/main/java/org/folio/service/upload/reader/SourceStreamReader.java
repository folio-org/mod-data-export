package org.folio.service.upload.reader;

import org.folio.rest.jaxrs.model.FileDefinition;

import java.util.List;
import java.util.stream.Stream;

/**
 * The root interface for source readers.
 */
public interface SourceStreamReader {
  /**
   * Returns pre-initialized stream configured by the given batch size
   *
   * @param fileDefinition file definition
   * @param batchSize      size batch for one stream iteration
   * @return Stream
   */
   Stream<List<String>> getSourceStream(FileDefinition fileDefinition, int batchSize);
}
