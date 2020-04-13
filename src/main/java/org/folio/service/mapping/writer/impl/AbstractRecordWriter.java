package org.folio.service.mapping.writer.impl;

import org.folio.service.mapping.processor.rule.DataSource;
import org.folio.service.mapping.reader.values.CompositeValue;
import org.folio.service.mapping.reader.values.ListValue;
import org.folio.service.mapping.reader.values.SimpleValue;
import org.folio.service.mapping.reader.values.StringValue;
import org.folio.service.mapping.writer.RecordWriter;
import org.folio.service.mapping.writer.fields.RecordControlField;
import org.folio.service.mapping.writer.fields.RecordDataField;

import java.util.Collections;
import java.util.List;

import static org.folio.service.mapping.reader.values.SimpleValue.SubType.LIST_OF_STRING;
import static org.folio.service.mapping.reader.values.SimpleValue.SubType.STRING;

public abstract class AbstractRecordWriter implements RecordWriter {

  @Override
  public void write(SimpleValue simpleValue) {
    DataSource dataSource = simpleValue.getDataSource();
    String tag = dataSource.getTag();
    if (STRING.equals(simpleValue.getSubType())) {
      StringValue stringValue = (StringValue) simpleValue;
      if (dataSource.isSubFieldSource() || dataSource.isIndicatorSource()) {
        RecordDataField recordDataField = buildDataFieldForStringValues(tag, Collections.singletonList(stringValue));
        writeDataField(recordDataField);
      } else {
        RecordControlField recordControlField = new RecordControlField(tag, stringValue.getValue());
        writeControlField(recordControlField);
      }
    } else if (LIST_OF_STRING.equals(simpleValue.getSubType())) {
      ListValue listValue = (ListValue) simpleValue;
      if (dataSource.isSubFieldSource() || dataSource.isIndicatorSource()) {
        RecordDataField recordDataField = buildDataFieldForListOfStrings(listValue);
        writeDataField(recordDataField);
      } else {
        for (String value : listValue.getValue()) {
          RecordControlField recordControlField = new RecordControlField(tag, value);
          writeControlField(recordControlField);
        }
      }
    }
  }

  @Override
  public void write(CompositeValue compositeValue) {
    String tag = compositeValue.getValue().get(0).get(0).getDataSource().getTag();
    for (List<StringValue> entry : compositeValue.getValue()) {
      RecordDataField recordDataField = buildDataFieldForStringValues(tag, entry);
      writeDataField(recordDataField);
    }
  }

  protected abstract void writeControlField(RecordControlField recordControlField);

  protected abstract void writeDataField(RecordDataField recordDataField);

  private RecordDataField buildDataFieldForListOfStrings(ListValue listValue) {
    DataSource dataSource = listValue.getDataSource();
    String tag = listValue.getDataSource().getTag();
    RecordDataField field = new RecordDataField(tag);
    for (String stringValue : listValue.getValue()) {
      if (listValue.getDataSource().isSubFieldSource()) {
        char subFieldCode = dataSource.getSubfield().charAt(0);
        String subFieldData = stringValue;
        field.addSubField(subFieldCode, subFieldData);
      } else if (dataSource.isIndicatorSource()) {
        char indicator = stringValue.charAt(0);
        if ("1".equals(dataSource.getIndicator())) {
          field.setIndicator1(indicator);
        } else if ("2".equals(dataSource.getIndicator())) {
          field.setIndicator2(indicator);
        }
      }
    }
    return field;
  }

  private RecordDataField buildDataFieldForStringValues(String tag, List<StringValue> entry) {
    RecordDataField field = new RecordDataField(tag);
    for (StringValue stringValue : entry) {
      DataSource dataSource = stringValue.getDataSource();
      if (dataSource.isSubFieldSource()) {
        char subFieldCode = dataSource.getSubfield().charAt(0);
        String subFieldData = stringValue.getValue();
        if (subFieldData != null) {
          field.addSubField(subFieldCode, subFieldData);
        }
      } else if (dataSource.isIndicatorSource()) {
        char indicator = stringValue.getValue().charAt(0);
        if ("1".equals(dataSource.getIndicator())) {
          field.setIndicator1(indicator);
        } else if ("2".equals(dataSource.getIndicator())) {
          field.setIndicator2(indicator);
        }
      }
    }
    return field;
  }

}
