package org.folio.dataexp.service.transformationfields;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.processor.referencedata.JsonObjectWrapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class JsonPathBuilderUnitTest {

  private static final String HR_ID_JSON_PATH_RESULT = "$.instance.hrid";
  private static final String IDENTIFIER_TYPES_LCCN_ID = "c858e4f2-2b6b-4385-842b-60732ee14abb";
  private static final String LCCN_RESPONSE_AS_STRING = "{\"id\":\"c858e4f2-2b6b-4385-842b-60732ee14abb\",\"name\":\"LCCN\",\"source\":\"folio\"}";
  private static final String IDENTIFIER_JSON_PATH_RESULT = "$.instance.identifiers[?(@.identifierTypeId=='c858e4f2-2b6b-4385-842b-60732ee14abb')].value";
  private final JsonPathBuilder jsonPathBuilder = new JsonPathBuilder();
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void shouldReturnCorrectDisplayNameKey_whenTypeIsInstanceAndIdIsIdentifiers() {
    TransformationFieldsConfig transformationFieldsConfig = TransformationFieldsConfig.HR_ID;

    String jsonPath = jsonPathBuilder.build(RecordTypes.INSTANCE, transformationFieldsConfig);

    assertEquals(HR_ID_JSON_PATH_RESULT, jsonPath);
  }

  @Test
  @SneakyThrows
  void shouldReturnCorrectDisplayNameKey_whenTypeIsInstanceAndIdIsIdentifiers_WithReferenceData_BuiltById() {
    TransformationFieldsConfig transformationFieldsConfig = TransformationFieldsConfig.IDENTIFIERS;
    Map<String, JsonObjectWrapper> referenceDataEntry = new HashMap<>();
    var value = new JsonObjectWrapper(mapper.readValue(LCCN_RESPONSE_AS_STRING, new TypeReference<>() {}));
    referenceDataEntry.put(IDENTIFIER_TYPES_LCCN_ID, value);

    for (Map.Entry<String, JsonObjectWrapper> refData : referenceDataEntry.entrySet()) {
      String jsonPath = jsonPathBuilder.build(RecordTypes.INSTANCE, transformationFieldsConfig, refData);

      assertNotEquals(transformationFieldsConfig.getPath(), jsonPath);
      assertEquals(IDENTIFIER_JSON_PATH_RESULT, jsonPath);
    }
  }
}
