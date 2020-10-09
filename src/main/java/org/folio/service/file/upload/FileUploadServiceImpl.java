package org.folio.service.file.upload;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.HttpStatus;
import org.folio.clients.InventoryClient;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.Progress;
import org.folio.service.file.definition.FileDefinitionService;
import org.folio.service.file.storage.FileStorage;
import org.folio.service.job.JobExecutionService;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.COMPLETED;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.ERROR;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.IN_PROGRESS;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.NEW;

@Service
public class FileUploadServiceImpl implements FileUploadService {
  private FileDefinitionService fileDefinitionService;
  private JobExecutionService jobExecutionService;
  private FileStorage fileStorage;
  @Autowired
  private InventoryClient inventoryClient;

  public FileUploadServiceImpl(@Autowired FileStorage fileStorage, @Autowired FileDefinitionService fileDefinitionService,
                               @Autowired JobExecutionService jobExecutionService) {
    this.fileStorage = fileStorage;
    this.fileDefinitionService = fileDefinitionService;
    this.jobExecutionService = jobExecutionService;
  }

  @Override
  public Future<FileDefinition> startUploading(String fileDefinitionId, String tenantId) {
    return fileDefinitionService.getById(fileDefinitionId, tenantId)
      .compose(fileDefinition -> {
        if (!fileDefinition.getStatus().equals(NEW)) {
          throw new ServiceException(HttpStatus.HTTP_BAD_REQUEST, ErrorCode.FILE_ALREADY_UPLOADED);
        }
        return fileDefinitionService.update(fileDefinition.withStatus(IN_PROGRESS), tenantId);
      });
  }

  @Override
  public Future<FileDefinition> saveFileChunk(FileDefinition fileDefinition, byte[] data, String tenantId) {
    if (data.length > 0) {
      return fileStorage.saveFileDataAsync(data, fileDefinition)
        .compose(ar -> updateFileDefinitionWithJobExeution(new JobExecution(), fileDefinition, tenantId));
    }
    return Future.succeededFuture(fileDefinition);
  }

  @Override
  public Future<FileDefinition> completeUploading(FileDefinition fileDefinition, String tenantId) {
    return fileDefinitionService.update(fileDefinition.withStatus(COMPLETED), tenantId);
  }

  @Override
  public Future<FileDefinition> saveUUIDsByCQL(FileDefinition fileDefinition, String query, OkapiConnectionParams params) {
    Optional<JsonObject> instancesUUIDs = inventoryClient.getInstancesBulkUUIDs(query, params);
    List<String> ids = new ArrayList<>();
    if (instancesUUIDs.isPresent()) {
      JsonArray jsonIds = instancesUUIDs.get().getJsonArray("ids");
      if (jsonIds.size() > 0) {
        for (Object id : jsonIds) {
          ids.add((String) id);
        }
        return fileStorage.saveFileDataAsyncCQL(ids, fileDefinition)
          .compose(ar -> updateFileDefinitionWithJobExeution(getJobExecutionWithProgress(jsonIds.size()),
            fileDefinition, params.getTenantId()));
      }
    }
    return Future.succeededFuture(fileDefinition);
  }

  @Override
  public Future<FileDefinition> errorUploading(String fileDefinitionId, String tenantId) {
    return fileDefinitionService.getById(fileDefinitionId, tenantId)
      .compose(fileDefinition -> fileDefinitionService.update(fileDefinition.withStatus(ERROR), tenantId));
  }


  private Future<FileDefinition> updateFileDefinitionWithJobExeution(JobExecution jobExecution, FileDefinition fileDefinition, String tenantId) {
    return jobExecutionService.save(jobExecution, tenantId)
      .compose(savedJob -> fileDefinitionService.update(fileDefinition.withJobExecutionId(savedJob.getId()), tenantId));
  }

  private JobExecution getJobExecutionWithProgress(int total) {
    return new JobExecution().
      withProgress(new Progress()
        .withTotal(String.valueOf(total)));
  }


}
