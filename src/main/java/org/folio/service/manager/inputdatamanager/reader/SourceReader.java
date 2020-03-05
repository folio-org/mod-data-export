package org.folio.service.manager.inputdatamanager.reader;

import org.folio.rest.jaxrs.model.FileDefinition;

import java.util.Iterator;
import java.util.List;

/**
 * The root interface for source readers.
 */
public interface SourceReader {
  /**
   * Returns pre-initialized stream configured by the given batch size
   *
   * @param fileDefinition file definition
   * @param batchSize      size batch for one stream iteration
   * @return Stream
   */
   Iterator<List<String>> getFileContentIterator(FileDefinition fileDefinition, int batchSize);
}
