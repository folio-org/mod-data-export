package org.folio.service.file.definition;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.dao.FileDefinitionDao;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.UUID;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

@Service
public class FileDefinitionServiceImpl implements FileDefinitionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private FileDefinitionDao fileDefinitionDao;

  @Override
  public Future<Optional<FileDefinition>> getById(String id, String tenantId) {
    return fileDefinitionDao.getById(id, tenantId);
  }

  @Override
  public Future<FileDefinition> save(FileDefinition fileDefinition, String tenantId) {
    if (fileDefinition.getId() == null) {
      fileDefinition.setId(UUID.randomUUID().toString());
    }
    return fileDefinitionDao.save(fileDefinition, tenantId);
  }

  @Override
  public Future<FileDefinition> update(FileDefinition fileDefinition, String tenantId) {
    return fileDefinitionDao.update(fileDefinition, tenantId);
  }

  @Override
  public Future<FileDefinition> findFileDefinition(String fileDefinitionId, String tenantId) {
    return getById(fileDefinitionId, tenantId)
      .compose(optionalFileDefinition -> {
        if (optionalFileDefinition.isPresent()) {
          return succeededFuture(optionalFileDefinition.get());
        } else {
          String errorMessage = String.format("File definition not found with id %s", fileDefinitionId);
          LOGGER.error(errorMessage);
          return failedFuture(new NotFoundException(errorMessage));
        }
      });
  }
}
