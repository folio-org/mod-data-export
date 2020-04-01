package org.folio.service.file.upload;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.service.job.JobExecutionService;
import org.folio.service.file.definition.FileDefinitionService;
import org.folio.service.file.storage.FileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.folio.rest.jaxrs.model.FileDefinition.Status.COMPLETED;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.ERROR;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.IN_PROGRESS;

@Service
public class FileUploadServiceImpl implements FileUploadService {
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
    return fileDefinitionService.findFileDefinition(fileDefinitionId, tenantId)
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
    return fileDefinitionService.findFileDefinition(fileDefinitionId, tenantId)
      .compose(fileDefinition -> fileDefinitionService.update(fileDefinition.withStatus(ERROR), tenantId));
  }

}
