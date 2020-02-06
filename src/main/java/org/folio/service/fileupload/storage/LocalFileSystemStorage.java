package org.folio.service.fileupload.storage;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.fileupload.reader.SourceStreamReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class LocalFileSystemStorage implements FileStorage {
  private static final String FILE_STORAGE_PATH = "./storage/files";
  private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileSystemStorage.class);

  private Vertx vertx;
  private FileSystem fs;

  public LocalFileSystemStorage(Vertx vertx) {
    this.vertx = vertx;
    this.fs = vertx.fileSystem();
  }

  @Override
  public SourceStreamReader getReader() {
    throw new UnsupportedOperationException("Method is not implemented yet");
  }

  @Override
  public Future<FileDefinition> saveFileData(byte[] data, FileDefinition fileDefinition) {
    Promise<FileDefinition> promise = Promise.promise();
    String fileId = fileDefinition.getId();
    vertx.<Void>executeBlocking(b -> {
      try {
        String path = getFilePath(fileDefinition);
        if (!fs.existsBlocking(path)) {
          fs.mkdirsBlocking(path.substring(0, path.indexOf(fileDefinition.getFileName()) - 1));
        }
        final Path pathToFile = Paths.get(path);
        Files.write(pathToFile, data, pathToFile.toFile().exists() ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
        fileDefinition.setSourcePath(path);
        promise.complete(fileDefinition);
      } catch (Exception e) {
        LOGGER.error("Error during save data to the local system's storage. FileId: {}", fileId, e);
        promise.fail(e);
      }
    }, null);
    return promise.future();
  }

  private String getFilePath(FileDefinition fileDefinition) {
    if (fileDefinition.getSourcePath() == null) {
      return FILE_STORAGE_PATH + "/" + fileDefinition.getId() + "/" + fileDefinition.getFileName();
    } else {
      return fileDefinition.getSourcePath();
    }
  }
}
