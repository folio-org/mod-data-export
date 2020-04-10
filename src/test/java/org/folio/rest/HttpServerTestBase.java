package org.folio.rest;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.util.OkapiConnectionParams;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.HashMap;
import java.util.Map;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

/**
 * Class for tests that base on testing code using HTTP mock server
 */
public abstract class HttpServerTestBase {

  protected static final String TENANT_ID = "diku";
  protected static final String TOKEN = "token";
  private static final String HOST = "http://localhost:";
  protected static Vertx vertx;
  protected static OkapiConnectionParams okapiConnectionParams;
  protected static HttpServer httpServer;
  protected static int port;

  @BeforeClass
  public static void setUpHttpServer() {
    vertx = Vertx.vertx();
    port = NetworkUtils.nextFreePort();
    httpServer = vertx.createHttpServer();

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
