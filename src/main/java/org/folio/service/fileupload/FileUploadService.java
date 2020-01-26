package org.folio.service.fileupload;

import org.folio.service.fileupload.storage.FileStorageService;

public interface FileUploadService {

  default FileStorageService getFileStorage() {
    // Returns an implementation depending on the system properties (fileSystem/networkDrive)
    return new FileStorageService() {};
  }
}
