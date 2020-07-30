package org.folio.rest.impl;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpStatus;
import org.folio.rest.RestVerticleTestBase;
import org.folio.rest.jaxrs.model.FieldNameCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
class FileNameServiceTest extends RestVerticleTestBase {

  private static final String FIELD_NAMES_URL = "/data-export/fieldNames";

  @Test
  void getFieldNamesReturned200Status(VertxTestContext context) {
    Response response = RestAssured.given()
      .spec(jsonRequestSpecification)
      .when()
      .get(FIELD_NAMES_URL);

    context.verify(() -> {
      FieldNameCollection fieldNameCollection = response.as(FieldNameCollection.class);
      assertEquals(HttpStatus.SC_OK, response.getStatusCode());
      assertFalse(fieldNameCollection.getFieldNames().isEmpty());
      context.completeNow();
    });
  }

}
