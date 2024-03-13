package org.folio.dataexp.service.transformationfields;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.dataexp.domain.dto.RecordTypes;
import org.junit.jupiter.api.Test;

class DisplayNameKeyBuilderUnitTest {

  private static final String FIELD_ID_IDENTIFIERS = "identifiers";
  private static final String DISPLAY_NAME_RESULT_FOR_IDENTIFIERS_ID = "instance.identifiers";
  private final DisplayNameKeyBuilder displayNameKeyBuilder = new DisplayNameKeyBuilder();

  @Test
  void shouldReturnCorrectDisplayNameKey_whenTypeIsInstanceAndIdIsIdentifiers() {
    String displayNameKey = displayNameKeyBuilder.build(RecordTypes.INSTANCE, FIELD_ID_IDENTIFIERS);

    assertEquals(DISPLAY_NAME_RESULT_FOR_IDENTIFIERS_ID, displayNameKey);
  }
}
