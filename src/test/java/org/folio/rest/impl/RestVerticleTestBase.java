package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.rest.impl.StorageTestSuite.URL_TO_HEADER;
import static org.folio.rest.impl.StorageTestSuite.mockPort;
import static org.folio.rest.impl.StorageTestSuite.port;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.folio.rest.tools.PomReader;
import org.folio.util.OkapiConnectionParams;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;


/**
 * Class for tests that base on testing code using Vertx REST verticle
 */
public abstract class RestVerticleTestBase {
  static {
    //TODO
    System.setProperty(LogManager.LOGGER_DELEGATE_FACTORY_CLASS_NAME, "io.vertx.core.logging.Log4j2LogDelegateFactory");
  }

  private static final String HOST = "http://localhost:";
  protected static final String OKAPI_HEADER_URL = "x-okapi-url";

  protected static RequestSpecification jsonRequestSpecification;
  protected static Vertx vertx;
  protected OkapiConnectionParams okapiConnectionParams;
  protected static final String MOCK_OKAPI_URL = HOST + mockPort;
  protected static final String EXPORT_URL = "/data-export/export";
  protected static final String QUICK_EXPORT_URL = "/data-export/quick-export";
  protected static final String FILE_DEFINITION_SERVICE_URL = "/data-export/file-definitions/";
  protected static final String ERROR_LOGS_SERVICE_URL = "/data-export/logs";
  protected static final String JOB_EXECUTIONS_URL = "/data-export/job-executions";
  protected static final String EXPIRE_JOBS_URL = "/data-export/expire-jobs";
  protected static final String CLEAN_UP_FILES_URL = "/data-export/clean-up-files";
  protected static final String FIELD_NAMES_URL = "/data-export/transformation-fields";
  protected static final String MAPPING_PROFILE_URL = "/data-export/mapping-profiles";
  protected static final String UPLOAD_URL = "/upload";
  protected static final String STORAGE_DIRECTORY_PATH = "./storage";
  protected static final String FILES_FOR_UPLOAD_DIRECTORY = "endToEndTestFiles/";
  protected static final String DEFAULT_JOB_PROFILE_ID = "6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a";
  private static boolean invokeStorageTestSuiteAfter = false;

  public static final String BASE_OKAPI_URL = HOST + port;
  public static final String TENANT_ID = "diku";
  public static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6ImJlOTZmNDg4LTgwY2YtNTVhNC05Njg3LTE1ZjAyMmE4ZDkyYiIsImlhdCI6MTU4NDA5ODc3MywidGVuYW50IjoiZGlrdSJ9.fI3FHPS23tvLVyk3vfAknvhnvrRNBABPchJdfjV0UNI";

  /**
   * When not run from StorageTestSuite then this method invokes StorageTestSuite.before() and
   * StorageTestSuite.after() to allow to run a single test class, for example from within an
   * IDE during development.
   */
  @BeforeAll
  public static void testBaseBeforeClass() throws InterruptedException, ExecutionException, TimeoutException, IOException {
    vertx = StorageTestSuite.getVertx();
    if (vertx == null) {
      invokeStorageTestSuiteAfter = true;
      StorageTestSuite.before();
      vertx = StorageTestSuite.getVertx();
    }
  }

  @AfterAll
  public static void testBaseAfterClass()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException {

    if (invokeStorageTestSuiteAfter) {
      System.out.println("Running test on own, un-initialising suite manually");
      StorageTestSuite.after();
    }
  }

