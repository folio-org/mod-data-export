package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.jaxrs.resource.DataExportJobProfiles;

public class DataExportImplJobProfileImpl implements DataExportJobProfiles {

  @Override
  public void postDataExportJobProfiles(String lang, JobProfile entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    // TODO Auto-generated method stub

  }

  @Override
  public void getDataExportJobProfilesById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    // TODO Auto-generated method stub

  }

  @Override
  public void deleteDataExportJobProfilesById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    // TODO Auto-generated method stub

  }

  @Override
  public void putDataExportJobProfilesById(String id, String lang, JobProfile entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    // TODO Auto-generated method stub

  }

  @Override
  public void getDataExportJobProfiles(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    // TODO Auto-generated method stub

  }}
