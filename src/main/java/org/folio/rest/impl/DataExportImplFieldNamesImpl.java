package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.folio.rest.jaxrs.resource.DataExportFieldNames;
import org.folio.service.fieldname.FieldNamesService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ExceptionToResponseMapper;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;

public class DataExportImplFieldNamesImpl implements DataExportFieldNames {

  @Autowired
  FieldNamesService fieldNamesService;

  public DataExportImplFieldNamesImpl(Vertx vertx, String tenantId) { //NOSONAR
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getDataExportFieldNames(String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    succeededFuture()
      .compose(ar -> fieldNamesService.getFieldNames(new OkapiConnectionParams(okapiHeaders)))
      .map(GetDataExportFieldNamesResponse::respond200WithApplicationJson)
      .map(Response.class::cast)
      .otherwise(ExceptionToResponseMapper::map)
      .onComplete(asyncResultHandler);
  }

}
