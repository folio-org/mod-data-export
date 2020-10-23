package org.folio.rest.impl;

import io.restassured.RestAssured;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
class DataExportCleanUpFilesTest extends RestVerticleTestBase {

  @Test
  void shouldReturn_204Status_forHappyPathCleanUpFiles() {
    RestAssured.given()
      .spec(jsonRequestSpecification)
      .when()
      .post(CLEAN_UP_FILES_URL)
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);
  }

}
