package org.folio.service.upload.storage;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.upload.reader.SourceStreamReader;

/**
 * File storage service, service to save files.
 */
public interface FileStorage {

  /**
   * Saves file bytes to the storage asynchronously
   *
   */
  Future<FileDefinition> saveFileDataAsync(byte[] data, FileDefinition fileDefinition);

  FileDefinition saveFileDataBlocking(byte[] data, FileDefinition fileDefinition);
}
