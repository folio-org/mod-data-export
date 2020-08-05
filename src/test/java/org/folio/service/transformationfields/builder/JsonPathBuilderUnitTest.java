package org.folio.service.transformationfields.builder;

import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.TransformationField.RecordType;
import org.folio.service.transformationfields.TransformationFieldsConfig;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


@RunWith(MockitoJUnitRunner.class)
class JsonPathBuilderUnitTest {

  private static final String HR_ID_JSON_PATH_RESULT = "$.instance.hrid";
  private static final String IDENTIFIER_TYPES_LCCN_ID = "c858e4f2-2b6b-4385-842b-60732ee14abb";
  private static final String ALTERNATIVE_TITLE_ID = "5c364ce4-c8fd-4891-a28d-bb91c9bcdbfb";
  private static final String LCCN_RESPONSE_AS_STRING = "{\"id\":\"c858e4f2-2b6b-4385-842b-60732ee14abb\",\"name\":\"LCCN\",\"source\":\"folio\"}";
  private static final String ALTERNATIVE_TITLE_RESPONSE_AS_STRING = "{\"id\":\"5c364ce4-c8fd-4891-a28d-bb91c9bcdbfb\",\"name\":\"Cover title\",\"source\":\"folio\",\"metadata\":{\"createdDate\":\"2020-08-03T02:55:49.659+0000\"}}";
  private static final String IDENTIFIER_JSON_PATH_RESULT = "$.instance[*].identifiers[?(@.identifierTypeId==c858e4f2-2b6b-4385-842b-60732ee14abb)].value";
  private static final String ALTERNATIVE_TITLE_JSON_PATH_RESULT = "$.instance[*].alternativeTitles[?(@.alternativeTitle==Cover title)]";
  private final JsonPathBuilder jsonPathBuilder = new JsonPathBuilder();

  @Test
  void shouldReturnCorrectDisplayNameKey_whenTypeIsInstanceAndIdIsIdentifiers() {
    // given
    TransformationFieldsConfig transformationFieldsConfig = TransformationFieldsConfig.HR_ID;

    // when
    String jsonPath = jsonPathBuilder.build(RecordType.INSTANCE, transformationFieldsConfig);

    // then
    assertEquals(HR_ID_JSON_PATH_RESULT, jsonPath);
  }

  @Test
  void shouldReturnCorrectDisplayNameKey_whenTypeIsInstanceAndIdIsIdentifiers_WithReferenceData_BuiltById() {
    // given
    TransformationFieldsConfig transformationFieldsConfig = TransformationFieldsConfig.IDENTIFIERS;
    Map<String, JsonObject> referenceDataEntry = new HashMap<>();
    referenceDataEntry.put(IDENTIFIER_TYPES_LCCN_ID, new JsonObject(LCCN_RESPONSE_AS_STRING));

    for (Map.Entry<String, JsonObject> refData : referenceDataEntry.entrySet()) {
      // when
      String jsonPath = jsonPathBuilder.build(RecordType.INSTANCE, transformationFieldsConfig, refData);

      // then
      assertNotEquals(transformationFieldsConfig.getPath(), jsonPath);
      assertEquals(IDENTIFIER_JSON_PATH_RESULT, jsonPath);
    }

  }

  @Test
  void shouldReturnCorrectDisplayNameKey_whenTypeIsInstanceAndIdIsAlternativeTitle_WithReferenceData_BuiltByValue() {
    // given
    TransformationFieldsConfig transformationFieldsConfig = TransformationFieldsConfig.ALTERNATIVE_TITLES;
    Map<String, JsonObject> referenceDataEntry = new HashMap<>();
    referenceDataEntry.put(ALTERNATIVE_TITLE_ID, new JsonObject(ALTERNATIVE_TITLE_RESPONSE_AS_STRING));

    for (Map.Entry<String, JsonObject> refData : referenceDataEntry.entrySet()) {
      // when
      String jsonPath = jsonPathBuilder.build(RecordType.INSTANCE, transformationFieldsConfig, refData);

      // then
      assertNotEquals(transformationFieldsConfig.getPath(), jsonPath);
      assertEquals(ALTERNATIVE_TITLE_JSON_PATH_RESULT, jsonPath);
    }

  }

}
