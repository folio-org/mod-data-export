package org.folio.service.manager.inputdatamanager.reader;

import org.folio.rest.jaxrs.model.FileDefinition;

import java.util.List;

/**
 * The root interface for source readers.
 */
public interface SourceReader {

  /**
   * Initialized source iterator configured by the given batch size
   *
   * @param fileDefinition file definition
   * @param batchSize      size batch for one stream iteration
   */
  void init(FileDefinition fileDefinition, int batchSize);

  /**
   * Returns {@code true} if the reader has more chunks to read.
   *
   * @return {@code true} if the reader has more chunks to read otherwise false
   */
  boolean hasNext();

  /**
   * Read the next list of string chunk.
   *
   * @return the next list of string chunk
   */
  List<String> readNext();

  /**
   * Closes the stream of the reader
   */
  void close();


}
