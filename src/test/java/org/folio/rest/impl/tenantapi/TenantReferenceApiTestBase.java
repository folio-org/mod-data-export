package org.folio.rest.impl.tenantapi;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.vertx.core.Vertx;
import org.folio.rest.impl.StorageTestSuite;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static io.restassured.RestAssured.given;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;
import static org.hamcrest.Matchers.equalTo;

public class TenantReferenceApiTestBase {

  private static boolean invokeStorageTestSuiteAfter = false;

  @BeforeAll
  public static void testBaseBeforeClass() throws InterruptedException, ExecutionException, TimeoutException, IOException {
    Vertx vertx = StorageTestSuite.getVertx();
    if (vertx == null) {
      invokeStorageTestSuiteAfter = true;
      StorageTestSuite.before();
    }

  }

  @AfterAll
  public static void testBaseAfterClass() throws InterruptedException, ExecutionException, TimeoutException {
    if (invokeStorageTestSuiteAfter) {
      StorageTestSuite.after();
    }
  }

  void verifyCollectionQuantity(String endpoint, int quantity, Header tenantHeader) throws MalformedURLException {
    getData(endpoint, tenantHeader)
      .then()
      .log().all()
      .statusCode(200)
      .body("totalRecords", equalTo(quantity));
  }

  Response getData(String endpoint, Header tenantHeader) throws MalformedURLException {
    return given()
      .header(tenantHeader)
      .contentType(ContentType.JSON)
      .get(storageUrl(endpoint));
  }

}
