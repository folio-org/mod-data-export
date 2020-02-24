package org.folio.service.upload.storage;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.file.FileSystem;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.upload.reader.SourceStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Component
public class LocalFileSystemStorage implements FileStorage {
  private static final String FILE_STORAGE_PATH = "./storage/files";
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private WorkerExecutor workerExecutor;
  private FileSystem fileSystem;

  public LocalFileSystemStorage(Vertx vertx) {
    this.workerExecutor = vertx.createSharedWorkerExecutor("local-file-storage-worker");
    this.fileSystem = vertx.fileSystem();
  }

  @Override
  public SourceStreamReader getReader() {
    throw new UnsupportedOperationException("Method is not implemented yet");
  }

  @Override
  public Future<FileDefinition> saveFileData(byte[] data, FileDefinition fileDefinition) {
    Promise<FileDefinition> promise = Promise.promise();
    String fileId = fileDefinition.getId();
    workerExecutor.<Void>executeBlocking(blockingFuture -> {
      try {
        String path = getFilePath(fileDefinition);
        if (!fileSystem.existsBlocking(path)) {
          fileSystem.mkdirsBlocking(path.substring(0, path.indexOf(fileDefinition.getFileName()) - 1));
          fileDefinition.setSourcePath(path);
        }
        final Path pathToFile = Paths.get(path);
        Files.write(pathToFile, data, pathToFile.toFile().exists() ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
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
