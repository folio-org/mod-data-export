package org.folio.rest.impl;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.PomReader;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;

import java.io.IOException;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;

public abstract class AbstractRestTest {

  protected static final String OKAPI_TENANT_HEADER = "x-okapi-tenant";
  protected static final String OKAPI_TOKEN_HEADER = "x-okapi-token";
  protected static final String OKAPI_URL_HEADER = "x-okapi-url";

  protected static final String TENANT_ID = "diku";
  protected static final String TOKEN = "token";
  private static final String HTTP_PORT = "http.port";
  private static final String HOST = "http://localhost:";

  protected static RequestSpecification requestSpecification;
  private static int port;
  private static Vertx vertx;

  @Rule
  public WireMockRule mockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new Slf4jNotifier(true)));

  protected final static JsonObject OKAPI_CONNECTION_PARAMS = new JsonObject()
    .put(OKAPI_URL_HEADER, HOST + port)
    .put(OKAPI_TENANT_HEADER, TENANT_ID)
    .put(OKAPI_TOKEN_HEADER, TOKEN);

  @BeforeClass
  public static void setUpClass(final TestContext context) throws Exception {
    vertx = Vertx.vertx();
    runDatabase();
    deployVerticle(context);
  }

  private static void runDatabase() throws Exception {
    PostgresClient.stopEmbeddedPostgres();
    PostgresClient.closeAllClients();
    PostgresClient.setIsEmbedded(true);
    PostgresClient.getInstance(vertx).startEmbeddedPostgres();
  }

  private static void deployVerticle(final TestContext context) {
    Async async = context.async();
    port = NetworkUtils.nextFreePort();
    String okapiUrl = HOST + port;

    TenantClient tenantClient = new TenantClient(okapiUrl, TENANT_ID, TOKEN);
    DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put(HTTP_PORT, port));
    vertx.deployVerticle(RestVerticle.class.getName(), options, res -> {
      try {
        TenantAttributes tenantAttributes = new TenantAttributes();
        tenantAttributes.setModuleTo(PomReader.INSTANCE.getModuleName());
        tenantClient.postTenant(tenantAttributes, res2 -> {
          async.complete();
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  @AfterClass
  public static void tearDownClass(final TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      PostgresClient.stopEmbeddedPostgres();
      async.complete();
    }));
  }

  @Before
  public void setUp(TestContext context) throws IOException {
    this.requestSpecification = new RequestSpecBuilder()
      .setContentType(ContentType.JSON)
      .addHeader(OKAPI_HEADER_TENANT, TENANT_ID)
      .setBaseUri(HOST + port)
      .addHeader("Accept", "text/plain, application/json")
      .build();
  }
}
