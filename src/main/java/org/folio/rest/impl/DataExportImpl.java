package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.resource.DataExport;
import org.folio.service.manager.ExportManager;
import org.folio.util.ExceptionToResponseMapper;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;

public class DataExportImpl implements DataExport {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataExportImpl.class);

  private ExportManager exportManager;

  public DataExportImpl(Vertx vertx, String tenantId) {     //NOSONAR
    this.exportManager = ExportManager.createProxy(vertx);
  }

  @Override
  public void postDataExportExport(ExportRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    vertxContext.runOnContext(c -> {
      try {
        LOGGER.info("Starting the data-export process, request: {}", entity);
        exportManager.startExport(JsonObject.mapFrom(entity), JsonObject.mapFrom(okapiHeaders));
        succeededFuture()
          .map(PostDataExportExportResponse.respond204())
          .map(Response.class::cast)
          .otherwise(ExceptionToResponseMapper::map)
          .setHandler(asyncResultHandler);
      } catch (Exception exception) {
        asyncResultHandler.handle(succeededFuture(
          ExceptionToResponseMapper.map(exception)));
      }
    });
  }
}
