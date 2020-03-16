package org.folio.service.file.storage;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FileDefinition;

/**
 * File storage service, service to save files.
 */
public interface FileStorage {

  /**
   * Saves bytes to the storage asynchronously
   */
  Future<FileDefinition> saveFileDataAsync(byte[] data, FileDefinition fileDefinition);

  /**
   * Save bytes to the storage in blocking manner
   */
  FileDefinition saveFileDataBlocking(byte[] data, FileDefinition fileDefinition);

  /**
   * Deletes File and related parent directory from the storage and returns true if succeeded
   */
  Future<Boolean> deleteFileAndParentDirectory(FileDefinition fileDefinition);
}
