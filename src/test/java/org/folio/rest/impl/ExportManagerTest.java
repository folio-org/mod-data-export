package org.folio.rest.impl;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.service.manager.ExportManager;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

@RunWith(VertxUnitRunner.class)
public class ExportManagerTest extends AbstractRestTest {

  private static final String EXPORT_URL = "/data-export/export";

  @Test
  public void shouldReturn_204Status_forHappyPath(TestContext context) {
    Async async = context.async();
    // given
    ExportManager.create(Vertx.vertx());
    ExportRequest exportRequest = new ExportRequest()
    .withFileDefinition(new FileDefinition()
      .withId(UUID.randomUUID().toString())
      .withFileName("inventoryUUIDs.csv"))
    .withJobProfile(new JobProfile()
      .withId(UUID.randomUUID().toString())
      .withDestination("fileSystem")
    );
    // when
    Response response = RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(exportRequest).encode())
      .when()
      .post(EXPORT_URL);
    // then
    context.assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
    async.complete();
  }

  @Test
  public void shouldReturn_422Status_ifRequestIsWrong(TestContext context) {
    Async async = context.async();
    // given
    ExportRequest exportRequest = new ExportRequest();
    // when
    Response response = RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(exportRequest).encode())
      .when()
      .post(EXPORT_URL);
    // then
    context.assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatusCode());
    async.complete();
  }
}
