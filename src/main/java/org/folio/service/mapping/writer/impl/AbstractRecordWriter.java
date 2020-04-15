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
  private static final String INDICATOR_1 = "1";
  private static final String INDICATOR_2 = "2";

  @Override
  public void write(String tag, SimpleValue simpleValue) {
    DataSource dataSource = simpleValue.getDataSource();
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
        RecordDataField recordDataField = buildDataFieldForListOfStrings(tag, listValue);
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
  public void write(String tag, CompositeValue compositeValue) {
    for (List<StringValue> entry : compositeValue.getValue()) {
      RecordDataField recordDataField = buildDataFieldForStringValues(tag, entry);
      writeDataField(recordDataField);
    }
  }

  protected abstract void writeControlField(RecordControlField recordControlField);

  protected abstract void writeDataField(RecordDataField recordDataField);

  private RecordDataField buildDataFieldForListOfStrings(String tag, ListValue listValue) {
    DataSource dataSource = listValue.getDataSource();
    RecordDataField field = new RecordDataField(tag);
    for (String stringValue : listValue.getValue()) {
      if (listValue.getDataSource().isSubFieldSource()) {
        char subFieldCode = dataSource.getSubfield().charAt(0);
        String subFieldData = stringValue;
        field.addSubField(subFieldCode, subFieldData);
      } else if (dataSource.isIndicatorSource()) {
        char indicator = stringValue.charAt(0);
        if (INDICATOR_1.equals(dataSource.getIndicator())) {
          field.setIndicator1(indicator);
        } else if (INDICATOR_2.equals(dataSource.getIndicator())) {
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
        if (INDICATOR_1.equals(dataSource.getIndicator())) {
          field.setIndicator1(indicator);
        } else if (INDICATOR_2.equals(dataSource.getIndicator())) {
          field.setIndicator2(indicator);
        }
      }
    }
    return field;
  }

}
