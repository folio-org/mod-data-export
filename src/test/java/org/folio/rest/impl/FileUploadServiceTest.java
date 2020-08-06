package org.folio.rest.impl;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecutionCollection;
import org.folio.util.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
class FileUploadServiceTest extends RestVerticleTestBase {
  @Test
  void postFileDefinition_return200Status(VertxTestContext context) {
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
    context.verify(() -> {
      assertEquals(HttpStatus.SC_OK, response.getStatusCode());
      FileDefinition createdFileDefinition = response.as(FileDefinition.class);
      assertEquals(givenFileDefinition.getId(), createdFileDefinition.getId());
      assertEquals(givenFileDefinition.getFileName(), createdFileDefinition.getFileName());
      assertEquals(FileDefinition.Status.NEW, createdFileDefinition.getStatus());
      context.completeNow();
    });

  }

  @Test
  void postFileDefinition_return422Status_whenFileNameExtensionNotCsv(VertxTestContext context) {
    // given
    FileDefinition givenFileDefinition = new FileDefinition()
      .withId(UUID.randomUUID().toString())
      .withFileName("InventoryUUIDs.txt");
    // when
    Response response = RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(givenFileDefinition).encode())
      .when()
      .post(FILE_DEFINITION_SERVICE_URL);
    // then
    context.verify(() -> {
      assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatusCode());
      assertEquals(ErrorCode.INVALID_UPLOADED_FILE_EXTENSION.getDescription(), response.getBody().asString());
      context.completeNow();
    });

  }

  @Test
  void postFileDefinition_return422Status_whenFileNameRequestParamMissing(VertxTestContext context) {
    // given
    FileDefinition givenEntity = new FileDefinition();
    // when
    Response response = RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(givenEntity).encode())
      .when()
      .post(FILE_DEFINITION_SERVICE_URL);
    // then

    context.verify(() -> {
      assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatusCode());
      context.completeNow();
    });

  }

  @Test
  void getFileDefinition_return404Status(VertxTestContext context) {
    // when
    Response response = RestAssured.given()
      .spec(jsonRequestSpecification)
      .get(FILE_DEFINITION_SERVICE_URL + "/" + UUID.randomUUID().toString());
    // then
    context.verify(() -> {
      assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
      context.completeNow();
    });

  }

  @Test
  void shouldUploadFile_return200Status(VertxTestContext context) throws IOException {
    // given fileToUpload, binaryRequestSpecification and fileDefinition
    File fileToUpload = getFileByName("InventoryUUIDs.csv");
    RequestSpecification binaryRequestSpecification = new RequestSpecBuilder()
      .setContentType(ContentType.BINARY)
      .addHeader(OKAPI_HEADER_TENANT, TENANT_ID)
      .setBaseUri(BASE_OKAPI_URL)
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
      .statusCode(HttpStatus.SC_CREATED);
    // then we can start file uploading and assert response body and check file content
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
    // and then created job execution for current upload file definition
    JobExecutionCollection jobExecutions = RestAssured.given()
      .spec(jsonRequestSpecification)
      .when()
      .get(JOB_EXECUTIONS_URL + "/" + "?query=id=" + uploadedFileDefinition.getJobExecutionId())
      .then()
      .statusCode(HttpStatus.SC_OK)
      .extract().body().as(JobExecutionCollection.class);

    File uploadedFile = new File(uploadedFileDefinition.getSourcePath());

    context.verify(() -> {
      assertTrue(FileUtils.contentEquals(fileToUpload, uploadedFile));
      assertEquals(uploadedFileDefinition.getJobExecutionId(), jobExecutions.getJobExecutions().get(0).getId());
      assertNotNull(jobExecutions.getJobExecutions().get(0).getHrId());
      context.completeNow();
    });

    // clean up storage
    FileUtils.deleteDirectory(new File("./storage"));
  }

  @NotNull
  private File getFileByName(String fileName) {
    ClassLoader classLoader = getClass().getClassLoader();
    return new File(Objects.requireNonNull(classLoader.getResource("files/" + fileName)).getFile());
  }
}
