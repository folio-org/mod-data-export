package org.folio.service.cleanup;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.dao.FileDefinitionDao;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.upload.storage.FileStorage;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StorageCleanupServiceImpl implements StorageCleanupService {

  private static final long TIME_WITHOUT_CHANGES_VALUE_IN_MILLIS = 3600_000;

  private static final Logger LOGGER = LoggerFactory.getLogger(StorageCleanupServiceImpl.class);

  @Autowired
  private FileDefinitionDao fileDefinitionDao;
  @Autowired
  private FileStorage fileStorage;

  @Override
  public Future<Boolean> cleanStorage(OkapiConnectionParams params) {
    Promise<Boolean> promise = Promise.promise();
    return fileDefinitionDao.getExpiredEntries(getLastChangedDate(), params.getTenantId())
      .compose(fileDefinitions -> deleteExpiredFilesAndRelatedFileDefinitions(fileDefinitions, params.getTenantId()))
      .compose(compositeFuture -> {
        promise.complete(isFilesDeleted(compositeFuture));
        return promise.future();
      });
  }

  private Date getLastChangedDate() {
    return new Date(new Date().getTime() - TIME_WITHOUT_CHANGES_VALUE_IN_MILLIS);
  }

  private Future<CompositeFuture> deleteExpiredFilesAndRelatedFileDefinitions(List<FileDefinition> fileDefinitions, String tenantId) {
    List<Future> deleteFilesFutures = fileDefinitions.stream()
      .map(fileDefinition -> deleteExpiredFileAndRelatedFileDefinition(fileDefinition, tenantId))
      .collect(Collectors.toList());
    return CompositeFuture.all(deleteFilesFutures);
  }

  private Future<Boolean> deleteExpiredFileAndRelatedFileDefinition(FileDefinition fileDefinition, String tenantId) {
    Promise<Boolean> promise = Promise.promise();
    return fileStorage.deleteFileAndParentDirectory(fileDefinition)
      .compose(isFileDeleted -> {
        if (Boolean.TRUE.equals(isFileDeleted)) {
          return fileDefinitionDao.deleteById(fileDefinition.getId(), tenantId);
        }
        return Future.succeededFuture(false);
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
