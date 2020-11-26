package org.folio.service.file.definition;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.dao.FileDefinitionDao;
import org.folio.rest.jaxrs.model.QuickExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.service.job.JobExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.lang.invoke.MethodHandles;
import java.util.UUID;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.jaxrs.model.FileDefinition.Status.NEW;

@Service
public class FileDefinitionServiceImpl implements FileDefinitionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String CSV_FILE_FORMAT = ".csv";

  @Autowired
  private FileDefinitionDao fileDefinitionDao;
  @Autowired
  private JobExecutionService jobExecutionService;

  @Override
  public Future<FileDefinition> getById(String id, String tenantId) {
    return fileDefinitionDao.getById(id, tenantId)
      .compose(optionalFileDefinition -> {
        if (optionalFileDefinition.isPresent()) {
          return succeededFuture(optionalFileDefinition.get());
        } else {
          String errorMessage = String.format("File definition not found with id %s", id);
          LOGGER.error(errorMessage);
          return failedFuture(new NotFoundException(errorMessage));
        }
      });
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
  public Future<FileDefinition> prepareFileDefinitionForQuickExport(QuickExportRequest.Type type, String jobProfileId, String tenantId) {
    Promise<FileDefinition> promise = Promise.promise();
    jobExecutionService.save(new JobExecution(), tenantId)
      .onSuccess(jobExecution -> {
        FileDefinition fileDefinition = new FileDefinition()
          .withUploadFormat(getUploadFormatByType(type))
          .withJobExecutionId(jobExecution.getId())
          .withFileName(jobExecution.getId() + CSV_FILE_FORMAT)
          .withStatus(NEW);
        save(fileDefinition, tenantId)
          .onSuccess(promise::complete)
          .onFailure(ar -> promise.fail(ar.getCause()));
      })
      .onFailure(ar -> promise.fail(ar.getCause()));

    return promise.future();
  }

  private FileDefinition.UploadFormat getUploadFormatByType(QuickExportRequest.Type type) {
    return QuickExportRequest.Type.CQL.equals(type)
      ? FileDefinition.UploadFormat.CQL
      : FileDefinition.UploadFormat.CSV;
  }

}
