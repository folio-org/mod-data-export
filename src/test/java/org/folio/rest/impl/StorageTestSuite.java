package org.folio.rest.impl;

import io.restassured.http.Header;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.PomReader;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.jupiter.api.*;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.folio.rest.impl.RestVerticleTestBase.BASE_OKAPI_URL;
import static org.folio.rest.impl.RestVerticleTestBase.TENANT_ID;
import static org.folio.rest.impl.RestVerticleTestBase.TOKEN;
import static org.folio.rest.tools.client.Response.isSuccess;

@RunWith(JUnitPlatform.class)
public class StorageTestSuite {
  private static final Logger logger = LogManager.getLogger(StorageTestSuite.class);

  private static Vertx vertx;
  private static MockServer mockServer;

  public static int port = NetworkUtils.nextFreePort();
  public static final Header URL_TO_HEADER = new Header("X-Okapi-Url-to","http://localhost:"+port);
  public static final int mockPort = NetworkUtils.nextFreePort();

  private StorageTestSuite() {}

  public static URL storageUrl(String path) throws MalformedURLException {
    return new URL("http", "localhost", port, path);
  }

  public static Vertx getVertx() {
    return vertx;
  }

  @BeforeAll
  public static void before() throws IOException, InterruptedException, ExecutionException, TimeoutException {

    // tests expect English error messages only, no Danish/German/...
    Locale.setDefault(Locale.US);

    vertx = Vertx.vertx();

    mockServer = new MockServer(mockPort);
    mockServer.start();

    logger.info("Start embedded database");
    PostgresClient.setIsEmbedded(true);
    PostgresClient.getInstance(vertx).startEmbeddedPostgres();


    deployVerticle();
  }

  @AfterAll
  public static void after() throws InterruptedException, ExecutionException, TimeoutException, MalformedURLException {
    CompletableFuture<String> undeploymentComplete = new CompletableFuture<>();

    vertx.close(res -> {
      if(res.succeeded()) {
        undeploymentComplete.complete(null);
      }
      else {
        undeploymentComplete.completeExceptionally(res.cause());
      }
    });

    undeploymentComplete.get(20, TimeUnit.SECONDS);
    logger.info("Stop database");
    PostgresClient.stopEmbeddedPostgres();
    mockServer.close();
  }

  private static void deployVerticle() throws InterruptedException, ExecutionException, TimeoutException {
    TenantClient tenantClient = new TenantClient(BASE_OKAPI_URL, TENANT_ID, TOKEN);
    logger.info("Starting verticle on port: "+ port);
    DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
    CompletableFuture<String> deploymentComplete = new CompletableFuture<>();
    vertx.deployVerticle(RestVerticle.class.getName(), options, res -> {
      if (res.succeeded()) {
        TenantAttributes tenantAttributes = new TenantAttributes();
        tenantAttributes.setModuleTo(PomReader.INSTANCE.getModuleName());
        try {
          tenantClient.postTenant(tenantAttributes, res2 -> {
            if (isSuccess(res2.result().statusCode())){
              deploymentComplete.complete(res.result());
            } else {
              deploymentComplete.completeExceptionally(new Exception(res2.result().statusMessage()));
            }
          });
        } catch (Exception e) {
          deploymentComplete.completeExceptionally(e);
        }

      } else {
        deploymentComplete.completeExceptionally(res.cause());
      }
    });
    deploymentComplete.get(60, TimeUnit.SECONDS);


  }

  @Nested
  class EntitiesCrudTestNested extends EntitiesCrudTest {
  }

  @Nested
  class ExportManagerTestNested extends ExportManagerTest {
  }

  @Nested
  class FileUploadServiceTestNested extends FileUploadServiceTest {
  }

  @Nested
  class JobExecutionServiceTestNested extends JobExecutionServiceTest {
  }

  @Nested
  class StorageCleanupServiceImplTestNested extends StorageCleanupServiceImplTest {
  }

  @Nested
  class ConfigurationsClientTestNested extends ConfigurationsClientTest {
  }

  @Nested
  class InventoryClientTestNested extends InventoryClientTest {
  }

  @Nested
  class SourceRecordStorageTestNested extends SourceRecordStorageTest {
  }

  @Nested
  class UsersClientTestNested extends UsersClientTest {
  }

  @Nested
  class DataExportTestNested extends DataExportTest {
  }

  @Nested
  class TransformationFieldsServiceTestNested extends TransformationFieldsServiceTest {
  }

  @Nested
  class ErrorLogsTestNested extends ErrorLogsTest {
  }

  @Nested
  class DataExportCleanUpFilesTestNested extends DataExportCleanUpFilesTest {
  }

}
