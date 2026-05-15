package org.folio.dataexp.service.transformationfields;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.junit.jupiter.api.Test;

class FieldIdBuilderUnitTest {
  private static final String FIELD_ID_CONTROL_NUMBER_RESULT = "instance.identifiers";
  private static final String FIELD_ID_CONTROL_NUMBER_RESULT_WITH_REF_DATA =
      "instance.identifiers.system.control.number";
  private static final String REFERENCE_DATA_CONTROL_NUMBER_NAME = "System control number";
  private static final String FIELD_CONFIG_IDENTIFIER_ID = "identifiers";
  private FieldIdBuilder fieldIdBuilder = new FieldIdBuilder();

  @Test
  void shouldReturnCorrectFieldId_whenBuildWithoutReferenceData() {
    String fieldIdValue = fieldIdBuilder.build(RecordTypes.INSTANCE, FIELD_CONFIG_IDENTIFIER_ID);

    assertEquals(FIELD_ID_CONTROL_NUMBER_RESULT, fieldIdValue);
  }

  @Test
  void shouldReturnCorrectFieldId_whenBuildWithReferenceData() {
    String fieldIdValue =
        fieldIdBuilder.build(
            RecordTypes.INSTANCE, FIELD_CONFIG_IDENTIFIER_ID, REFERENCE_DATA_CONTROL_NUMBER_NAME);

    assertEquals(FIELD_ID_CONTROL_NUMBER_RESULT_WITH_REF_DATA, fieldIdValue);
  }

  @Test
  void shouldReturnCorrectFieldId_whenBuildWithEmptyReferenceData() {
    String fieldIdValue =
        fieldIdBuilder.build(RecordTypes.INSTANCE, FIELD_CONFIG_IDENTIFIER_ID, StringUtils.EMPTY);

    assertEquals(FIELD_ID_CONTROL_NUMBER_RESULT, fieldIdValue);
  }
}
