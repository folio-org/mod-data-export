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
  protected String encoding = StandardCharsets.UTF_8.name();
  private MarcFactory factory = MarcFactory.newInstance();
  protected Record record = factory.newRecord();

  @Override
  public void writeControlField(RecordControlField recordControlField) {
    ControlField marcControlField = factory.newControlField(recordControlField.getTag(), recordControlField.getData());
    record.addVariableField(marcControlField);
  }

  @Override
  public void writeDataField(RecordDataField recordDataField) {
    DataField marcDataField = factory.newDataField(recordDataField.getTag(), recordDataField.getIndicator1(), recordDataField.getIndicator2());
    for (Map.Entry<Character, String> subField : recordDataField.getSubFields()) {
      Character subFieldCode = subField.getKey();
      String subFieldData = subField.getValue();
      marcDataField.addSubfield(factory.newSubfield(subFieldCode, subFieldData));
    }
    record.addVariableField(marcDataField);
  }

  @Override
  public String getResult() {
    OutputStream outputStream = new ByteArrayOutputStream();
    MarcWriter writer = new MarcStreamWriter(outputStream, encoding);
    writer.write(record);
    writer.close();
    return outputStream.toString();
  }
}
