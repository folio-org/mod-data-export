package org.folio.service.file.storage;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FileDefinition;

import java.util.List;

/**
 * File storage service, service to save files.
 */
public interface FileStorage {

  /**
   * Saves bytes to the storage asynchronously
   */
  Future<FileDefinition> saveFileDataAsync(byte[] data, FileDefinition fileDefinition);

  /**
   * Saves bytes to the storage asynchronously
   */
  Future<FileDefinition> saveFileDataAsyncCQL(List<String> uuids, FileDefinition fileDefinition);

  /**
   * Save bytes to the storage in blocking manner
   */
  FileDefinition saveFileDataBlocking(byte[] data, FileDefinition fileDefinition);

  /**
   * Deletes File and related parent directory from the storage and returns true if succeeded
   */
  Future<Boolean> deleteFileAndParentDirectory(FileDefinition fileDefinition);

  /**
   * Returned true if file exists with provided path, otherwise returns false
   */
  boolean isFileExist(String path);

}
