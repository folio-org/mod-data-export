package org.folio.dataexp.service.export.strategies.ld;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LinkedDataConverterTest extends BaseDataExportInitializer {
  @Autowired LinkedDataConverter linkedDataConverter;

  // Rather than test conversion accuracy, just make sure the
  // wiring is correct given faulty input, as opposed to failing
  // to run at all due to miswiring.
  @Test
  void convertLdJsonToBibframe2RdfExceptionTest() {
    assertThrows(
        JsonProcessingException.class, () -> linkedDataConverter.convertLdJsonToBibframe2Rdf("{"));
  }

  @SneakyThrows
  @Test
  void convertLdJsonToBibframe2RdfHostnameTest() {
    var resource =
        linkedDataConverter
            .convertLdJsonToBibframe2Rdf(
                """
      {
        "id": "12345",
        "types": [
          "http://bibfra.me/vocab/lite/Instance"
        ]
      }""")
            .toString();
    System.out.println(resource);
    assertTrue(resource.contains("http://localhost/linked-data-editor/resources/12345"));
  }
}
