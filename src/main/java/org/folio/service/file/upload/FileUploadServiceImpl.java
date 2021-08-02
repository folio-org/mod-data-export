package org.folio.service.file.upload;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.HttpStatus;
import org.folio.clients.InventoryClient;
import org.folio.clients.UsersClient;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.Progress;
import org.folio.rest.jaxrs.model.QuickExportRequest;
import org.folio.service.file.definition.FileDefinitionService;
import org.folio.service.file.storage.FileStorage;
import org.folio.service.job.JobExecutionService;
import org.folio.service.logs.ErrorLogService;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.folio.rest.jaxrs.model.FileDefinition.Status.COMPLETED;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.ERROR;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.IN_PROGRESS;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.NEW;

@Service
public class FileUploadServiceImpl implements FileUploadService {
  @Autowired
  private FileDefinitionService fileDefinitionService;
  @Autowired
  private JobExecutionService jobExecutionService;
  @Autowired
  private FileStorage fileStorage;
  @Autowired
  private InventoryClient inventoryClient;
  @Autowired
  private ErrorLogService errorLogService;
  @Autowired
  private UsersClient usersClient;

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
    return fileStorage.saveFileDataAsync(data, fileDefinition)
      .compose(ar -> {
        if (Objects.isNull(fileDefinition.getJobExecutionId())) {
          return updateFileDefinitionWithJobExecution(new JobExecution(), fileDefinition, tenantId);
        }
        return Future.succeededFuture(fileDefinition);
      });
  }

  @Override
  public Future<FileDefinition> completeUploading(FileDefinition fileDefinition, String tenantId) {
    return fileDefinitionService.update(fileDefinition.withStatus(COMPLETED), tenantId);
  }

  @Override
  public Future<FileDefinition> saveUUIDsByCQL(FileDefinition fileDefinition, String query, OkapiConnectionParams params) {
    if (StringUtils.isNotBlank(query)) {
      Promise<Optional<JsonObject>> instancesIdsPromise = Promise.promise();
      Vertx.vertx().executeBlocking((blockingFeature) -> {
        Optional<JsonObject> instancesUUIDs = inventoryClient.getInstancesBulkUUIDs(query, params);
        instancesIdsPromise.complete(instancesUUIDs);
        blockingFeature.complete();
      });
      return instancesIdsPromise.future().compose(optionalInstancesUUIDs -> {
        List<String> ids = new ArrayList<>();
        if (optionalInstancesUUIDs.isPresent()) {
          JsonArray jsonIds = optionalInstancesUUIDs.get().getJsonArray("ids");
          if (jsonIds.size() > 0) {
            for (Object id : jsonIds) {
              ids.add(((JsonObject) id).getString("id"));
            }
            JobExecution jobExecution = new JobExecution().withProgress(new Progress().withTotal(jsonIds.size()));
            return fileStorage.saveFileDataAsyncCQL(ids, fileDefinition)
              .compose(ar -> updateFileDefinitionWithJobExecution(jobExecution, fileDefinition, params.getTenantId()));
          }
        }
        return updateFileDefinitionWithJobExecution(new JobExecution(), fileDefinition, params.getTenantId());
      });
    }
    return updateFileDefinitionWithJobExecution(new JobExecution(), fileDefinition, params.getTenantId());
  }

  @Override
  public Future<FileDefinition> errorUploading(String fileDefinitionId, String tenantId) {
    return fileDefinitionService.getById(fileDefinitionId, tenantId)
      .compose(fileDefinition -> fileDefinitionService.update(fileDefinition.withStatus(ERROR), tenantId));
  }

  @Override
  public Future<FileDefinition> uploadFileDependsOnTypeForQuickExport(QuickExportRequest request, FileDefinition fileDefinition, OkapiConnectionParams params) {
    Promise<FileDefinition> promise = Promise.promise();
    jobExecutionService.getById(fileDefinition.getJobExecutionId(), params.getTenantId())
      .onSuccess(jobExecution ->
        fileDefinitionService.update(fileDefinition.withStatus(IN_PROGRESS), params.getTenantId())
          .onSuccess(inProgressFileDef -> {
            if (QuickExportRequest.Type.CQL.equals(request.getType())) {
              uploadFileWithCQLQueryQuickExport(request, inProgressFileDef, jobExecution, params)
                .onSuccess(promise::complete)
                .onFailure(ar -> failFileDefinitionAndJobExecution(promise, inProgressFileDef, jobExecution, request, ar.getCause(), params));
            } else {
              List<String> uuids = request.getUuids();
              fileStorage.saveFileDataAsyncCQL(uuids, inProgressFileDef)
                .onComplete(ar -> {
                  if (ar.succeeded()) {
                    updateFileDefinitionAndJobExecution(jobExecution.withProgress(new Progress().withTotal(uuids.size())), ar.result(), request, params)
                      .onSuccess(promise::complete)
                      .onFailure(async -> promise.fail(async.getCause()));
                  } else {
                    failFileDefinitionAndJobExecution(promise, inProgressFileDef, jobExecution, request, ar.cause(), params);
                  }
                });
            }
          }).onFailure(ar -> failFileDefinitionAndJobExecution(promise, fileDefinition, jobExecution, request, ar.getCause(), params))

      ).onFailure(ar -> failFileDefinitionAndJobExecution(promise, fileDefinition, null, request, ar.getCause(), params));

    return promise.future();
  }

  private Future<FileDefinition> uploadFileWithCQLQueryQuickExport(QuickExportRequest request, FileDefinition fileDefinition, JobExecution jobExecution, OkapiConnectionParams params) {
    Promise<FileDefinition> promise = Promise.promise();
    Optional<JsonObject> instancesUUIDs = inventoryClient.getInstancesBulkUUIDs(request.getCriteria(), params);
    if (instancesUUIDs.isPresent()) {
      saveUUIDsFromJson(instancesUUIDs.get(), fileDefinition, jobExecution, request, params)
        .onSuccess(promise::complete)
        .onFailure(ar -> failFileDefinitionAndJobExecution(promise, fileDefinition, jobExecution, request, ar.getCause(), params));
    } else {
      saveUUIDsFromJson(new JsonObject(), fileDefinition, jobExecution, request, params)
        .onSuccess(promise::complete)
        .onFailure(ar -> failFileDefinitionAndJobExecution(promise, fileDefinition, jobExecution, request, ar.getCause(), params));
    }
    return promise.future();
  }

  private Future<FileDefinition> saveUUIDsFromJson(JsonObject instancesUUIDs, FileDefinition fileDefinition, JobExecution jobExecution, QuickExportRequest request, OkapiConnectionParams params) {
    List<String> ids = new ArrayList<>();
    if (instancesUUIDs != null && null != instancesUUIDs.getJsonArray("ids")) {
      JsonArray jsonIds = instancesUUIDs.getJsonArray("ids");
      if (jsonIds.size() > 0) {
        for (Object id : jsonIds) {
          ids.add(((JsonObject) id).getString("id"));
        }
        return fileStorage.saveFileDataAsyncCQL(ids, fileDefinition)
          .compose(ar -> Objects.isNull(jobExecution)
            ? updateFileDefinitionWithJobExecution(new JobExecution().withProgress(new Progress().withTotal(ids.size())), fileDefinition.withStatus(COMPLETED), params.getTenantId())
            : updateFileDefinitionAndJobExecution(jobExecution.withProgress(new Progress().withTotal(ids.size())), fileDefinition.withStatus(COMPLETED), request, params));
      }
    }

    return Objects.isNull(fileDefinition.getJobExecutionId())
      ? updateFileDefinitionWithJobExecution(new JobExecution(), fileDefinition, params.getTenantId())
      : fileDefinitionService.update(fileDefinition.withStatus(COMPLETED), params.getTenantId());
  }

  private Future<FileDefinition> updateFileDefinitionWithJobExecution(JobExecution jobExecution, FileDefinition fileDefinition, String tenantId) {
    return jobExecutionService.save(jobExecution, tenantId)
      .compose(savedJob -> fileDefinitionService.update(fileDefinition.withJobExecutionId(savedJob.getId()), tenantId));
  }

  private Future<FileDefinition> updateFileDefinitionAndJobExecution(JobExecution jobExecution, FileDefinition fileDefinition, QuickExportRequest request, OkapiConnectionParams params) {
    Promise<FileDefinition> promise = Promise.promise();
    jobExecutionService.update(jobExecution, params.getTenantId())
      .onSuccess(savedJob ->
        fileDefinitionService.update(fileDefinition, params.getTenantId())
          .onSuccess(promise::complete)
          .onFailure(ar -> failFileDefinitionAndJobExecution(promise, fileDefinition, savedJob, request, ar.getCause(), params)))
      .onFailure(ar -> failFileDefinitionAndJobExecution(promise, fileDefinition, jobExecution, request, ar.getCause(), params));

    return promise.future();
  }

  private void failFileDefinition(Promise<FileDefinition> promise, FileDefinition fileDefinition, String tenantId, Throwable cause) {
    fileDefinitionService.update(fileDefinition.withStatus(ERROR), tenantId)
      .onSuccess(fileDef -> promise.fail(cause))
      .onFailure(fileDef -> promise.fail(fileDef.getCause()));
  }

  private void failFileDefinitionAndJobExecution(Promise<FileDefinition> promise, FileDefinition fileDefinition, JobExecution jobExecution, QuickExportRequest request, Throwable cause, OkapiConnectionParams params) {
    if (!Objects.isNull(jobExecution)) {
      errorLogService.saveGeneralError("Fail to upload file for job execution with id: " + jobExecution.getId(), jobExecution.getId(), params.getTenantId());
      Optional<JsonObject> optionalUser = usersClient.getById(request.getMetadata().getCreatedByUserId(), jobExecution.getId(), params);
      if (optionalUser.isPresent()) {
        jobExecutionService.prepareAndSaveJobForFailedExport(jobExecution, fileDefinition, optionalUser.get(), 0, true, params.getTenantId());
      } else {
        errorLogService.saveGeneralError(ErrorCode.USER_NOT_FOUND.getDescription() + " with id: " + request.getMetadata().getCreatedByUserId(), jobExecution.getId(), params.getTenantId());
        jobExecutionService.update(jobExecution.withStatus(JobExecution.Status.FAIL), params.getTenantId());
      }
    }
    failFileDefinition(promise, fileDefinition, params.getTenantId(), cause);
  }

}
