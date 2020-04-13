package org.folio.service.mapping.processor.translations;

import io.vertx.core.json.JsonObject;
import org.folio.service.mapping.processor.rule.Translation;

public enum TranslationsHolder implements TranslationFunction {
  STUB_FUNCTION() {
    @Override
    public String apply(String value, JsonObject parameters, Settings settings) {
      return value;
    }
  };

  public static TranslationFunction lookup(Translation translation) {
    return valueOf(translation.getFunction().toUpperCase());
  }
}
