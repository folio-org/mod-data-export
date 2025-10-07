package org.folio.dataexp.service.export.strategies.ld;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.folio.dataexp.BaseDataExportInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LinkedDataConverterTest extends BaseDataExportInitializer {
  @Autowired
  LinkedDataConverter linkedDataConverter;

  // Rather than test conversion accuracy, just make sure the
  // wiring is correct given faulty input, as opposed to failing
  // to run at all due to miswiring.
  @Test
  void convertLdJsonToBibframe2RdfExceptionTest() {
    assertThrows(JsonProcessingException.class, () ->
        linkedDataConverter.convertLdJsonToBibframe2Rdf("{"));
  }
}
