package org.folio.rest;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.util.OkapiConnectionParams;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

/**
 * Class for tests that base on testing code using HTTP mock server
 */
public abstract class HttpServerTestBase {

  protected static final String TENANT_ID = "diku";
  protected static final String TOKEN = "token";
  protected static final String USERS_BY_ID_URL = "/users/:id";
  protected static final String RECORDS_BY_ID_URL = "/source-storage/records";
  protected static final String INSTANCE_BY_ID_URL = "/instance-storage/instances";
  protected static final String INVENTORY_RESPONSE_JSON = "clients/inventoryStorageResponse.json";
  protected static final String INVENTORY_EMPTY_RESPONSE_JSON = "clients/InventoryStorageEmptyResponse.json";
  protected static final String USER_RESPONSE_JSON = "clients/userResponse.json";
  protected static final String SRS_RESPONSE_JSON = "clients/srsResponse.json";
  protected static Vertx vertx;
  protected static Router router;
  protected static OkapiConnectionParams okapiConnectionParams;
  private static final String HOST = "http://localhost:";
  private static HttpServer httpServer;

  @BeforeClass
  public static void setUpHttpServer() throws InterruptedException, ExecutionException, TimeoutException {
    vertx = Vertx.vertx();
    int port = NetworkUtils.nextFreePort();
    router = Router.router(vertx);
    httpServer = vertx.createHttpServer();
    CompletableFuture<HttpServer> waitForDeployment = new CompletableFuture<>();
    httpServer.requestHandler(router).listen(port, result -> {
      if(result.succeeded()) {
        waitForDeployment.complete(result.result());
      }
      else {
        waitForDeployment.completeExceptionally(result.cause());
      }
    });
    waitForDeployment.get(60, TimeUnit.SECONDS);

    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put("x-okapi-url", HOST + port);
    okapiHeaders.put(OKAPI_HEADER_TENANT, TENANT_ID);
    okapiHeaders.put(OKAPI_HEADER_TOKEN, TOKEN);
    okapiConnectionParams = new OkapiConnectionParams(okapiHeaders);
  }

  @AfterClass
  public static void tearDownClass() {
    httpServer.close();
  }
}
