package org.folio.service.mapping.processor.translations;

import io.vertx.core.json.JsonObject;

public enum TranslationsHolder implements TranslationFunction {
  SET_VALUE() {
    @Override
    public String apply(String value, JsonObject parameters, Settings settings) {
      return parameters.getString("value");
    }
  };

  public static TranslationFunction lookup(String function) {
    return valueOf(function.toUpperCase());
  }
}
