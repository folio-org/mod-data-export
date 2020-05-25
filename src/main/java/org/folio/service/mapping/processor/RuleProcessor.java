package org.folio.service.mapping.processor;

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
import org.folio.service.mapping.settings.Settings;
import org.folio.service.mapping.writer.RecordWriter;

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

  public String process(EntityReader reader, RecordWriter writer, Settings settings, List<Rule> rules) {
    rules.forEach(rule -> {
      if (LEADER_FIELD.equals(rule.getField())) {
        rule.getDataSources().forEach(dataSource -> writer.writeLeader(dataSource.getTranslation()));
      } else {
        RuleValue ruleValue = reader.read(rule);
        switch (ruleValue.getType()) {
          case SIMPLE:
            SimpleValue simpleValue = (SimpleValue) ruleValue;
            translate(simpleValue, settings);
            writer.writeField(rule.getField(), simpleValue);
            break;
          case COMPOSITE:
            CompositeValue compositeValue = (CompositeValue) ruleValue;
            translate(compositeValue, settings);
            writer.writeField(rule.getField(), compositeValue);
            break;
          case MISSING:
        }
      }
    });
    return writer.getResult();
  }

  private void translate(SimpleValue simpleValue, Settings settings) {
    Translation translation = simpleValue.getDataSource().getTranslation();
    if (translation != null) {
      TranslationFunction translationFunction = TranslationsHolder.lookup(translation.getFunction());
      if (STRING.equals(simpleValue.getSubType())) {
        StringValue stringValue = (StringValue) simpleValue;
        String readValue = stringValue.getValue();
        String translatedValue = translationFunction.apply(readValue, translation, settings);
        stringValue.setValue(translatedValue);
      } else if (LIST_OF_STRING.equals(simpleValue.getSubType())) {
        ListValue listValue = (ListValue) simpleValue;
        List<String> translatedValues = new ArrayList<>();
        for (String readValue : listValue.getValue()) {
          String translatedValue = translationFunction.apply(readValue, translation, settings);
          translatedValues.add(translatedValue);
        }
        listValue.setValue(translatedValues);
      }
    }
  }

  private void translate(CompositeValue compositeValue, Settings settings) {
    List<List<StringValue>> readValues = compositeValue.getValue();
    for (List<StringValue> readEntry : readValues) {
      readEntry.forEach(stringValue -> {
        Translation translation = stringValue.getDataSource().getTranslation();
        if (translation != null) {
          TranslationFunction translationFunction = TranslationsHolder.lookup(translation.getFunction());
          String readValue = stringValue.getValue();
          String translatedValue = translationFunction.apply(readValue, translation, settings);
          stringValue.setValue(translatedValue);
        }
      });
    }
  }
}
