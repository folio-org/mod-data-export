package org.folio.rest.impl;

import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import org.apache.http.HttpStatus;
import org.assertj.core.util.Lists;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.Transformations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class MappingProfileServiceTest extends RestVerticleTestBase {

  @Test
  void postMappingProfile_return422Status_whenTransformationFieldIdDoesntExist() {
    MappingProfile mappingProfile = new MappingProfile()
      .withName("mappingProfileName")
      .withRecordTypes(Lists.newArrayList(RecordType.INSTANCE))
      .withTransformations(Lists.newArrayList(new Transformations()
        .withFieldId("missingFieldId")
        .withRecordType(RecordType.INSTANCE)));
    RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(mappingProfile).encode())
      .when()
      .post(MAPPING_PROFILE_URL)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  void postMappingProfile_return422Status_whenTransformationRecordTypeIncorrect() {
    MappingProfile mappingProfile = new MappingProfile()
      .withName("mappingProfileName")
      .withRecordTypes(Lists.newArrayList(RecordType.HOLDINGS))
      .withTransformations(Lists.newArrayList(new Transformations()
        .withFieldId("instance.id")
        .withRecordType(RecordType.HOLDINGS)));
    RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(mappingProfile).encode())
      .when()
      .post(MAPPING_PROFILE_URL)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  void postMappingProfile_return201Status() {
    MappingProfile mappingProfile = new MappingProfile()
      .withName("mappingProfileName")
      .withRecordTypes(Lists.newArrayList(RecordType.INSTANCE))
      .withTransformations(Lists.newArrayList(new Transformations()
        .withFieldId("instance.id")
        .withRecordType(RecordType.INSTANCE)));
    RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(mappingProfile).encode())
      .when()
      .post(MAPPING_PROFILE_URL)
      .then()
      .statusCode(HttpStatus.SC_CREATED);
  }
}
