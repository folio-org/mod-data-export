package org.folio.service.inputdatamanager.reader;

import org.folio.rest.jaxrs.model.FileDefinition;

import java.io.IOException;
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
   Iterable<String> getSourceStream(FileDefinition fileDefinition, int batchSize) throws IOException;
}
