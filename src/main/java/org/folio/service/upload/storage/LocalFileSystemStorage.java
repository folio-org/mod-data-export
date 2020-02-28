package org.folio.service.upload.storage;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.file.FileSystem;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static java.lang.String.format;

@Component
public class LocalFileSystemStorage implements FileStorage {
  private static final String FILE_STORAGE_PATH = "./storage/files";
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private WorkerExecutor workerExecutor;
  private FileSystem fileSystem;

  public LocalFileSystemStorage(@Autowired Vertx vertx) {
    this.workerExecutor = vertx.createSharedWorkerExecutor("local-file-storage-worker");
    this.fileSystem = vertx.fileSystem();
  }

  @Override
  public Future<FileDefinition> saveFileDataAsync(byte[] data, FileDefinition fileDefinition) {
    Promise<FileDefinition> promise = Promise.promise();
    workerExecutor.<Void>executeBlocking(blockingFuture -> {
      try {
        saveFileData(data, fileDefinition);
      } catch (Exception e) {
        LOGGER.error("Error during save data to the local system's storage. FileId: {}", fileDefinition.getId(), e);
        promise.fail(e);
      }
      promise.complete(fileDefinition);
    }, null);
    return promise.future();
  }

  @Override
  public FileDefinition saveFileDataBlocking(byte[] data, FileDefinition fileDefinition) {
    try {
      saveFileData(data, fileDefinition);
    } catch (IOException e) {
      LOGGER.error("Error during save data to the local system's storage. FileId: {}", fileDefinition.getId(), e);
      throw new RuntimeException(e);
    }
    return fileDefinition;
  }

  private void saveFileData(byte[] data, FileDefinition fileDefinition) throws IOException {
    String path = getFilePath(fileDefinition);
    if (!fileSystem.existsBlocking(path)) {
      fileSystem.mkdirsBlocking(path.substring(0, path.indexOf(fileDefinition.getFileName()) - 1));
      fileDefinition.setSourcePath(path);
    }
    Path pathToFile = Paths.get(path);
    Files.write(pathToFile, data, pathToFile.toFile().exists() ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
  }

  private String getFilePath(FileDefinition fileDefinition) {
    if (fileDefinition.getId() == null || fileDefinition.getFileName() == null) {
      throw new IllegalArgumentException(format("File definition does not have necessary info, file definition: %s", fileDefinition));
    }
    if (fileDefinition.getSourcePath() == null) {
      return FILE_STORAGE_PATH + "/" + fileDefinition.getId() + "/" + fileDefinition.getFileName();
    } else {
      return fileDefinition.getSourcePath();
    }
  }
}
