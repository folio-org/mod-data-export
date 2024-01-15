package org.folio.dataexp.service.export.strategies;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonToMarcConverterTest {


  @Test
  @SneakyThrows
  void convertJsonRecordToMarcRecordTest() {
    var json = """
      {
          "fields": [{
                  "001": "ho00000000009"
              }
          ],
          "leader": "00476cy  a22001574  4500"
      }""";
    var convertor = new JsonToMarcConverter();

    var expected = "00052cy  a22000374  4500001001400000\u001Eho00000000009\u001E\u001D";
    var actual = convertor.convertJsonRecordToMarcRecord(json, new ArrayList<>());
    assertEquals(expected, actual);
  }

}
