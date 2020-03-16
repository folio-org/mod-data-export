package org.folio.service.file.upload;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.service.job.JobExecutionService;
import org.folio.service.file.definition.FileDefinitionService;
import org.folio.service.file.storage.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;

import java.lang.invoke.MethodHandles;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.COMPLETED;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.ERROR;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.IN_PROGRESS;

@Service
public class FileUploadServiceImpl implements FileUploadService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private FileDefinitionService fileDefinitionService;
  private JobExecutionService jobExecutionService;
  private FileStorage fileStorage;

  public FileUploadServiceImpl(@Autowired FileStorage fileStorage, @Autowired FileDefinitionService fileDefinitionService,
  @Autowired JobExecutionService jobExecutionService) {
    this.fileStorage = fileStorage;
    this.fileDefinitionService = fileDefinitionService;
    this.jobExecutionService = jobExecutionService;
  }

  @Override
  public Future<FileDefinition> startUploading(String fileDefinitionId, String tenantId) {
    return findFileDefinition(fileDefinitionId, tenantId)
      .compose(fileDefinition -> fileDefinitionService.update(fileDefinition.withStatus(IN_PROGRESS), tenantId));
  }

  @Override
  public Future<FileDefinition> saveFileChunk(FileDefinition fileDefinition, byte[] data, String tenantId) {
    return fileStorage.saveFileDataAsync(data, fileDefinition);
  }

  @Override
  public Future<FileDefinition> completeUploading(FileDefinition fileDefinition, String tenantId) {
    JobExecution jobExecution = new JobExecution();
    return jobExecutionService.save(jobExecution, tenantId).compose(savedJob ->
      fileDefinitionService.update(fileDefinition.withStatus(COMPLETED).withJobExecutionId(savedJob.getId()), tenantId));
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
