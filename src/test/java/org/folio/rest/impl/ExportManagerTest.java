package org.folio.rest.impl;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.folio.rest.RestVerticleTestBase;
import org.folio.rest.jaxrs.model.ExportRequest;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobProfile;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(VertxUnitRunner.class)
public class ExportManagerTest extends RestVerticleTestBase {

  private static final String EXPORT_URL = "/data-export/export";
  private static final String FILE_DEFINITION_SERVICE_URL = "/data-export/fileDefinitions";
  private static final String JOB_EXECUTIONS_URL = "/data-export/jobExecutions";

  @Test
  public void shouldReturn_204Status_forHappyPath(TestContext context) throws IOException {
    Async async = context.async();

    File fileToUpload = getFileByName("InventoryUUIDs.csv");
    RequestSpecification binaryRequestSpecification = new RequestSpecBuilder()
      .setContentType(ContentType.BINARY)
      .addHeader(OKAPI_HEADER_TENANT, TENANT_ID)
      .setBaseUri(OKAPI_URL)
      .build();

    //given
    FileDefinition givenFileDefinition = new FileDefinition()
      .withId(UUID.randomUUID().toString())
      .withFileName("InventoryUUIDs.csv");
    RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(givenFileDefinition).encode())
      .when()
      .post(FILE_DEFINITION_SERVICE_URL)
      .then()
      .statusCode(HttpStatus.SC_CREATED);
    FileDefinition uploadedFileDefinition = RestAssured.given()
      .spec(binaryRequestSpecification)
      .when()
      .body(FileUtils.openInputStream(fileToUpload))
      .post(FILE_DEFINITION_SERVICE_URL + "/" + givenFileDefinition.getId() + "/upload")
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("sourcePath", notNullValue())
      .body("metadata.createdDate", notNullValue())
      .body("status", is(FileDefinition.Status.COMPLETED.name()))
      .extract().body().as(FileDefinition.class);

    // when
    ExportRequest exportRequest = new ExportRequest()
      .withFileDefinition(uploadedFileDefinition)
      .withJobProfile(new JobProfile()
        .withId(UUID.randomUUID().toString())
        .withDestination("fileSystem")
      );
    Response response = RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(exportRequest).encode())
      .when()
      .post(EXPORT_URL);

    // then
    context.assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

    FileUtils.deleteDirectory(new File("./storage"));
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

  @NotNull
  private File getFileByName(String fileName) {
    ClassLoader classLoader = getClass().getClassLoader();
    return new File(Objects.requireNonNull(classLoader.getResource("files/" + fileName)).getFile());
  }
}