  @AfterEach
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(new File(STORAGE_DIRECTORY_PATH));
  }

  @AfterAll
  public static void tearDownClass() throws InterruptedException, ExecutionException, TimeoutException, MalformedURLException {
    if (invokeStorageTestSuiteAfter) {
      StorageTestSuite.after();
    }
  }

  @BeforeEach
  public void setUp() throws IOException {
    setUpOkapiConnectionParams();
    setUpJsonRequestSpecification();
    MockServer.release();
  }

  private void setUpOkapiConnectionParams() {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    headers.put(OKAPI_HEADER_URL, MOCK_OKAPI_URL);
    this.okapiConnectionParams = new OkapiConnectionParams(headers);
  }

  private void setUpJsonRequestSpecification() {
    jsonRequestSpecification = new RequestSpecBuilder()
      .setContentType(ContentType.JSON)
      .addHeader(OKAPI_HEADER_TOKEN, TOKEN)
      .addHeader(OKAPI_HEADER_TENANT, TENANT_ID)
      .addHeader(OKAPI_HEADER_URL, MOCK_OKAPI_URL)
      .setBaseUri(BASE_OKAPI_URL)
      .addHeader("Accept", "text/plain, application/json")
      .build();
  }

  protected Response postRequest(JsonObject body, String path) {
    return RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(body.encode())
      .when()
      .post(path);
  }

  protected Response postRequest(JsonObject body, String path, String tenantID) {
    return RestAssured.given()
      .spec(buildCustomJsonRequestSpecification(tenantID))
      .body(body.encode())
      .when()
      .post(path);
  }

  protected Response getRequest(String path) {
    return RestAssured.given()
      .spec(jsonRequestSpecification)
      .when()
      .get(path);
  }

  protected Response getRequestById(String path, String id) {
    return RestAssured.given()
      .spec(jsonRequestSpecification)
      .pathParam("id", id)
      .when()
      .get(path);
  }

  protected Response putRequestById(String path, String id, String body) {
    return RestAssured.given()
      .spec(jsonRequestSpecification)
      .pathParam("id", id)
      .when()
      .body(body)
      .put(path);
  }

  protected Response deleteRequestById(String path, String id) {
    return RestAssured.given()
      .spec(jsonRequestSpecification)
      .pathParam("id", id)
      .when()
      .delete(path);
  }


  public static String getMockData(String path) throws IOException {
    try (InputStream resourceAsStream = RestVerticleTestBase.class.getClassLoader().getResourceAsStream(path)) {
      if (resourceAsStream != null) {
        return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
      } else {
        StringBuilder sb = new StringBuilder();
        try (Stream<String> lines = Files.lines(Paths.get(path))) {
          lines.forEach(sb::append);
        }
        return sb.toString();
      }
    }
  }

  protected RequestSpecification buildRequestSpecification(String tenantID) {
    return new RequestSpecBuilder()
      .setContentType(ContentType.BINARY)
      .addHeader(OKAPI_HEADER_TENANT, tenantID)
      .setBaseUri(BASE_OKAPI_URL)
      .build();
  }

  public static ValidatableResponse postToTenant(Header tenantHeader) throws MalformedURLException {
    String moduleId = String.format("%s-%s", PomReader.INSTANCE.getModuleName(), PomReader.INSTANCE.getVersion());
    JsonObject jsonBody = new JsonObject();
    jsonBody.put("module_to", moduleId);
    URL url = new URL("http", "localhost", port, "/_/tenant");
    return given()
      .header(tenantHeader)
      .header(URL_TO_HEADER)
      .contentType(ContentType.JSON)
      .body(jsonBody.encodePrettily())
      .post(url)
      .then();
  }

  public static void deleteTenant(Header tenantHeader)
      throws MalformedURLException {
      given()
        .header(tenantHeader)
        .contentType(ContentType.JSON)
        .delete("/_/tenant")
        .then()
        .statusCode(204);
    }


  protected RequestSpecification buildCustomJsonRequestSpecification(String tenantId) {
    return new RequestSpecBuilder()
      .setContentType(ContentType.JSON)
      .addHeader(OKAPI_HEADER_TOKEN, TOKEN)
      .addHeader(OKAPI_HEADER_TENANT, tenantId)
      .addHeader(OKAPI_HEADER_URL, MOCK_OKAPI_URL)
      .setBaseUri(BASE_OKAPI_URL)
      .addHeader("Accept", "text/plain, application/json")
      .build();
  }

}
