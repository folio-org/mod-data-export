package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.folio.rest.jaxrs.resource.DataExportLogs;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.service.logs.ErrorLogService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ExceptionToResponseMapper;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.ws.rs.core.Response;
import java.util.Map;

public class DataExportImplLogsImpl implements DataExportLogs {

  private final String tenantId;

  @Autowired
  private ErrorLogService errorLogService;

  public DataExportImplLogsImpl(Vertx vertx, String tenantId) { //NOSONAR
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
    this.tenantId = TenantTool.calculateTenantId(tenantId);
  }

  @Override
  public void getDataExportLogs(@Min(0) @Max(2147483647) int offset, @Min(0) @Max(2147483647) int limit, String query, @Pattern(regexp = "[a-zA-Z]{2}") String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    Future.succeededFuture()
      .compose(ar -> errorLogService.getByJobExecutionId(query, offset, limit, tenantId))
      .map(DataExportImplLogsImpl.GetDataExportLogsResponse::respond200WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);
  }

}
