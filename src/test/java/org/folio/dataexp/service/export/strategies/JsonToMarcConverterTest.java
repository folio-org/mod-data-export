package org.folio.dataexp.service.export.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import lombok.SneakyThrows;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.junit.jupiter.api.Test;
import org.marc4j.marc.impl.DataFieldImpl;
import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.marc4j.marc.VariableField;
import java.io.ByteArrayInputStream;
import org.marc4j.MarcStreamReader;

class JsonToMarcConverterTest {

  @Test
  @SneakyThrows
  void convertJsonRecordToMarcRecordTest() {
    var variableField = new DataFieldImpl("tag", 'a', 'b');
    var json =
        """
        {
            "fields": [{
                    "001": "ho00000000009"
                }
            ],
            "leader": "00476cy  a22001574  4500"
        }""";
    var convertor = new JsonToMarcConverter();

    var expected =
        "00067cy  a22000494  4500001001400000tag000300014\u001Eho00000000009\u001Eab\u001E\u001D";
    var actual =
        convertor.convertJsonRecordToMarcRecord(json, List.of(variableField), new MappingProfile());
    assertEquals(expected, actual);
  }

    @Test
  void testConvertJsonRecordToMarcRecordWhenIsUtfFalseShouldApplyAnselEncoding() throws IOException {
    // TestMate-e5e4ccf59ecd034ccf02f869c405a2ad
    // Given
    var jsonToMarcConverter = new JsonToMarcConverter();
    var mappingProfile = new MappingProfile();
    List<VariableField> additionalFields = Collections.emptyList();
    var isUtf = false;
    var jsonRecord = """
        {
          "leader": "00080nam a2200049   4500",
          "fields": [
            {
              "245": {
                "subfields": [
                  {
                    "a": "α"
                  }
                ],
                "ind1": " ",
                "ind2": " "
              }
            }
          ]
        }""";
    // When
    var actualOutputStream = jsonToMarcConverter.convertJsonRecordToMarcRecord(jsonRecord, additionalFields, mappingProfile, isUtf);
    // Then
    var actualBytes = actualOutputStream.toByteArray();
    var utf8Bytes = "α".getBytes(StandardCharsets.UTF_8);
    // MARC-21/MARC-8 escape sequence for Greek is ESC ( g, followed by the character code.
    // ESC = 0x1B (27), '(' = 0x28 (40), 'g' = 0x67 (103)
    byte[] greekEscapeSequence = new byte[]{(byte) 0x1B, (byte) 0x28, (byte) 0x67};
    assertThat(actualBytes).isNotEmpty();
    assertThat(actualBytes).doesNotContain(utf8Bytes);
    // Verify that the converter applied MARC-8 encoding by checking for the Greek escape sequence
    assertThat(actualBytes).containsSequence(greekEscapeSequence);
    // Verify structural integrity (Record Terminator)
    assertThat(actualBytes[actualBytes.length - 1]).isEqualTo((byte) 0x1D);
  }

    @Test
  void testConvertJsonRecordToMarcRecordShouldApplyMappingProfileSuppressionRules() throws IOException {
    // TestMate-12d85d2a1b5416006022e12ee299aed1
    // Given
    var jsonToMarcConverter = new JsonToMarcConverter();
    var mappingProfile = new MappingProfile();
    mappingProfile.setFieldsSuppression("500");
    mappingProfile.setSuppress999ff(true);
    var jsonRecord = """
        {
          "leader": "00135nam a2200073 i 4500",
          "fields": [
            {
              "001": "instance-001"
            },
            {
              "500": {
                "subfields": [
                  {
                    "a": "General note to be suppressed"
                  }
                ],
                "ind1": " ",
                "ind2": " "
              }
            },
            {
              "999": {
                "subfields": [
                  {
                    "s": "System field to be suppressed"
                  }
                ],
                "ind1": "f",
                "ind2": "f"
              }
            }
          ]
        }""";
    List<VariableField> additionalFields = Collections.emptyList();
    var isUtf = true;
    // When
    var actualOutputStream = jsonToMarcConverter.convertJsonRecordToMarcRecord(jsonRecord, additionalFields, mappingProfile, isUtf);
    // Then
    var actualBytes = actualOutputStream.toByteArray();
    assertThat(actualBytes[actualBytes.length - 1]).isEqualTo((byte) 0x1D);
    try (var is = new java.io.ByteArrayInputStream(actualBytes)) {
      var reader = new org.marc4j.MarcStreamReader(is);
      assertThat(reader.hasNext()).isTrue();
      var record = reader.next();
      // Verify 001 exists
      assertThat(record.getControlNumber()).isEqualTo("instance-001");
      // Verify 500 is suppressed
      assertThat(record.getVariableFields("500")).isEmpty();
      // Verify 999 ff is suppressed
      var fields999 = record.getDataFields().stream()
          .filter(f -> "999".equals(f.getTag()))
          .toList();
      boolean has999ff = fields999.stream()
          .anyMatch(f -> f.getIndicator1() == 'f' && f.getIndicator2() == 'f');
      assertThat(has999ff).isFalse();
    }
  }

