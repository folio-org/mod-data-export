package org.folio.service.mapping.writer.impl;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.folio.service.mapping.processor.translations.Translation;
import org.folio.service.mapping.writer.fields.RecordControlField;
import org.folio.service.mapping.writer.fields.RecordDataField;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.*;
import org.marc4j.marc.impl.SortedMarcFactoryImpl;

public class MarcRecordWriter extends AbstractRecordWriter {
  protected String encoding = StandardCharsets.UTF_8.name();
  private MarcFactory factory = new SortedMarcFactoryImpl();
  protected Record record = factory.newRecord();

  @Override
  public void writeLeader(Translation translation) {
    if (translation.getFunction().equals("set_17-19_positions")) {
      char[] implDefined2 = new char[3];
      implDefined2[0] = translation.getParameter("position17").charAt(0);
      implDefined2[1] = translation.getParameter("position18").charAt(0);
      implDefined2[2] = translation.getParameter("position19").charAt(0);
      record.getLeader().setImplDefined2(implDefined2);
    }
  }

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

  @Override
  public List<VariableField> getFields() {
    return record.getVariableFields();
  }
}
