package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.apache.commons.io.IOUtils;
import org.folio.rest.annotations.Stream;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.resource.DataExport;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.service.upload.FileUploadService;
import org.folio.service.upload.definition.FileDefinitionService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ExceptionToResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.RestVerticle.STREAM_ABORT;
import static org.folio.rest.jaxrs.model.FileDefinition.Status;
import static org.folio.util.ExceptionToResponseMapper.map;

public class DataExportImpl implements DataExport {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private FileDefinitionService fileDefinitionService;
  @Autowired
  private FileUploadService fileUploadService;
  /*
      Reference to the Future to keep uploading state in track while uploading happens.
      Since in streaming uploading the RMB does not recreate rest.impl resource,
      we can save the state here, at the resource fields.
  */
  private Future<FileDefinition> fileUploadStateFuture;
  private String tenantId;

  public DataExportImpl(Vertx vertx, String tenantId) { //NOSONAR
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
    this.tenantId = TenantTool.calculateTenantId(tenantId);
  }

  @Override
  public void postDataExportExport(ExportRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
      try {
        LOGGER.info("Starting the data-export process, request: {}", entity);
        // call import manager to start
        succeededFuture()
          .map(PostDataExportExportResponse.respond204())
          .map(Response.class::cast)
          .setHandler(asyncResultHandler);
      } catch (Exception exception) {
        asyncResultHandler.handle(succeededFuture(map(exception)));
      }
  }

  @Override
  public void postDataExportFileDefinitions(FileDefinition entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture()
      .compose(ar ->  fileDefinitionService.save(entity.withStatus(Status.NEW), tenantId))
      .map(PostDataExportFileDefinitionsResponse::respond201WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .setHandler(asyncResultHandler);
  }

  @Override
  public void getDataExportFileDefinitionsByFileDefinitionId(String fileDefinitionId, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture()
      .compose(ar -> fileDefinitionService.getById(fileDefinitionId, tenantId))
      .map(optionalDefinition -> optionalDefinition.orElseThrow(() ->
        new NotFoundException(String.format("File definition with id [%s] is not found", fileDefinitionId))))
      .map(GetDataExportFileDefinitionsByFileDefinitionIdResponse::respond200WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .setHandler(asyncResultHandler);
  }

  @Stream
  @Override
  public void postDataExportFileDefinitionsUploadByFileDefinitionId(String fileDefinitionId, InputStream entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      Future<Response> responseFuture;
      if (okapiHeaders.containsKey(STREAM_ABORT)) {
        responseFuture = fileUploadService.errorUploading(fileDefinitionId, tenantId)
          .map(String.format("Upload stream for the file [id = '%s'] has been interrupted", fileDefinitionId))
          .map(PostDataExportFileDefinitionsUploadByFileDefinitionIdResponse::respond400WithTextPlain);
      } else {
        byte[] data = IOUtils.toByteArray(entity);
        if (fileUploadStateFuture == null) {
          fileUploadStateFuture = fileUploadService.startUploading(fileDefinitionId, tenantId);
        }
        fileUploadStateFuture = fileUploadStateFuture
          .compose(fileDefinition -> fileUploadService.saveFileChunk(fileDefinition, data, tenantId))
          .compose(fileDefinition -> data.length == 0
            ? fileUploadService.completeUploading(fileDefinition, tenantId)
            : succeededFuture(fileDefinition));
        responseFuture = fileUploadStateFuture.map(PostDataExportFileDefinitionsUploadByFileDefinitionIdResponse::respond200WithApplicationJson);
      }
      responseFuture.map(Response.class::cast)
        .otherwise(ExceptionToResponseMapper::map)
        .setHandler(asyncResultHandler);
    } catch (Exception e) {
      asyncResultHandler.handle(succeededFuture(map(e)));
    }
  }
}
