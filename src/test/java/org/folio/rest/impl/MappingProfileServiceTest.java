//package org.folio.rest.impl;
//
//import io.restassured.RestAssured;
//import io.restassured.response.Response;
//import io.vertx.core.json.JsonObject;
//import io.vertx.ext.unit.Async;
//import io.vertx.ext.unit.TestContext;
//import io.vertx.ext.unit.junit.VertxUnitRunner;
//import org.apache.http.HttpStatus;
//import org.folio.rest.RestVerticleTestBase;
//import org.folio.rest.jaxrs.model.MappingProfile;
//import org.folio.rest.jaxrs.model.Metadata;
//import org.folio.rest.jaxrs.model.RecordType;
//import org.folio.rest.jaxrs.model.Transformations;
//import org.folio.rest.jaxrs.model.UserInfo;
//import org.folio.rest.persist.Criteria.Criterion;
//import org.folio.rest.persist.PostgresClient;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import java.util.Date;
//import java.util.UUID;
//
//import static java.util.Collections.singletonList;
//import static org.hamcrest.Matchers.empty;
//import static org.hamcrest.Matchers.is;
//
//@RunWith(VertxUnitRunner.class)
//public class MappingProfileServiceTest extends RestVerticleTestBase {
//  private static final String MAPPING_PROFILES_URL = "/data-export/mappingProfiles";
//  private static final String TABLE = "mapping_profiles";
//
//  MappingProfile mappingProfile = new MappingProfile()
//    .withId(UUID.randomUUID().toString())
//    .withName("The first mapping profile")
//    .withRecordTypes(singletonList(RecordType.INSTANCE))
//    .withOutputFormat(MappingProfile.OutputFormat.MARC)
//    .withTransformations(singletonList(new Transformations()
//      .withFieldId("electronicAccess.linkText")
//      .withRecordType(RecordType.INSTANCE)))
//    .withUserInfo(new UserInfo())
//    .withMetadata(new Metadata()
//      .withCreatedDate(new Date()));
//
//
//  @Before
//  public void clearTable(TestContext context) {
//    Async async = context.async();
//    PostgresClient pgClient = PostgresClient.getInstance(vertx, TENANT_ID);
//    pgClient.delete(TABLE, new Criterion(), event -> {
//      if (event.failed()) {
//        context.fail(event.cause());
//      }
//      async.complete();
//    });
//  }
//
//
//  @Test
//  public void getMappingProfilesByQuery_return200Status_forHappyPath(TestContext context) {
//    RestAssured.given()
//      .spec(jsonRequestSpecification)
//      .when()
//      .get(MAPPING_PROFILES_URL + DELIMITER + "?query=recordTypes=INSTANCE")
//      .then()
//      .statusCode(HttpStatus.SC_OK);
//  }
//
//  @Test
//  public void getMappingProfilesByQuery_shouldReturnAllProfiles() {
//    createProfile();
//    RestAssured.given()
//      .spec(jsonRequestSpecification)
//      .when()
//      .get(MAPPING_PROFILES_URL + DELIMITER + "?outputFormat=MARC")
//      .then().log().all()
//      .statusCode(HttpStatus.SC_OK)
//      .body("totalRecords", is(1));
//  }
//
//  @Test
//  public void getMappingProfilesByQuery_return200StatusWithEmptyList_ifNoMappingProfilesWithGivenQuery(TestContext context) {
//    RestAssured.given()
//      .spec(jsonRequestSpecification)
//      .when()
//      .get(MAPPING_PROFILES_URL + DELIMITER + "?query=id=" + UUID.randomUUID().toString())
//      .then()
//      .statusCode(HttpStatus.SC_OK)
//      .body("mappingProfiles", empty())
//      .body("totalRecords", is(0));
//  }
//
//  @Test
//  public void getMappingProfilesById_return200Status_andGivenProfile(TestContext context) {
//    createProfile();
//    RestAssured.given()
//      .spec(jsonRequestSpecification)
//      .when()
//      .get(MAPPING_PROFILES_URL + DELIMITER + mappingProfile.getId())
//      .then()
//      .statusCode(HttpStatus.SC_OK)
//      .body("id", is(mappingProfile.getId()))
//      .body("name", is(mappingProfile.getName()))
//      .body("description", is(mappingProfile.getDescription()))
//      .body("transformations[0].fieldId", is(mappingProfile.getTransformations().get(0).getFieldId()))
//      .body("transformations[0].recordType", is(mappingProfile.getTransformations().get(0).getRecordType().toString()))
//      .body("outputFormat", is(mappingProfile.getOutputFormat().toString()));
//
//  }
//
//  @Test
//  public void postMappingProfiles_shouldReturn201StatusForHappyPath_422StatusForTheSecondCreation(TestContext context) {
//    RestAssured.given()
//      .spec(jsonRequestSpecification)
//      .body(mappingProfile)
//      .when()
//      .post(MAPPING_PROFILES_URL)
//      .then()
//      .statusCode(HttpStatus.SC_CREATED);
//
//    RestAssured.given().spec(jsonRequestSpecification)
//      .body(mappingProfile)
//      .when()
//      .post(MAPPING_PROFILES_URL)
//      .then().log().all()
//      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
//  }
//
//  @Test
//  public void postMappingProfiles_shouldReturnBadRequest() {
//    createProfile();
//    RestAssured.given()
//      .spec(jsonRequestSpecification)
//      .body(new JsonObject().toString())
//      .when()
//      .post(MAPPING_PROFILES_URL)
//      .then()
//      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
//  }
//
//  @Test
//  public void putMappingProfiles_shouldReturnNotFound() {
//    RestAssured.given()
//      .spec(jsonRequestSpecification)
//      .body(mappingProfile)
//      .when()
//      .put(MAPPING_PROFILES_URL + DELIMITER + UUID.randomUUID().toString())
//      .then()
//      .statusCode(HttpStatus.SC_NOT_FOUND);
//  }
//
//  @Test
//  public void putMappingProfiles_shouldUpdateProfile() {
//    Response response = RestAssured.given()
//      .spec(jsonRequestSpecification)
//      .body(mappingProfile)
//      .when()
//      .post(MAPPING_PROFILES_URL);
//    Assert.assertThat(response.statusCode(), is(HttpStatus.SC_CREATED));
//    MappingProfile mappingProfileToUpdate = response.body().as(MappingProfile.class);
//    mappingProfileToUpdate.setName("testName");
//
//    RestAssured.given()
//      .spec(jsonRequestSpecification)
//      .body(mappingProfileToUpdate)
//      .when()
//      .put(MAPPING_PROFILES_URL + DELIMITER + mappingProfile.getId())
//      .then()
//      .statusCode(HttpStatus.SC_NO_CONTENT);
//
//    RestAssured.given()
//      .spec(jsonRequestSpecification)
//      .when()
//      .get(MAPPING_PROFILES_URL + DELIMITER + mappingProfile.getId())
//      .then()
//      .statusCode(HttpStatus.SC_OK)
//      .body("name", is(mappingProfileToUpdate.getName()));
//  }
//
//  @Test
//  public void deleteMappingProfiles_shouldReturnNotFound() {
//    RestAssured.given()
//      .spec(jsonRequestSpecification)
//      .when()
//      .delete(MAPPING_PROFILES_URL + DELIMITER + UUID.randomUUID().toString())
//      .then()
//      .log().all()
//      .statusCode(HttpStatus.SC_NOT_FOUND);
//  }
//
//  @Test
//  public void deleteMappingProfiles_shouldDeleteProfile() {
//    createProfile();
//    RestAssured.given()
//      .spec(jsonRequestSpecification)
//      .when()
//      .delete(MAPPING_PROFILES_URL + DELIMITER + mappingProfile.getId())
//      .then()
//      .statusCode(HttpStatus.SC_NO_CONTENT);
//  }
//
//  private void createProfile() {
//    RestAssured.given()
//      .spec(jsonRequestSpecification)
//      .body(mappingProfile)
//      .when()
//      .post(MAPPING_PROFILES_URL)
//      .then()
//      .statusCode(HttpStatus.SC_CREATED);
//  }
//
//}
