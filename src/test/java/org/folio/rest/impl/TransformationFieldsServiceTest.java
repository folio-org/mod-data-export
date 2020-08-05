package org.folio.rest.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.restassured.RestAssured;
import io.restassured.internal.path.json.JSONAssertion;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.folio.rest.jaxrs.model.TransformationFieldCollection;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;

import static org.folio.TestUtil.readFileContentFromResources;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
class TransformationFieldsServiceTest extends RestVerticleTestBase {

  private static final String FIELD_NAMES_URL = "/data-export/transformationFields";
  private static final String TRANSFORMATION_FIELDS_MOCK_DATA_PATH = "mapping/expectedTransformationFields.json";
  private static final String TOTAL_RECORDS = "totalRecords";

  @Test
  void getFieldNamesReturned200Status(VertxTestContext context) {
    Response response = RestAssured.given()
      .spec(jsonRequestSpecification)
      .when()
      .get(FIELD_NAMES_URL);

    context.verify(() -> {
      TransformationFieldCollection transformationFieldCollection = response.as(TransformationFieldCollection.class);
      assertEquals(HttpStatus.SC_OK, response.getStatusCode());
      assertFalse(transformationFieldCollection.getTransformationFields().isEmpty());
      JsonObject expectedJson = new JsonObject(readFileContentFromResources(TRANSFORMATION_FIELDS_MOCK_DATA_PATH));
      JsonObject actualJson = new JsonObject(response.body().prettyPrint());
      assertEquals(expectedJson, actualJson);
      assertNotEquals(0, (int) transformationFieldCollection.getTotalRecords());
      assertEquals((int) expectedJson.getValue(TOTAL_RECORDS), transformationFieldCollection.getTotalRecords());
      context.completeNow();
    });
  }

}
