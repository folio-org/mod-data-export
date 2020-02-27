package org.folio.service.upload.storage;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.upload.reader.SourceStreamReader;

/**
 * File storage service.
 * Service to save files and reading files being stored.
 */
public interface FileStorage {

  /**
   * Returns instance of the source reader.
   *
   * @return SourceStreamReader
   * @see SourceStreamReader
   */
  SourceStreamReader getReader();

  /**
   * Saves file bytes to the storage and return its path
   */
  Future<FileDefinition> saveFileData(byte[] data, FileDefinition fileDefinition);

  /**
   * Deletes File and related parent directory from the storage and returns true if succeeded
   */
  Future<Boolean> deleteFileAndParentDirectory(FileDefinition fileDefinition);
}
