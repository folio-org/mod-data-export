package org.folio.service.fileupload.storage;

import io.vertx.core.Vertx;

public final class FileStorageFactory {

  /**
   * Returns instance of the FileStorageService depending on the system properties: fileSystem/networkDrive
   *
   * @return FileStorageService
   * @see FileStorage
   */
  public static FileStorage create(Vertx vertx) {
    return new LocalFileSystemStorage(vertx);
  }
}
