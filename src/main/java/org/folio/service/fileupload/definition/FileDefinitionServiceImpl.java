package org.folio.service.fileupload.definition;

import io.vertx.core.Future;
import org.folio.dao.FileDefinitionDao;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class FileDefinitionServiceImpl implements FileDefinitionService {

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
}
