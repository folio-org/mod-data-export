package org.folio.rest.impl;

import java.util.UUID;

import org.apache.http.HttpStatus;
import org.assertj.core.util.Lists;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.Transformations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class MappingProfileServiceTest extends RestVerticleTestBase {

  private static final String INVALID_TRANSFORMATION = "123qq$qweqwe";
  private static final String INVALID_TAG = "12a  $a";
  private static final String INVALID_FIRST_INDICATOR = "123. $a";
  private static final String INVALID_SECOND_INDICATOR = "123 ,$a";
  private static final String INVALID_SUBFIELD = "123  $1234";
  private static final String EMPTY_TRANSFORMATION = "";
  private static final String VALID_TRANSFORMATION = "13qq$q";

  @Test
  void postMappingProfile_return422Status_whenTransformationFieldIdDoesntExist() {
    MappingProfile mappingProfile = new MappingProfile()
      .withName("mappingProfileName")
      .withRecordTypes(Lists.newArrayList(RecordType.INSTANCE))
      .withTransformations(Lists.newArrayList(new Transformations()
        .withFieldId("missingFieldId")
        .withRecordType(RecordType.INSTANCE)));
    postMappingProfileAndVerifyStatusCode(mappingProfile, HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  void postMappingProfile_return422Status_whenTransformationRecordTypeIncorrect() {
    MappingProfile mappingProfile = new MappingProfile()
      .withName("mappingProfileName")
      .withRecordTypes(Lists.newArrayList(RecordType.HOLDINGS))
      .withTransformations(Lists.newArrayList(new Transformations()
        .withFieldId("instance.id")
        .withRecordType(RecordType.HOLDINGS)));
    postMappingProfileAndVerifyStatusCode(mappingProfile, HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  void postMappingProfile_return422Status_whenTransformationInvalid() {
    MappingProfile mappingProfile = new MappingProfile()
      .withName("mappingProfileName")
      .withRecordTypes(Lists.newArrayList(RecordType.HOLDINGS))
      .withTransformations(Lists.newArrayList(new Transformations()
        .withFieldId("instance.id")
        .withRecordType(RecordType.INSTANCE)
        .withTransformation(INVALID_TRANSFORMATION)));
    postMappingProfileAndVerifyStatusCode(mappingProfile, HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  void postMappingProfile_return422Status_whenTransformationHasInvalidTag() {
    MappingProfile mappingProfile = new MappingProfile()
      .withName("mappingProfileName")
      .withRecordTypes(Lists.newArrayList(RecordType.HOLDINGS))
      .withTransformations(Lists.newArrayList(new Transformations()
        .withFieldId("instance.id")
        .withRecordType(RecordType.INSTANCE)
        .withTransformation(INVALID_TAG)));
    postMappingProfileAndVerifyStatusCode(mappingProfile, HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  void postMappingProfile_return422Status_whenTransformationHasInvalidFirstIndicator() {
    MappingProfile mappingProfile = new MappingProfile()
      .withName("mappingProfileName")
      .withRecordTypes(Lists.newArrayList(RecordType.HOLDINGS))
      .withTransformations(Lists.newArrayList(new Transformations()
        .withFieldId("instance.id")
        .withRecordType(RecordType.INSTANCE)
        .withTransformation(INVALID_FIRST_INDICATOR)));
    postMappingProfileAndVerifyStatusCode(mappingProfile, HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  void postMappingProfile_return422Status_whenTransformationHasInvalidSecondIndicator() {
    MappingProfile mappingProfile = new MappingProfile()
      .withName("mappingProfileName")
      .withRecordTypes(Lists.newArrayList(RecordType.HOLDINGS))
      .withTransformations(Lists.newArrayList(new Transformations()
        .withFieldId("instance.id")
        .withRecordType(RecordType.INSTANCE)
        .withTransformation(INVALID_SECOND_INDICATOR)));
    postMappingProfileAndVerifyStatusCode(mappingProfile, HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  void postMappingProfile_return422Status_whenTransformationHasInvalidSubfield() {
    MappingProfile mappingProfile = new MappingProfile()
      .withName("mappingProfileName")
      .withRecordTypes(Lists.newArrayList(RecordType.HOLDINGS))
      .withTransformations(Lists.newArrayList(new Transformations()
        .withFieldId("instance.id")
        .withRecordType(RecordType.INSTANCE)
        .withTransformation(INVALID_SUBFIELD)));
    postMappingProfileAndVerifyStatusCode(mappingProfile, HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  void postMappingProfile_return422Status_whenTransformationItemRecordTypeAndEmptyValue() {
    MappingProfile mappingProfile = new MappingProfile()
      .withName("mappingProfileName")
      .withRecordTypes(Lists.newArrayList(RecordType.ITEM))
      .withTransformations(Lists.newArrayList(new Transformations()
        .withFieldId("item.chronology")
        .withRecordType(RecordType.ITEM)
        .withTransformation(EMPTY_TRANSFORMATION)));
    postMappingProfileAndVerifyStatusCode(mappingProfile, HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  void postMappingProfile_return201Status_whenTransformationHoldingRecordTypeAndEmptyValue() {
    MappingProfile mappingProfile = new MappingProfile()
      .withName(UUID.randomUUID().toString())
      .withRecordTypes(Lists.newArrayList(RecordType.HOLDINGS))
      .withTransformations(Lists.newArrayList(new Transformations()
        .withFieldId("holdings.hrid")
        .withPath("$.holdings[*].hrid")
        .withRecordType(RecordType.HOLDINGS)
        .withTransformation(EMPTY_TRANSFORMATION)));
    postMappingProfileAndVerifyStatusCode(mappingProfile, HttpStatus.SC_CREATED);
  }

  @Test
  void postMappingProfile_return201Status_whenTransformationInstanceRecordTypeAndEmptyValue() {
    MappingProfile mappingProfile = new MappingProfile()
      .withName(UUID.randomUUID().toString())
      .withRecordTypes(Lists.newArrayList(RecordType.INSTANCE))
      .withTransformations(Lists.newArrayList(new Transformations()
        .withFieldId("instance.id")
        .withPath("$.instance.id")
        .withRecordType(RecordType.INSTANCE)
        .withTransformation(EMPTY_TRANSFORMATION)));
    postMappingProfileAndVerifyStatusCode(mappingProfile, HttpStatus.SC_CREATED);
  }

  private void postMappingProfileAndVerifyStatusCode(MappingProfile mappingProfile, int expectedStatusCode) {
    RestAssured.given()
      .spec(jsonRequestSpecification)
      .body(JsonObject.mapFrom(mappingProfile).encode())
      .when()
      .post(MAPPING_PROFILE_URL)
      .then()
      .statusCode(expectedStatusCode);
  }

}
