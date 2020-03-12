package org.folio.rest;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.PomReader;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.util.OkapiConnectionParams;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.Map;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;

/**
 * Class for tests that base on testing code using Vertx REST verticle
 */
public abstract class RestVerticleTestBase {

  protected static final String TENANT_ID = "diku";
  protected static final String TOKEN = "token";
  private static final String HOST = "http://localhost:";
  protected static final int PORT = NetworkUtils.nextFreePort();
  private static final String OKAPI_HEADER_URL = "x-okapi-url";
  protected static final String OKAPI_URL = HOST + PORT;
  protected static RequestSpecification jsonRequestSpecification;
  protected static Vertx vertx;
  protected OkapiConnectionParams okapiConnectionParams;

  @BeforeClass
  public static void setUpClass(final TestContext context) throws Exception {
    vertx = Vertx.vertx();
    runDatabase();
    deployVerticle(context);
  }

  private static void runDatabase() throws Exception {
    PostgresClient.setIsEmbedded(true);
    PostgresClient.getInstance(vertx).startEmbeddedPostgres();
  }

  private static void deployVerticle(final TestContext context) {
    Async async = context.async();
    TenantClient tenantClient = new TenantClient(OKAPI_URL, TENANT_ID, TOKEN);
    DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", PORT));
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
    setUpOkapiConnectionParams();
    setUpJsonRequestSpecification();
  }

  private void setUpOkapiConnectionParams() {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    headers.put(OKAPI_HEADER_URL, OKAPI_URL);
    this.okapiConnectionParams = new OkapiConnectionParams(headers);
  }

  private void setUpJsonRequestSpecification() {
    jsonRequestSpecification = new RequestSpecBuilder()
      .setContentType(ContentType.JSON)
      .addHeader(OKAPI_HEADER_TENANT, TENANT_ID)
      .setBaseUri(OKAPI_URL)
      .addHeader("Accept", "text/plain, application/json")
      .build();
  }
}
