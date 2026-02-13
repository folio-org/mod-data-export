package org.folio.dataexp.service.export.strategies.translation.builder;

import org.folio.dataexp.domain.dto.Transformations;
import org.folio.processor.translations.Translation;

/** Interface for building translation objects. */
public interface TranslationBuilder {

  /**
   * Builds a translation using the provided function name and mapping transformation.
   *
   * @param functionName the name of the translation function
   * @param mappingTransformation the transformation mapping
   * @return a Translation object
   */
  Translation build(String functionName, Transformations mappingTransformation);
}