    @Test
  void testConvertJsonRecordToMarcRecordWhenJsonHasMultipleRecordsShouldProcessAll() throws IOException {
    // TestMate-37ffb05be40b0e42aa4e8477ab7cd738
    // Given
    var jsonToMarcConverter = new JsonToMarcConverter();
    var mappingProfile = new MappingProfile();
    List<VariableField> additionalFields = Collections.emptyList();
    var isUtf = true;
    // MarcJsonReader expects a stream of JSON objects, not a JSON array.
    // Removing the outer brackets and the separating comma to provide sequential JSON objects.
    var jsonRecord = """
        {
          "leader": "00080nam a2200049   4500",
          "fields": [
            {
              "001": "rec-001"
            }
          ]
        }
        {
          "leader": "00080nam a2200049   4500",
          "fields": [
            {
              "001": "rec-002"
            }
          ]
        }""";
    // When
    var actualOutputStream = jsonToMarcConverter.convertJsonRecordToMarcRecord(jsonRecord, additionalFields, mappingProfile, isUtf);
    // Then
    var actualBytes = actualOutputStream.toByteArray();
    assertThat(actualBytes).isNotEmpty();
    assertThat(actualBytes[actualBytes.length - 1]).isEqualTo((byte) 0x1D);
    try (var bais = new ByteArrayInputStream(actualBytes)) {
      var reader = new MarcStreamReader(bais);
      int recordCount = 0;
      while (reader.hasNext()) {
        var record = reader.next();
        recordCount++;
        if (recordCount == 1) {
          assertThat(record.getControlNumber()).isEqualTo("rec-001");
        } else if (recordCount == 2) {
          assertThat(record.getControlNumber()).isEqualTo("rec-002");
        }
      }
      assertThat(recordCount).isEqualTo(2);
    }
  }

    @Test
  void testConvertJsonRecordToMarcRecordWhenAdditionalFieldsEmptyShouldHandleGracefully() throws IOException {
    // TestMate-b53d21e418ef1468b91aba1ded659556
    // Given
    var jsonToMarcConverter = new JsonToMarcConverter();
    var mappingProfile = new MappingProfile();
    List<VariableField> additionalFields = null;
    var isUtf = true;
    var jsonRecord = """
        {
          "leader": "00080nam a2200049   4500",
          "fields": [
            {
              "001": "original-id"
            }
          ]
        }""";
    // When
    var actualOutputStream = jsonToMarcConverter.convertJsonRecordToMarcRecord(jsonRecord, additionalFields, mappingProfile, isUtf);
    // Then
    var actualBytes = actualOutputStream.toByteArray();
    assertThat(actualBytes).isNotEmpty();
    assertThat(actualBytes[actualBytes.length - 1]).isEqualTo((byte) 0x1D);
    try (var bais = new ByteArrayInputStream(actualBytes)) {
      var reader = new MarcStreamReader(bais);
      assertThat(reader.hasNext()).isTrue();
      var record = reader.next();
      assertEquals("original-id", record.getControlNumber());
      assertEquals(1, record.getVariableFields().size());
    }
  }
}
