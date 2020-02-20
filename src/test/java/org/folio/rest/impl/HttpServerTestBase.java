package org.folio.rest.impl;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;

import java.util.HashMap;
import java.util.Map;

import org.folio.clients.OkapiClientsFactory;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.util.OkapiConnectionParams;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.Router;

public abstract class HttpServerTestBase {

  protected static final String TENANT_ID = "diku";
  protected static final String TOKEN = "token";
  private static final String HOST = "http://localhost:";
  private static final int PORT = NetworkUtils.nextFreePort();
  private static final String OKAPI_URL = HOST + PORT;
  protected static RequestSpecification requestSpecification;
  protected static Vertx vertx = Vertx.vertx();
  protected static Router router;
  private static HttpServer httpServer;
  protected OkapiClientsFactory clients = OkapiClientsFactory.create(getOkapiConnectionParams());

  @BeforeClass
  public static void setUpClass(final TestContext context) throws Exception {
    router = Router.router(vertx);
    httpServer = vertx.createHttpServer();
    httpServer.requestHandler(router).listen(PORT);

  }

  @Before
  public void setUp() {
    requestSpecification = new RequestSpecBuilder()
      .setContentType(ContentType.JSON)
      .addHeader(OKAPI_HEADER_TENANT, TENANT_ID)
      .setBaseUri(OKAPI_URL)
      .addHeader("Accept", "text/plain, application/json")
      .build();
  }

  @NotNull
  protected OkapiConnectionParams getOkapiConnectionParams() {
    Map<String, String> okapiHeaders = new HashMap<>();
    okapiHeaders.put("x-okapi-url", HOST + httpServer.actualPort());
    return new OkapiConnectionParams(okapiHeaders);
  }

  @AfterClass
  public static void tearDownClass() {
    httpServer.close();
  }
}
