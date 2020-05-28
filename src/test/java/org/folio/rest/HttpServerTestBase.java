package org.folio.rest;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.util.OkapiConnectionParams;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.util.ExternalPathResolver.CONTENT_TERMS;
import static org.folio.util.ExternalPathResolver.HOLDING;
import static org.folio.util.ExternalPathResolver.IDENTIFIER_TYPES;
import static org.folio.util.ExternalPathResolver.INSTANCE;
import static org.folio.util.ExternalPathResolver.ITEM;
import static org.folio.util.ExternalPathResolver.SRS;
import static org.folio.util.ExternalPathResolver.USERS;
import static org.folio.util.ExternalPathResolver.resourcesPath;

/**
 * Class for tests that base on testing code using HTTP mock server
 */
public abstract class HttpServerTestBase {

  protected static final String TENANT_ID = "diku";
  protected static final String TOKEN = "token";
  protected static Vertx vertx;
  protected static Router router;
  protected static OkapiConnectionParams okapiConnectionParams;
  private static final String HOST = "http://localhost:";
  private static HttpServer httpServer;
  private static final String USERS_RECORDS_MOCK_DATA_PATH = "mockData/user/get_user_response.json";

  @BeforeAll
  public static void setUpHttpServer() throws InterruptedException, ExecutionException, TimeoutException {
    vertx = Vertx.vertx();
    int port = NetworkUtils.nextFreePort();
    router = defineRoutes();
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
    Router router = Router.router(vertx);

    router.route()
      .handler(BodyHandler.create());

    router.get(resourcesPath(USERS) + "/:id").handler(ctx -> handleGetUsersRecord(ctx));


    return router;
  }

  private static void handleGetUsersRecord(RoutingContext ctx) {
    try {
      JsonObject user = new JsonObject(RestVerticleTestBase.getMockData(USERS_RECORDS_MOCK_DATA_PATH));
      serverResponse(ctx, 200, APPLICATION_JSON, user.encodePrettily());
    } catch (IOException e) {
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }

  private static void serverResponse(RoutingContext ctx, int statusCode, String contentType, String body) {
    ctx.response()
      .setStatusCode(statusCode)
      .putHeader(HttpHeaders.CONTENT_TYPE, contentType)
      .end(body);
  }

  @AfterAll
  public static void tearDownClass() {
    httpServer.close();
  }
}
