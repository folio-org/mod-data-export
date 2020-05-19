package org.folio.rest.impl;

import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.TestUtil;
import org.folio.rest.RestVerticleTestBase;
import org.folio.util.TestEntities;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EntitiesCrudTest extends RestVerticleTestBase {

  private final Logger logger = LoggerFactory.getLogger(EntitiesCrudTest.class);
  private String sample = null;

  static Stream<TestEntities> deleteOrder() {
    return Stream.of(
      TestEntities.JOBPROFILE,
      TestEntities.MAPPINGPROFILE);
  }

  @ParameterizedTest
  @Order(1)
  @EnumSource(TestEntities.class)
  void testVerifyCollection(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-data-export %s test: Verifying database's initial state ... ", testEntity.name()));
    getRequest(testEntity.getEndpoint()).then()
      .log()
      .all()
      .statusCode(200)
      .body("totalRecords", equalTo(1));
  }

  @ParameterizedTest
  @Order(2)
  @EnumSource(TestEntities.class)
  void testPostData(TestEntities testEntity) throws IOException {
    logger.info(String.format("--- mod-data-export %s test: Creating %s ... ", testEntity.name(), testEntity.name()));
    sample = getSample(testEntity.getSampleFileName());
    Response response = postRequest(new JsonObject(sample), testEntity.getEndpoint());
    testEntity.setId(response.then()
      .extract()
      .path("id"));
    response.then().log()
    .all()
    .statusCode(201);
  }

  @ParameterizedTest
  @Order(3)
  @EnumSource(TestEntities.class)
  void testVerifyCollectionQuantity(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-data-export %s test: Verifying only 1 record was created, 2 records in total with default... ", testEntity.name()));
    getRequest(testEntity.getEndpoint()).then()
      .log()
      .all()
      .statusCode(200)
      .body("totalRecords", equalTo(2));

  }

  @ParameterizedTest
  @Order(4)
  @EnumSource(TestEntities.class)
  void testGetById(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-data-export %s test: Fetching %s with ID: %s", testEntity.name(), testEntity.name(),
        testEntity.getId()));
    getRequestById(testEntity.getEndpointWithId(), testEntity.getId()).then()
      .log()
      .ifValidationFails()
      .statusCode(200)
      .body("id", equalTo(testEntity.getId()));
  }

  @ParameterizedTest
  @Order(5)
  @EnumSource(TestEntities.class)
  void testPutById(TestEntities testEntity) throws IOException {
    logger.info(String.format("--- mod-data-export %s test: Editing %s with ID: %s", testEntity.name(), testEntity.name(),
        testEntity.getId()));
    JsonObject catJSON = new JsonObject(getSample(testEntity.getSampleFileName()));
    catJSON.put("id", testEntity.getId());
    catJSON.put(testEntity.getUpdatedFieldName(), testEntity.getUpdatedFieldValue());
    putRequestById(testEntity.getEndpointWithId(), testEntity.getId(), catJSON.toString()).then()
      .log()
      .ifValidationFails()
      .statusCode(204);

  }

  @ParameterizedTest
  @Order(6)
  @EnumSource(value = TestEntities.class, names = {"MAPPINGPROFILE"})
  void testDeleteEndpoint_foreginKeyFailure(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-data-exports %s test failure with Foreign Key Constraint: Deleting %s with ID: %s", testEntity.name(), testEntity.name(),
        testEntity.getId()));
    deleteRequestById(testEntity.getEndpointWithId(), testEntity.getId()).then()
      .log()
      .ifValidationFails()
      .statusCode(422);
  }

  @ParameterizedTest
  @Order(7)
  @MethodSource("deleteOrder")
  void testDeleteEndpoint(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-data-exports %s test: Deleting %s with ID: %s", testEntity.name(), testEntity.name(),
        testEntity.getId()));
    deleteRequestById(testEntity.getEndpointWithId(), testEntity.getId()).then()
      .log()
      .ifValidationFails()
      .statusCode(204);
  }

  @ParameterizedTest
  @Order(8)
  @EnumSource(TestEntities.class)
  void testVerifyDelete(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-data-exports %s test: Verify %s is deleted with ID: %s", testEntity.name(),
        testEntity.name(), testEntity.getId()));
    deleteRequestById(testEntity.getEndpointWithId(), testEntity.getId()).then()
    .log()
    .ifValidationFails()
    .statusCode(404);
  }


  private String getSample(String fileName) throws IOException {
    return TestUtil.readFileContentFromResources(fileName);
  }

}
