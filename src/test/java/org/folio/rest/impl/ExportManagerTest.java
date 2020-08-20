package org.folio.rest.impl;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
class ExportManagerTest extends RestVerticleTestBase {
  @Test
  void shouldReturn_422Status_ifRequestIsWrong(VertxTestContext context) {
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

  @Test
  void shouldReturn_400Status_ifFileDefinitionNotFound(VertxTestContext context) {
    // given
    ExportRequest exportRequest = new ExportRequest()
      .withFileDefinitionId(UUID.randomUUID().toString())
      .withJobProfileId(DEFAULT_JOB_PROFILE_ID);
    // when
    Response response = RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(exportRequest).encode())
      .when()
      .post(EXPORT_URL);
    // then
    context.verify(() -> {
      assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
      assertEquals(String.format("File definition not found with id %s", exportRequest.getFileDefinitionId()), response.body().print());
      context.completeNow();
    });
  }

  @Test
  void shouldReturn_400Status_ifJobProfileNotFound(VertxTestContext context) {
    // given
    ExportRequest exportRequest = new ExportRequest()
      .withFileDefinitionId(UUID.randomUUID().toString())
      .withJobProfileId(UUID.randomUUID().toString());
    FileDefinition fileDefinition = new FileDefinition()
      .withId(exportRequest.getFileDefinitionId())
      .withFileName("fileName.csv");
    postRequest(JsonObject.mapFrom(fileDefinition), FILE_DEFINITION_SERVICE_URL);

    // when
    Response response = RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(exportRequest)
        .encode())
      .when()
      .post(EXPORT_URL);
    // then
    context.verify(() -> {
      assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
      assertEquals(String.format("JobProfile not found with id %s", exportRequest.getJobProfileId()), response.body().print());
      context.completeNow();
    });
  }

}
