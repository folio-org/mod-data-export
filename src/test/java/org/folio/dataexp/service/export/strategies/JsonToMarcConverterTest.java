package org.folio.dataexp.service.export.strategies;

import lombok.SneakyThrows;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.junit.jupiter.api.Test;
import org.marc4j.marc.impl.DataFieldImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonToMarcConverterTest {


  @Test
  @SneakyThrows
  void convertJsonRecordToMarcRecordTest() {
    var variableField = new DataFieldImpl("tag", 'a', 'b');
    var json = """
      {
          "fields": [{
                  "001": "ho00000000009"
              }
          ],
          "leader": "00476cy  a22001574  4500"
      }""";
    var convertor = new JsonToMarcConverter();

    var expected = "00067cy  a22000494  4500001001400000tag000300014\u001Eho00000000009\u001Eab\u001E\u001D";
    var actual = convertor.convertJsonRecordToMarcRecord(json, List.of(variableField), new MappingProfile());
    assertEquals(expected, actual);
  }

}
