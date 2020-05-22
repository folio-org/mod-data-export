package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.util.ExceptionToResponseMapper.map;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.resource.DataExport;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.service.job.JobExecutionService;
import org.folio.service.manager.input.InputDataManager;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ExceptionToResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DataExportImpl implements DataExport {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  @Autowired
  private JobExecutionService jobExecutionService;

  @Autowired
  private DataExportHelper dataExportHelper;

  private InputDataManager inputDataManager;

  private String tenantId;

  public DataExportImpl(Vertx vertx, String tenantId) {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
    this.tenantId = TenantTool.calculateTenantId(tenantId);
    this.inputDataManager = vertx.getOrCreateContext().get(InputDataManager.class.getName());
  }

  @Override
  @Validate
  public void postDataExportExport(ExportRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      LOGGER.info("Starting the data-export process, request: {}", entity);
      inputDataManager.init(JsonObject.mapFrom(entity), okapiHeaders);
      succeededFuture()
        .map(PostDataExportExportResponse.respond204())
        .map(Response.class::cast)
        .onComplete(asyncResultHandler);
    } catch (Exception exception) {
      asyncResultHandler.handle(succeededFuture(map(exception)));
    }
  }

  @Override
  @Validate
  public void getDataExportJobExecutions(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext(v -> jobExecutionService.get(query, offset, limit, tenantId)
      .map(GetDataExportJobExecutionsResponse::respond200WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler));
  }

  @Override
  @Validate
  public void getDataExportJobExecutionsDownloadByJobExecutionIdAndExportFileId(String jobId, String exportFileId,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture().compose(ar -> dataExportHelper.getDownloadLink(jobId, exportFileId, tenantId))
      .map(GetDataExportJobExecutionsDownloadByJobExecutionIdAndExportFileIdResponse::respond200WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);
  }

}
