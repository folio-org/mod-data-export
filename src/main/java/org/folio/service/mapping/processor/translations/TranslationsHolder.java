package org.folio.service.mapping.processor.translations;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

public enum TranslationsHolder implements TranslationFunction {

  SET_VALUE() {
    @Override
    public String apply(String value, JsonObject parameters, Settings settings) {
      return parameters.getString("value");
    }
  },
  SET_NATURE_OF_CONTENT_TERM() {
    @Override
    public String apply(String id, JsonObject parameters, Settings settings) {
      Optional<JsonObject> optionalEntry = settings.getNatureOfContentTerms().stream()
        .filter(entry -> id.equals(entry.getString(ID)))
        .findFirst();
      if (optionalEntry.isPresent()) {
        JsonObject entry = optionalEntry.get();
        return entry.getString("name");
      } else {
        LOGGER.info("Nature of content term is not found by the given id: {}", id);
        return null;
      }
    }
  };

  public static TranslationFunction lookup(String function) {
    return valueOf(function.toUpperCase());
  }

  private static final String ID = "id";
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


}
