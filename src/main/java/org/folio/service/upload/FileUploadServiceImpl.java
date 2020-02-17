package org.folio.service.upload;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.upload.definition.FileDefinitionService;
import org.folio.service.upload.storage.FileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.*;

@Service
public class FileUploadServiceImpl implements FileUploadService {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadServiceImpl.class);
  private FileDefinitionService fileDefinitionService;
  private FileStorage fileStorage;

  public FileUploadServiceImpl(@Autowired FileStorage fileStorage, @Autowired FileDefinitionService fileDefinitionService) {
    this.fileStorage = fileStorage;
    this.fileDefinitionService = fileDefinitionService;
  }

  @Override
  public Future<FileDefinition> createFileDefinition(FileDefinition fileDefinition, String tenantId) {
    fileDefinition.setStatus(NEW);
    return fileDefinitionService.save(fileDefinition, tenantId);
  }

  @Override
  public Future<FileDefinition> startUploading(String fileDefinitionId, String tenantId) {
    return findFileDefinition(fileDefinitionId, tenantId)
      .compose(fileDefinition -> fileDefinitionService.update(fileDefinition.withStatus(IN_PROGRESS), tenantId));
  }

  @Override
  public Future<FileDefinition> saveFileChunk(FileDefinition fileDefinition, byte[] data, String tenantId) {
    return fileStorage.saveFileData(data, fileDefinition);
  }

  @Override
  public Future<FileDefinition> completeUploading(FileDefinition fileDefinition, String tenantId) {
    /* Create job, link it to the file definition */
    return fileDefinitionService.update(fileDefinition.withStatus(COMPLETED), tenantId);
  }

  @Override
  public Future<FileDefinition> errorUploading(String fileDefinitionId, String tenantId) {
    return findFileDefinition(fileDefinitionId, tenantId)
      .compose(fileDefinition -> fileDefinitionService.update(fileDefinition.withStatus(ERROR), tenantId));
  }

  private Future<FileDefinition> findFileDefinition(String fileDefinitionId, String tenantId) {
    return fileDefinitionService.getById(fileDefinitionId, tenantId)
      .compose(optionalFileDefinition -> {
        if (optionalFileDefinition.isPresent()) {
          FileDefinition fileDefinition = optionalFileDefinition.get();
          return succeededFuture(fileDefinition);
        } else {
          String errorMessage = "FileDefinition not found. Id: " + fileDefinitionId;
          LOGGER.error(errorMessage);
          return Future.failedFuture(new NotFoundException(errorMessage));
        }
      });
  }
}
