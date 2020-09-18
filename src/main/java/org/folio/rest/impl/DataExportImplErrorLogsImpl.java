package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.folio.rest.jaxrs.resource.DataExportLogs;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.service.logs.ErrorLogService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ExceptionToResponseMapper;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;

public class DataExportImplErrorLogsImpl implements  DataExportLogs {

  private final String tenantId;

  @Autowired
  private ErrorLogService errorLogService;

  public DataExportImplErrorLogsImpl(Vertx vertx, String tenantId) {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
    this.tenantId = TenantTool.calculateTenantId(tenantId);
  }

  @Override
  public void getDataExportLogsByJobExecutionID(String jobExecutionID, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture()
      .compose(ar -> errorLogService.getByJobExecutionId(jobExecutionID, offset, limit, tenantId))
      .map(DataExportImplErrorLogsImpl.GetDataExportLogsByJobExecutionIDResponse::respond200WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);
  }

}
