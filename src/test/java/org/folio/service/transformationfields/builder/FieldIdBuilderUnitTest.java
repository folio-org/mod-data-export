package org.folio.service.transformationfields.builder;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.TransformationField.RecordType;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(MockitoJUnitRunner.class)
class FieldIdBuilderUnitTest {

  private static final String FIELD_ID_CONTROL_NUMBER_RESULT = "instance.identifiers";
  private static final String FIELD_ID_CONTROL_NUMBER_RESULT_WITH_REF_DATA = "instance.identifiers.system.control.number";
  private static final String REFERENCE_DATA_CONTROL_NUMBER_NAME = "System control number";
  private static final String FIELD_CONFIG_IDENTIFIER_ID = "identifiers";
  private FieldIdBuilder fieldIdBuilder = new FieldIdBuilderImpl();

  @Test
  void shouldReturnCorrectFieldId_whenBuildWithoutReferenceData() {

    // when
    String fieldIdValue = fieldIdBuilder.build(RecordType.INSTANCE, FIELD_CONFIG_IDENTIFIER_ID);

    // then
    assertEquals(FIELD_ID_CONTROL_NUMBER_RESULT, fieldIdValue);
  }

  @Test
  void shouldReturnCorrectFieldId_whenBuildWithReferenceData() {

    // when
    String fieldIdValue = fieldIdBuilder.build(RecordType.INSTANCE, FIELD_CONFIG_IDENTIFIER_ID, REFERENCE_DATA_CONTROL_NUMBER_NAME);

    // then
    assertEquals(FIELD_ID_CONTROL_NUMBER_RESULT_WITH_REF_DATA, fieldIdValue);
  }

  @Test
  void shouldReturnCorrectFieldId_whenBuildWithEmptyReferenceData() {

    // when
    String fieldIdValue = fieldIdBuilder.build(RecordType.INSTANCE, FIELD_CONFIG_IDENTIFIER_ID, StringUtils.EMPTY);

    // then
    assertEquals(FIELD_ID_CONTROL_NUMBER_RESULT, fieldIdValue);
  }

}
