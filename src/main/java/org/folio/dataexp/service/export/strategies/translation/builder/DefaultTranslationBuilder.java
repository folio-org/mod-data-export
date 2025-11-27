package org.folio.dataexp.service.export.strategies.translation.builder;

import org.folio.dataexp.domain.dto.Transformations;
import org.folio.processor.translations.Translation;

/** Default implementation of TranslationBuilder. */
public class DefaultTranslationBuilder implements TranslationBuilder {

  /**
   * Builds a default translation using the provided function name and mapping transformation.
   *
   * @param functionName the name of the translation function
   * @param mappingTransformation the transformation mapping
   * @return a Translation object
   */
  @Override
  public Translation build(String functionName, Transformations mappingTransformation) {
    Translation translation = new Translation();
    translation.setFunction(functionName);
    return translation;
  }
}
