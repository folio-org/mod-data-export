package org.folio.service.fieldname.builder;

import org.folio.rest.jaxrs.model.FieldName;
import org.folio.service.fieldname.FieldNameConfig;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


@RunWith(MockitoJUnitRunner.class)
class JsonPathBuilderUnitTest {

  private static final String HR_ID_JSON_PATH_RESULT = "$.instance.hrid";
  private static final String IDENTIFIER_TYPES_LCCN_ID = "c858e4f2-2b6b-4385-842b-60732ee14abb";
  private static final String IDENTIFIER_JSON_PATH_RESULT = "$.instance[*].identifiers[?(@identifierTypeId==c858e4f2-2b6b-4385-842b-60732ee14abb)].value";
  private JsonPathBuilder jsonPathBuilder = new JsonPathBuilder();

  @Test
  void shouldReturnCorrectDisplayNameKey_whenTypeIsInstanceAndIdIsIdentifiers() {
    // given
    FieldNameConfig fieldNameConfig = FieldNameConfig.HR_ID;

    // when
    String jsonPath = jsonPathBuilder.build(FieldName.RecordType.INSTANCE, fieldNameConfig);

    // then
    assertEquals(HR_ID_JSON_PATH_RESULT, jsonPath);
  }

  @Test
  void shouldReturnCorrectDisplayNameKey_whenTypeIsInstanceAndIdIsIdentifiers_WithReferenceData() {
    // given
    FieldNameConfig fieldNameConfig = FieldNameConfig.IDENTIFIERS;

    // when
    String jsonPath = jsonPathBuilder.build(FieldName.RecordType.INSTANCE, fieldNameConfig, IDENTIFIER_TYPES_LCCN_ID);

    // then
    assertNotEquals(fieldNameConfig.getPath(), jsonPath);
    assertEquals(IDENTIFIER_JSON_PATH_RESULT, jsonPath);
  }

}
