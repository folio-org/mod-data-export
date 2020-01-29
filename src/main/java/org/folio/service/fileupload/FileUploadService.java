package org.folio.service.fileupload;

import org.folio.service.fileupload.storage.FileStorageService;

/**
 * File upload service. Service to upload files and read files being stored.
 */
public interface FileUploadService {

  /**
   * Returns instance of the FileStorageService depending on the system properties: fileSystem/networkDrive
   *
   * @return FileStorageService
   * @see FileStorageService
   */
   FileStorageService getFileStorageService();
}
