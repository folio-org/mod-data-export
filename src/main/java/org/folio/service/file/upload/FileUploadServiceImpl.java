package org.folio.service.file.upload;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.HttpStatus;
import org.folio.clients.InventoryClient;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.service.file.definition.FileDefinitionService;
import org.folio.service.file.storage.FileStorage;
import org.folio.service.job.JobExecutionService;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.z3950.zing.cql.CQLParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    return fileStorage.saveFileDataAsync(data, fileDefinition);
  }

  @Override
  public Future<FileDefinition> saveUUIDsByCQL(FileDefinition fileDefinition, String query, OkapiConnectionParams params) {
    Promise<FileDefinition> promise = Promise.promise();
    Optional<JsonObject> instancesUUIDs = inventoryClient.getInstancesBulkUUIDs(query, params);
    List<String> list = new ArrayList<>();
    if (instancesUUIDs.isPresent()) {
      JsonArray array = instancesUUIDs.get().getJsonArray("ids");
      // possibly a good place to use parralel stream. Also, worth thinking to add some additional field to file definition, for example 'format'
      // with possible values: csv || cql. Then we can create a job execution with progress here, set total to the array size, and while export,
      // avoid count of lines in file for calculate total field if format is cql.
      array.stream().forEach(var -> {
        String a = ((JsonObject) var).getString("id");
        list.add(a);
      });
    }
    fileStorage.saveFileDataAsyncCQL(list, fileDefinition);
    promise.complete(fileDefinition);
    return  promise.future();
  }

  @Override
  public Future<FileDefinition> completeUploading(FileDefinition fileDefinition, String tenantId) {
    JobExecution jobExecution = new JobExecution();
    return jobExecutionService.save(jobExecution, tenantId).compose(savedJob ->
      fileDefinitionService.update(fileDefinition.withStatus(COMPLETED).withJobExecutionId(savedJob.getId()), tenantId));
  }

  @Override
  public Future<FileDefinition> errorUploading(String fileDefinitionId, String tenantId) {
    return fileDefinitionService.getById(fileDefinitionId, tenantId)
      .compose(fileDefinition -> fileDefinitionService.update(fileDefinition.withStatus(ERROR), tenantId));
  }


}
