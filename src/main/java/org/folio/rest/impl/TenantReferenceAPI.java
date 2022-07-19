package org.folio.rest.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

public class TenantReferenceAPI extends TenantAPI {

  private static final Logger log = LogManager.getLogger(TenantReferenceAPI.class);
  private static final String DEFAULT_DATA_PATH = "templates/db_scripts/default_data/";
  private static final String DEFAULT_AUTHORITY_JOB_PROFILE = DEFAULT_DATA_PATH + "default_authority_export_job_profile.sql";
  private static final String DEFAULT_HOLDINGS_JOB_PROFILE = DEFAULT_DATA_PATH + "default_holdings_export_job_profile.sql";
  private static final String DEFAULT_INSTANCE_JOB_PROFILE = DEFAULT_DATA_PATH + "default_instance_export_job_profile.sql";

  private static final String TENANT_PLACEHOLDER = "${myuniversity}";
  private static final String MODULE_PLACEHOLDER = "${mymodule}";

  @Override
  Future<Integer> loadData(TenantAttributes attributes, String tenantId, Map<String, String> headers, Context context) {
    return super.loadData(attributes, tenantId, headers, context)
      .compose(num -> setupDefaultData(DEFAULT_INSTANCE_JOB_PROFILE, headers, context)
        .compose(d -> setupDefaultData(DEFAULT_HOLDINGS_JOB_PROFILE, headers, context))
        .compose(a -> setupDefaultData(DEFAULT_AUTHORITY_JOB_PROFILE, headers, context))
        .map(num));
  }

  private Future<List<String>> setupDefaultData(String script, Map<String, String> headers, Context context) {
    try {
      InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(script);

      if (inputStream == null) {
        log.info("Default data was not initialized: no resources found: {}", script);
        return Future.succeededFuture();
      }

      String sqlScript = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
      if (StringUtils.isBlank(sqlScript)) {
        return Future.succeededFuture();
      }

      String tenantId = TenantTool.calculateTenantId(headers.get("x-okapi-tenant"));
      String moduleName = PostgresClient.getModuleName();

      sqlScript = sqlScript.replace(TENANT_PLACEHOLDER, tenantId).replace(MODULE_PLACEHOLDER, moduleName);

      Promise<List<String>> promise = Promise.promise();
      PostgresClient.getInstance(context.owner()).runSQLFile(sqlScript, false, promise);

      return promise.future();
    } catch (IOException e) {
      log.error("Failed to initialize default data", e);
      return Future.failedFuture(e);
    }
  }

  @Override
  public void deleteTenantByOperationId(String operationId, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
                                        Context ctx) {
    log.info("deleteTenant");
    super.deleteTenantByOperationId(operationId, headers, res -> {
      Vertx vertx = ctx.owner();
      String tenantId = TenantTool.tenantId(headers);
      PostgresClient.getInstance(vertx, tenantId)
        .closeClient(event -> handler.handle(res));
    }, ctx);
  }
}
