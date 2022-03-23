package org.folio.service.file.cleanup;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.folio.dao.FileDefinitionDao;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.file.storage.FileStorage;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StorageCleanupServiceImpl implements StorageCleanupService {

  private static final long FILE_DEFINITION_EXPIRATION_TIME_IN_MILLS = 3600_000;

  private static final Logger LOGGER = LogManager.getLogger(StorageCleanupServiceImpl.class);

  @Autowired
  private FileDefinitionDao fileDefinitionDao;
  @Autowired
  private FileStorage fileStorage;

  @Override
  public Future<Boolean> cleanStorage(OkapiConnectionParams params) {
    Promise<Boolean> promise = Promise.promise();
    return fileDefinitionDao.getExpiredEntries(getFileDefinitionExpirationDate(), params.getTenantId())
      .compose(fileDefinitions -> deleteExpiredFilesAndRelatedFileDefinitions(fileDefinitions, params.getTenantId()))
      .compose(compositeFuture -> {
        promise.complete(isFilesDeleted(compositeFuture));
        return promise.future();
      });
  }

  private Date getFileDefinitionExpirationDate() {
    return new Date(new Date().getTime() - FILE_DEFINITION_EXPIRATION_TIME_IN_MILLS);
  }

  private Future<CompositeFuture> deleteExpiredFilesAndRelatedFileDefinitions(List<FileDefinition> fileDefinitions, String tenantId) {
    LOGGER.info("Start process of removing files and file definitions, number of file definitions to clean up: {}", fileDefinitions.size());
    List<Future> deleteFilesFutures = fileDefinitions.stream()
      .map(fileDefinition -> deleteExpiredFileAndRelatedFileDefinition(fileDefinition, tenantId))
      .collect(Collectors.toList());
    return CompositeFuture.all(deleteFilesFutures);
  }

  private Future<Boolean> deleteExpiredFileAndRelatedFileDefinition(FileDefinition fileDefinition, String tenantId) {
    Promise<Boolean> promise = Promise.promise();
    return fileStorage.deleteFileAndParentDirectory(fileDefinition)
      .compose(isFileDeleted -> {
        fileDefinitionDao.deleteById(fileDefinition.getId(), tenantId);
        return Future.succeededFuture(true);
      })
      .compose(isFileDefinitionDeleted -> {
        if (Boolean.FALSE.equals(isFileDefinitionDeleted)) {
          LOGGER.error("File definition with id {} was not deleted", fileDefinition.getId());
        }
        promise.complete(isFileDefinitionDeleted);
        return promise.future();
      });
  }

  private boolean isFilesDeleted(CompositeFuture compositeFuture) {
    boolean isFilesDeleted = compositeFuture.<Boolean>list()
      .stream()
      .reduce((a, b) -> a && b)
      .orElse(false);
    if (isFilesDeleted) {
      LOGGER.info("File storage cleaning has been successfully completed");
    } else {
      LOGGER.warn("File storage cleaning was not completed successfully");
    }
    return isFilesDeleted;
  }

}
