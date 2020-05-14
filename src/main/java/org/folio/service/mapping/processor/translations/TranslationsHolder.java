package org.folio.service.mapping.processor.translations;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.service.mapping.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public enum TranslationsHolder implements TranslationFunction {

  SET_VALUE() {
    @Override
    public String apply(String value, Translation translation, Settings settings) {
      return translation.getParameter("value");
    }
  },
  SET_NATURE_OF_CONTENT_TERM() {
    @Override
    public String apply(String id, Translation translation, Settings settings) {
      JsonObject entry = settings.getNatureOfContentTerms().get(id);
      if (entry == null) {
        LOGGER.error("Nature of content term is not found by the given id: {}", id);
        return StringUtils.EMPTY;
      } else {
        return entry.getString("name");
      }
    }
  };

  public static TranslationFunction lookup(String function) {
    return valueOf(function.toUpperCase());
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


}
