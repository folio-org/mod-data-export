package org.folio.service.mapping.processor.translations;

import org.folio.service.mapping.processor.rule.Parameter;

public enum TranslationsHolder implements TranslationFunction {
  SET_VALUE() {
    @Override
    public String apply(String value, Parameter parameters, Settings settings) {
      return parameters.getValue();
    }
  };

  public static TranslationFunction lookup(String function) {
    return valueOf(function.toUpperCase());
  }
}
