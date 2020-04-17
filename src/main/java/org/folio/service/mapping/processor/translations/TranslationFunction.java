package org.folio.service.mapping.processor.translations;

import io.vertx.core.json.JsonObject;
import org.folio.service.mapping.processor.RuleProcessor;

/**
 * This interface provides a contract to call data translations.
 *
 * @see RuleProcessor
 * @see TranslationsHolder
 */
@FunctionalInterface
public interface TranslationFunction {
  /**
   * Applies data transformation for the given value
   *
   * @param value      value of subfield or indicator
   * @param parameters specific parameters for the function, can be null
   * @param settings   setting from inventory-storage
   * @return translated result
   */
  String apply(String value, JsonObject parameters, Settings settings);
}
