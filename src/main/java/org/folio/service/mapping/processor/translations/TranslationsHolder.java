package org.folio.service.mapping.processor.translations;

import io.vertx.core.json.JsonObject;
import org.folio.service.mapping.processor.rule.Translation;

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
