package org.folio.rest.impl;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.util.OkapiConnectionParams;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.HashMap;
import java.util.Map;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

public abstract class HttpServerTestBase {

  protected static final String TENANT_ID = "diku";
  protected static final String TOKEN = "token";
  private static final String HOST = "http://localhost:";
  private static final int PORT = NetworkUtils.nextFreePort();
  protected static Vertx vertx;
  protected static Router router;
  private static HttpServer httpServer;
  protected static OkapiConnectionParams okapiConnectionParams;

  @BeforeClass
  public static void setUpHttpServer() {
    vertx = Vertx.vertx();
    router = Router.router(vertx);
    httpServer = vertx.createHttpServer();
    httpServer.requestHandler(router).listen(PORT);

    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put("x-okapi-url", HOST + httpServer.actualPort());
    okapiHeaders.put(OKAPI_HEADER_TENANT, TENANT_ID);
    okapiHeaders.put(OKAPI_HEADER_TOKEN, TOKEN);
    okapiConnectionParams = new OkapiConnectionParams(okapiHeaders);
  }

  @AfterClass
  public static void tearDownClass() {
    httpServer.close();
  }
}
