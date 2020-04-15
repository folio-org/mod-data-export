package org.folio.service.mapping.processor.translations;

import org.folio.service.mapping.processor.rule.Parameter;

@FunctionalInterface
public interface TranslationFunction {
  String apply(String value, Parameter parameters, Settings settings);
}
