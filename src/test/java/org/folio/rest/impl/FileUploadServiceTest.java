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
import org.folio.rest.jaxrs.model.FileDefinition;
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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class FileUploadServiceTest extends AbstractRestTest {
  private static final String STORAGE_ROOT_PATH = "./storage";
  private static final String FILE_DEFINITION_SERVICE_URL = "/data-export/fileDefinitions";

  @Test
  public void postFileDefinition_return201(TestContext context) {
    Async async = context.async();
    // given
    FileDefinition givenFileDefinition = new FileDefinition()
      .withId(UUID.randomUUID().toString())
      .withFileName("InventoryUUIDs.csv");
    // when created a new entity
    RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(givenFileDefinition).encode())
      .when()
      .post(FILE_DEFINITION_SERVICE_URL)
      .then()
      .statusCode(HttpStatus.SC_CREATED);
    // then retrieve it and verify
    Response response = RestAssured.given()
      .spec(jsonRequestSpecification)
      .get(FILE_DEFINITION_SERVICE_URL + "/" + givenFileDefinition.getId());
    context.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
    FileDefinition createdFileDefinition = response.as(FileDefinition.class);
    context.assertEquals(givenFileDefinition.getId(), createdFileDefinition.getId());
    context.assertEquals(givenFileDefinition.getFileName(), createdFileDefinition.getFileName());
    context.assertEquals(FileDefinition.Status.NEW, createdFileDefinition.getStatus());
    async.complete();
  }

  @Test
  public void postFileDefinition_return422(TestContext context) {
    // given
    FileDefinition givenEntity = new FileDefinition();
    // when
    Response response = RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(givenEntity).encode())
      .when()
      .post(FILE_DEFINITION_SERVICE_URL);
    // then
    // then
    context.assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatusCode());
  }

  @Test
  public void getFileDefinition_return404(TestContext context) {
    // when
    Response response = RestAssured.given()
      .spec(jsonRequestSpecification)
      .get(FILE_DEFINITION_SERVICE_URL + "/" + UUID.randomUUID().toString());
    // then
    context.assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
  }

  @Test
  public void shouldUploadFile_return200(TestContext context) throws IOException {
    Async async = context.async();
    // given fileToUpload, binaryRequestSpecification and fileDefinition
    File fileToUpload = getFileByName("InventoryUUIDs.csv");
    RequestSpecification binaryRequestSpecification = new RequestSpecBuilder()
      .setContentType(ContentType.BINARY)
      .addHeader(OKAPI_HEADER_TENANT, TENANT_ID)
      .setBaseUri(OKAPI_URL)
      .build();

    FileDefinition givenFileDefinition = new FileDefinition()
      .withId(UUID.randomUUID().toString())
      .withFileName("InventoryUUIDs.csv");
    // when created a new file definition
    RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(givenFileDefinition).encode())
      .when()
      .post(FILE_DEFINITION_SERVICE_URL)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .body("sourcePath", nullValue())
      .body("createdDate", notNullValue())
      .body("status", is(FileDefinition.Status.NEW.name()));
    // then we can start file uploading, assert response body and check file content
    FileDefinition uploadedFileDefinition = RestAssured.given()
      .spec(binaryRequestSpecification)
      .when()
      .body(FileUtils.openInputStream(fileToUpload))
      .post(FILE_DEFINITION_SERVICE_URL + "/" + givenFileDefinition.getId())
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("sourcePath", notNullValue())
      .body("createdDate", notNullValue())
      .body("status", is(FileDefinition.Status.COMPLETED.name()))
      .extract().body().as(FileDefinition.class);
    File uploadedFile = new File(uploadedFileDefinition.getSourcePath());
    assertTrue(FileUtils.contentEquals(fileToUpload, uploadedFile));
    FileUtils.deleteDirectory(new File(STORAGE_ROOT_PATH));
    async.complete();
  }

  @NotNull
  private File getFileByName(String fileName) {
    ClassLoader classLoader = getClass().getClassLoader();
    return new File(Objects.requireNonNull(classLoader.getResource("files/" + fileName)).getFile());
  }
}
