package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Objects.nonNull;
import static org.apache.logging.log4j.util.Strings.EMPTY;
import static org.folio.rest.RestVerticle.STREAM_ABORT;
import static org.folio.rest.jaxrs.model.FileDefinition.Status;
import static org.folio.rest.jaxrs.model.FileDefinition.UploadFormat.CQL;
import static org.folio.util.ExceptionToResponseMapper.map;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.rest.annotations.Stream;
import org.folio.rest.annotations.Validate;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.resource.DataExportFileDefinitions;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.service.file.definition.FileDefinitionService;
import org.folio.service.file.upload.FileUploadService;
import org.folio.service.job.JobExecutionService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ErrorCode;
import org.folio.util.ExceptionToResponseMapper;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataExportImplFileDefinitionImpl implements DataExportFileDefinitions {

  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  public static final String CSV_FORMAT_EXTENSION = "csv";
  public static final String CQL_FORMAT_EXTENSION = "cql";

  private static final int MAX_FILE_SIZE = 500_000;

  private static final String QUERY_KEY = "query";

  @Autowired
  private FileDefinitionService fileDefinitionService;

  @Autowired
  private FileUploadService fileUploadService;

  @Autowired
  private JobExecutionService jobExecutionService;

  private final Map<String, String> map = new HashMap<>();

  /*
      Reference to the Future to keep uploading state in track while uploading happens.
      Since in streaming uploading the RMB does not recreate rest.impl resource,
      we can save the state here, at the resource fields.
  */
  private Future<FileDefinition> fileUploadStateFuture;
  private String tenantId;

  public DataExportImplFileDefinitionImpl(Vertx vertx, String tenantId) { //NOSONAR
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
    this.tenantId = TenantTool.calculateTenantId(tenantId);
  }

  @Override
  @Validate
  public void postDataExportFileDefinitions(FileDefinition entity, Map<String, String> okapiHeaders,
                                            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if (nonNull(entity.getSize()) && entity.getSize() > MAX_FILE_SIZE) {
      String errorMessage = String.format("File size is too large: '%d'. Please use file with size less than %d.", entity.getSize(), MAX_FILE_SIZE);
      LOGGER.error(errorMessage);
      succeededFuture().map(DataExportFileDefinitions.PostDataExportFileDefinitionsResponse
        .respond413WithApplicationJson(new Error().withMessage(errorMessage))).map(Response.class::cast).onComplete(asyncResultHandler);
      return;
    }
    JobExecution jobExecution = new JobExecution().withId(UUID.randomUUID().toString());
    entity.setJobExecutionId(jobExecution.getId());
    succeededFuture()
      .compose(ar -> jobExecutionService.save(jobExecution, tenantId))
      .compose(ar -> validateFileNameExtension(entity.getFileName()))
      .compose(ar -> replaceCQLExtensionToCSV(entity))
      .compose(ar -> fileDefinitionService.save(entity.withStatus(Status.NEW), tenantId))
      .map(DataExportFileDefinitions.PostDataExportFileDefinitionsResponse::respond201WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);
  }


  @Override
  @Validate
  public void getDataExportFileDefinitionsByFileDefinitionId(String fileDefinitionId, Map<String, String> okapiHeaders,
                                                             Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture().compose(ar -> fileDefinitionService.getById(fileDefinitionId, tenantId))
      .map(DataExportFileDefinitions.GetDataExportFileDefinitionsByFileDefinitionIdResponse::respond200WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);

  }

  @Stream
  @Override
  public void postDataExportFileDefinitionsUploadByFileDefinitionId(String fileDefinitionId, InputStream entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      Future<Response> responseFuture;
      if (okapiHeaders.containsKey(STREAM_ABORT)) {
        responseFuture = fileUploadService.errorUploading(fileDefinitionId, tenantId)
          .map(String.format("Upload stream for the file [id = '%s'] has been interrupted", fileDefinitionId))
          .map(DataExportFileDefinitions.PostDataExportFileDefinitionsUploadByFileDefinitionIdResponse::respond400WithTextPlain);
        responseFuture.map(Response.class::cast)
          .otherwise(ExceptionToResponseMapper::map)
          .onComplete(asyncResultHandler);
      } else {
        byte[] data = IOUtils.toByteArray(entity);
        if (fileUploadStateFuture == null) {
          fileUploadStateFuture = fileUploadService.startUploading(fileDefinitionId, tenantId);
        }
        responseFuture = fileUploadStateFuture.map(PostDataExportFileDefinitionsUploadByFileDefinitionIdResponse::respond200WithApplicationJson);
        responseFuture.map(Response.class::cast)
          .otherwise(ExceptionToResponseMapper::map)
          .onComplete(asyncResultHandler);
        fileUploadStateFuture = fileUploadStateFuture
          .compose(fileDefinition -> saveFileDependsOnFileExtension(fileDefinition, data, new OkapiConnectionParams(okapiHeaders)))
          .compose(fileDefinition -> data.length == 0
            ? fileUploadService.completeUploading(fileDefinition, tenantId)
            : succeededFuture(fileDefinition));
      }
    } catch (Exception e) {
      asyncResultHandler.handle(succeededFuture(map(e)));
    }
  }

  private Future<FileDefinition> saveFileDependsOnFileExtension(FileDefinition fileDefinition, byte[] data, OkapiConnectionParams params) {
    if (CQL.equals(fileDefinition.getUploadFormat())) {
      var queryChunk = new String(data);
      if (queryChunk.isEmpty()) {
        return fileUploadService.saveUUIDsByCQL(fileDefinition, map.containsKey(QUERY_KEY) ? map.remove(QUERY_KEY) : EMPTY, params);
      } else {
        map.put(QUERY_KEY, map.getOrDefault(QUERY_KEY, EMPTY).concat(queryChunk));
        return Future.succeededFuture(fileDefinition);
      }
    } else {
      return fileUploadService.saveFileChunk(fileDefinition, data, tenantId);
    }
  }

  private Future<Void> validateFileNameExtension(String fileName) {
    if (!FilenameUtils.isExtension(fileName.toLowerCase(), CSV_FORMAT_EXTENSION) && !FilenameUtils.isExtension(fileName.toLowerCase(), CQL_FORMAT_EXTENSION)) {
      throw new ServiceException(HttpStatus.HTTP_UNPROCESSABLE_ENTITY, ErrorCode.INVALID_UPLOADED_FILE_EXTENSION);
    }
    return succeededFuture();
  }

  private Future<Void> replaceCQLExtensionToCSV(FileDefinition fileDefinition) {
    String fileName = fileDefinition.getFileName();
    if (CQL.equals(fileDefinition.getUploadFormat()) && FilenameUtils.isExtension(fileName.toLowerCase(), CQL_FORMAT_EXTENSION)) {
      fileDefinition.setFileName(FilenameUtils.getBaseName(fileName) + "." + CSV_FORMAT_EXTENSION);
    }
    return succeededFuture();
  }
}
