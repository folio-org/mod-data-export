package org.folio.dataexp.service.export.strategies;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.marc4j.MarcException;
import org.marc4j.MarcJsonReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.converter.impl.UnicodeToAnsel;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;
import org.marc4j.marc.impl.SortedMarcFactoryImpl;
import org.springframework.stereotype.Component;

/**
 * Converter for transforming JSON records to MARC records.
 */
@Log4j2
@Component
public class JsonToMarcConverter {

  /**
   * Converts a JSON record to a MARC record string.
   */
  public String convertJsonRecordToMarcRecord(
      String jsonRecord,
      List<VariableField> additionalFields,
      MappingProfile mappingProfile
  ) throws IOException {
    return convertJsonRecordToMarcRecord(
        jsonRecord,
        additionalFields,
        mappingProfile,
        true
    ).toString();
  }

  /**
   * Converts a JSON record to a MARC record as a ByteArrayOutputStream.
   */
  public ByteArrayOutputStream convertJsonRecordToMarcRecord(
      String jsonRecord,
      List<VariableField> additionalFields,
      MappingProfile mappingProfile,
      boolean isUtf
  ) throws IOException {
    var byteArrayInputStream =
        new ByteArrayInputStream(jsonRecord.getBytes(StandardCharsets.UTF_8));
    var byteArrayOutputStream = new ByteArrayOutputStream();
    try (byteArrayInputStream; byteArrayOutputStream) {
      var marcJsonReader = new MarcJsonReader(byteArrayInputStream);
      var marcStreamWriter =
          new MarcStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8.name());
      if (!isUtf) {
        marcStreamWriter.setConverter(new UnicodeToAnsel());
      }
      writeMarc(
          marcJsonReader,
          marcStreamWriter,
          additionalFields,
          mappingProfile
      );
      return byteArrayOutputStream;
    } catch (IOException e) {
      log.error(e.getMessage());
      throw e;
    }
  }

  private void writeMarc(
      MarcJsonReader marcJsonReader,
      MarcStreamWriter marcStreamWriter,
      List<VariableField> marcFields,
      MappingProfile mappingProfile
  ) {
    var suppressProcessor = new MarcSuppressProcessor(mappingProfile);
    try {
      while (marcJsonReader.hasNext()) {
        var marc = marcJsonReader.next();
        if (CollectionUtils.isNotEmpty(marcFields)) {
          marc = appendAdditionalFields(marc, marcFields);
        }
        suppressProcessor.suppress(marc);
        marcStreamWriter.write(marc);
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new MarcException(e.getMessage());
    }
  }

  private Record appendAdditionalFields(
      Record marcRecord,
      List<VariableField> marcFields
  ) {
    SortedMarcFactoryImpl sortedMarcFactory = new SortedMarcFactoryImpl();
    var sortedRecord = sortedMarcFactory.newRecord();
    sortedRecord.setLeader(marcRecord.getLeader());
    for (VariableField recordField : marcRecord.getVariableFields()) {
      sortedRecord.addVariableField(recordField);
    }
    for (VariableField generatedField : marcFields) {
      sortedRecord.addVariableField(generatedField);
    }
    return sortedRecord;
  }
}
