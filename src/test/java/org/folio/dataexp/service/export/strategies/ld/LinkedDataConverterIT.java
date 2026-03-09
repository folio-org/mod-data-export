package org.folio.dataexp.service.export.strategies.ld;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.service.BaseUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class LinkedDataConverterIT extends BaseDataExportInitializerIT {
  private static final String TEST_BASE_URL = "https://folio-test.example.org";

  @Autowired LinkedDataConverter linkedDataConverter;
  @MockitoBean BaseUrlService baseUrlService;

  @BeforeEach
  void setUpBaseUrlClient() {
    when(baseUrlService.getBaseUrl()).thenReturn(TEST_BASE_URL);
  }

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
    assertTrue(resource.contains(TEST_BASE_URL + "/linked-data-editor/resources/12345"));
  }
}
