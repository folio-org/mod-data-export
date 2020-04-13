package org.folio.service.mapping.writer.impl;

import org.folio.service.mapping.writer.fields.RecordControlField;
import org.folio.service.mapping.writer.fields.RecordDataField;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MarcRecordWriter extends AbstractRecordWriter {
  protected final String ENCODING = StandardCharsets.UTF_8.name();
  private final MarcFactory FACTORY = MarcFactory.newInstance();
  protected final Record RECORD = FACTORY.newRecord();

  @Override
  public void writeControlField(RecordControlField recordControlField) {
    ControlField marcControlField = FACTORY.newControlField(recordControlField.getTag(), recordControlField.getData());
    RECORD.addVariableField(marcControlField);
  }

  @Override
  public void writeDataField(RecordDataField recordDataField) {
    DataField marcDataField = FACTORY.newDataField(recordDataField.getTag(), recordDataField.getIndicator1(), recordDataField.getIndicator2());
    for (Map.Entry<Character, String> subField : recordDataField.getSubFields()) {
      Character subFieldCode = subField.getKey();
      String subFieldData = subField.getValue();
      marcDataField.addSubfield(FACTORY.newSubfield(subFieldCode, subFieldData));
    }
    RECORD.addVariableField(marcDataField);
  }

  @Override
  public String getResult() {
    OutputStream outputStream = new ByteArrayOutputStream();
    MarcWriter writer = new MarcStreamWriter(outputStream, ENCODING);
    writer.write(RECORD);
    writer.close();
    return outputStream.toString();
  }
}
