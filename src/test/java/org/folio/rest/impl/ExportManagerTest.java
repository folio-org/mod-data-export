package org.folio.rest.impl;

import static org.junit.Assert.assertEquals;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpStatus;
import org.folio.rest.RestVerticleTestBase;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;

import java.util.UUID;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
public class ExportManagerTest extends RestVerticleTestBase {

  private static final String EXPORT_URL = "/data-export/export";

  @Test
  public void shouldReturn_204Status_forHappyPath(VertxTestContext context) {
    // given
    ExportRequest exportRequest = new ExportRequest()
      .withFileDefinitionId(UUID.randomUUID().toString())
      .withJobProfileId(UUID.randomUUID().toString());
    // when
    Response response = RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(exportRequest).encode())
      .when()
      .post(EXPORT_URL);
    // then
    context.verify(()->{
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        context.completeNow();
    });
  }

  @Test
  public void shouldReturn_422Status_ifRequestIsWrong(VertxTestContext context) {
    // given
    ExportRequest exportRequest = new ExportRequest();
    // when
    Response response = RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(exportRequest)
        .encode())
      .when()
      .post(EXPORT_URL);
    // then
    context.verify(() -> {
      assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatusCode());
      context.completeNow();
    });
  }

}
