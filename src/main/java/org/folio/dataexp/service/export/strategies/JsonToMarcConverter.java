package org.folio.dataexp.service.export.strategies;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.marc4j.MarcException;
import org.marc4j.MarcJsonReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.marc.VariableField;
import org.marc4j.marc.Record;
import org.marc4j.marc.impl.SortedMarcFactoryImpl;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Log4j2
@Component
public class JsonToMarcConverter {

  public String convertJsonRecordToMarcRecord(String jsonRecord, List<VariableField> additionalFields, MappingProfile mappingProfile) throws IOException {
    var byteArrayInputStream = new ByteArrayInputStream(jsonRecord.getBytes(StandardCharsets.UTF_8));
    var byteArrayOutputStream = new ByteArrayOutputStream();
    try (byteArrayInputStream; byteArrayOutputStream) {
      var marcJsonReader = new MarcJsonReader(byteArrayInputStream);
      var marcStreamWriter = new MarcStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8.name());
      writeMarc(marcJsonReader, marcStreamWriter, additionalFields, mappingProfile);
      return byteArrayOutputStream.toString();
    } catch (IOException e) {
      log.error(e.getMessage());
      throw e;
    }
  }

  private void writeMarc(MarcJsonReader marcJsonReader, MarcStreamWriter marcStreamWriter, List<VariableField> marcFields, MappingProfile mappingProfile) {
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

  private Record appendAdditionalFields(Record marcRecord, List<VariableField> marcFields) {
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
