package org.folio.service.mapping.processor.translations;

import org.folio.service.mapping.processor.RuleProcessor;
import org.folio.service.mapping.settings.Settings;

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
   * @param translation translation
   * @param settings   setting from inventory-storage
   * @return translated result
   */
  String apply(String value, Translation translation, Settings settings);
}
