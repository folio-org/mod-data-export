package org.folio.rest.impl;

import io.restassured.RestAssured;
import io.vertx.core.Context;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.service.file.definition.FileDefinitionService;
import org.folio.spring.SpringContextUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class DataExportCleanUpFilesTest extends RestVerticleTestBase {

  @Autowired
  private FileDefinitionService fileDefinitionService;

  public DataExportCleanUpFilesTest() {
    Context vertxContext = vertx.getOrCreateContext();
    SpringContextUtil.init(vertxContext.owner(), vertxContext, DataExportTest.TestMock.class);
    SpringContextUtil.autowireDependencies(this, vertxContext);
  }

  @Test
  void shouldReturn_204Status_forHappyPathCleanUpFiles() {
    RestAssured.given()
      .spec(jsonRequestSpecification)
      .when()
      .post(CLEAN_UP_FILES_URL)
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);
  }

  @Test
  void shouldReturn_204Status_forHappyPathCleanUpFiles_andRemoveOldFileDefinitions(VertxTestContext context) throws ParseException {
    // given
    String fileDefinitionId = UUID.randomUUID().toString();
    String dateString = "2020-09-15T11:02:00.847+0000";
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSZ");
    Date date = dateFormat.parse(dateString);
    FileDefinition fileDefinition = new FileDefinition()
      .withMetadata(new Metadata()
        .withUpdatedDate(date)
        .withCreatedDate(date))
      .withJobExecutionId(UUID.randomUUID().toString())
      .withId(fileDefinitionId)
      .withSourcePath("Path");
    fileDefinitionService.save(fileDefinition, TENANT_ID);

    vertx.setTimer(3000L, ar -> {
      // when
      RestAssured.given()
        .spec(jsonRequestSpecification)
        .when()
        .post(CLEAN_UP_FILES_URL)
        .then()
        .statusCode(HttpStatus.SC_NO_CONTENT);

      // then
      fileDefinitionService.getById(fileDefinitionId, TENANT_ID)
        .onComplete(asyncResult -> {
          assertTrue(asyncResult.succeeded());
          assertNull(asyncResult.result());
          context.completeNow();
        });
    });
  }

}
