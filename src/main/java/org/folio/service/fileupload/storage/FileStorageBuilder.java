package org.folio.service.fileupload.storage;

import io.vertx.core.Vertx;

public final class FileStorageBuilder {

  /**
   * Returns instance of the FileStorageService depending on the system properties: fileSystem/networkDrive
   *
   * @return FileStorageService
   * @see FileStorage
   */
  public static FileStorage build(Vertx vertx) {
    return new LocalFileSystemStorage(vertx);
  }
}
