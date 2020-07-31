package org.folio.service.fieldname.builder;

import org.folio.rest.jaxrs.model.TransformationField.RecordType;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(MockitoJUnitRunner.class)
class DisplayNameKeyBuilderUnitTest {

  private static final String FIELD_ID_IDENTIFIERS = "identifiers";
  private static final String DISPLAY_NAME_RESULT_FOR_IDENTIFIERS_ID = "instance.identifiers";
  private DisplayNameKeyBuilder displayNameKeyBuilder = new DisplayNameKeyBuilderImpl();

  @Test
  void shouldReturnCorrectDisplayNameKey_whenTypeIsInstanceAndIdIsIdentifiers() {

    // when
    String displayNameKey = displayNameKeyBuilder.build(RecordType.INSTANCE, FIELD_ID_IDENTIFIERS);

    // then
    assertEquals(DISPLAY_NAME_RESULT_FOR_IDENTIFIERS_ID, displayNameKey);
  }

}
