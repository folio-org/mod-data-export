package org.folio.rest;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.commons.io.IOUtils;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.util.OkapiConnectionParams;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import java.io.FileReader;
import java.io.IOException;
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
@RunWith(VertxUnitRunner.class)
public abstract class HttpServerTestBase {

  protected static final String TENANT_ID = "diku";
  protected static final String TOKEN = "token";
  protected static OkapiConnectionParams okapiConnectionParams;
  private static final String HOST = "http://localhost:";
  private static Vertx vertx;
  private static HttpServer httpServer;

  @BeforeClass
  public static void setUpHttpServer() throws InterruptedException, ExecutionException, TimeoutException {
    vertx = Vertx.vertx();
    int port = NetworkUtils.nextFreePort();
    Router router = defineRoutes();
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

  private static Router defineRoutes() {
    final Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.get("/source-storage/records").handler(HttpServerTestBase::getRecordsByIds);
    router.get("/users/:id").handler(HttpServerTestBase::getUserById);
    router.get("/instance-storage/instances").handler(HttpServerTestBase::getInstancesByIds);
    return router;
  }

  private static void getRecordsByIds(RoutingContext rc) {
    try {
      String json = IOUtils.toString(new FileReader("src/test/resources/srsResponse.json"));
      JsonObject data = new JsonObject(json);
      rc.response().setStatusCode(200).putHeader("content-type", "application/json").end(data.toBuffer());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void getInstancesByIds(RoutingContext rc) {
    try {
      String instancesJson = IOUtils.toString(new FileReader("src/test/resources/inventoryStorageResponse.json"));
      JsonObject data = new JsonObject(instancesJson);
      rc.response().setStatusCode(200).putHeader("content-type", "application/json").end(data.toBuffer());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void getUserById(RoutingContext rc) {
    try {
      String json = IOUtils.toString(new FileReader("src/test/resources/clients/userResponse.json"));
      JsonObject data = new JsonObject(json);
      rc.response().setStatusCode(200).putHeader("content-type", "application/json").end(data.toBuffer());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownClass() {
    httpServer.close();
  }

}
