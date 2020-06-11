package org.folio.service.mapping.processor;

import org.folio.service.mapping.processor.rule.Metadata;
import org.folio.service.mapping.processor.rule.Rule;
import org.folio.service.mapping.processor.translations.Translation;
import org.folio.service.mapping.processor.translations.TranslationFunction;
import org.folio.service.mapping.processor.translations.TranslationsHolder;
import org.folio.service.mapping.reader.EntityReader;
import org.folio.service.mapping.reader.values.CompositeValue;
import org.folio.service.mapping.reader.values.ListValue;
import org.folio.service.mapping.reader.values.RuleValue;
import org.folio.service.mapping.reader.values.SimpleValue;
import org.folio.service.mapping.reader.values.StringValue;
import org.folio.service.mapping.referencedata.ReferenceData;
import org.folio.service.mapping.writer.RecordWriter;
import org.marc4j.marc.VariableField;
import java.util.ArrayList;
import java.util.List;

import static org.folio.service.mapping.reader.values.SimpleValue.SubType.LIST_OF_STRING;
import static org.folio.service.mapping.reader.values.SimpleValue.SubType.STRING;

/**
 * RuleProcessor is a central part of mapping.
 * <p>
 * High-level algorithm:
 * # read data by the given rule
 * # translate data
 * # write data
 *
 * @see EntityReader
 * @see TranslationFunction
 * @see RecordWriter
 */
public final class RuleProcessor {
  private static final String LEADER_FIELD = "leader";

  public List<VariableField> processFields(EntityReader reader, RecordWriter writer, ReferenceData referenceData, List<Rule> rules) {
    rules.forEach(rule -> {
        if (LEADER_FIELD.equals(rule.getField())) {
          rule.getDataSources().forEach(dataSource -> writer.writeLeader(dataSource.getTranslation()));
        } else {
          processRule(reader, writer, referenceData, rule);
        }});
    return writer.getFields();
  }

  private void processRule(EntityReader reader, RecordWriter writer,ReferenceData referenceData, Rule rule) {
    RuleValue ruleValue = reader.read(rule);
    switch (ruleValue.getType()) {
      case SIMPLE:
        SimpleValue simpleValue = (SimpleValue) ruleValue;
        translate(simpleValue, referenceData, rule.getMetadata());
        writer.writeField(rule.getField(), simpleValue);
        break;
      case COMPOSITE:
        CompositeValue compositeValue = (CompositeValue) ruleValue;
        translate(compositeValue, referenceData, rule.getMetadata());
        writer.writeField(rule.getField(), compositeValue);
        break;
      case MISSING:
    }
  }

  public String process(EntityReader reader, RecordWriter writer, ReferenceData referenceData, List<Rule> rules) {
    rules.forEach(rule -> {
      if (LEADER_FIELD.equals(rule.getField())) {
        rule.getDataSources().forEach(dataSource -> writer.writeLeader(dataSource.getTranslation()));
      } else {
        processRule(reader, writer, referenceData, rule);
      }
    });
    return writer.getResult();
  }

  private <S extends SimpleValue> void translate(S simpleValue, ReferenceData referenceData, Metadata metadata) {
    Translation translation = simpleValue.getDataSource().getTranslation();
    if (translation != null) {
      TranslationFunction translationFunction = TranslationsHolder.lookup(translation.getFunction());
      if (STRING.equals(simpleValue.getSubType())) {
        StringValue stringValue = (StringValue) simpleValue;
        String readValue = stringValue.getValue();
        String translatedValue = translationFunction.apply(readValue, 0, translation, referenceData, metadata);
        stringValue.setValue(translatedValue);
      } else if (LIST_OF_STRING.equals(simpleValue.getSubType())) {
        ListValue listValue = (ListValue) simpleValue;
        List<String> translatedValues = new ArrayList<>();
        for (int currentIndex = 0; currentIndex < listValue.getValue().size(); currentIndex++) {
          String readValue = listValue.getValue().get(currentIndex);
          String translatedValue = translationFunction.apply(readValue, currentIndex, translation, referenceData, metadata);
          translatedValues.add(translatedValue);
        }
        listValue.setValue(translatedValues);
      }
    }
  }

  private void translate(CompositeValue compositeValue, ReferenceData referenceData, Metadata metadata) {
    List<List<StringValue>> readValues = compositeValue.getValue();
    for (int currentIndex = 0; currentIndex < readValues.size(); currentIndex++) {
      List<StringValue> readEntry = readValues.get(currentIndex);
      for (StringValue stringValue : readEntry) {
        Translation translation = stringValue.getDataSource().getTranslation();
        if (translation != null) {
          TranslationFunction translationFunction = TranslationsHolder.lookup(translation.getFunction());
          String readValue = stringValue.getValue();
          String translatedValue = translationFunction.apply(readValue, currentIndex, translation, referenceData, metadata);
          stringValue.setValue(translatedValue);
        }
      }
    }
  }
}
