package org.folio.service.mapping.processor.translations;

import org.folio.service.mapping.processor.RuleProcessor;
import org.folio.service.mapping.processor.rule.Metadata;
import org.folio.service.mapping.referencedata.ReferenceData;

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
   * @param value         value of subfield or indicator
   * @param currentIndex  position of the value in parent list, applicable for composite values and list values
   * @param translation   translation
   * @param referenceData reference data from inventory-storage
   * @return translated result
   */
  String apply(String value, int currentIndex, Translation translation, ReferenceData referenceData, Metadata metadata);
}
