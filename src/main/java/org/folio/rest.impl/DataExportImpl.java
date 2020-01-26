package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.resource.DataExport;
import org.folio.service.manager.ExportManager;
import javax.ws.rs.core.Response;
import java.util.Map;

public class DataExportImpl implements DataExport {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataExportImpl.class);

  private ExportManager exportManager;

  public DataExportImpl(Vertx vertx, String tenantId) {
    this.exportManager = ExportManager.createProxy(vertx);
  }

  @Override
  public void postDataExportExport(ExportRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LOGGER.info("Starting the data-export process for the request: {}", entity);
    exportManager.startExport(JsonObject.mapFrom(entity), JsonObject.mapFrom(okapiHeaders));
    Future.succeededFuture()
      .map(PostDataExportExportResponse.respond204())
      .map(Response.class::cast)
      .setHandler(asyncResultHandler);
  }
}
