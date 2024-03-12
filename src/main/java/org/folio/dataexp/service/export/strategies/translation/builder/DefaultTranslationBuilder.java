package org.folio.dataexp.service.export.strategies.translation.builder;

import org.folio.dataexp.domain.dto.Transformations;
import org.folio.processor.translations.Translation;

public class DefaultTranslationBuilder implements TranslationBuilder {

  @Override
  public Translation build(String functionName, Transformations mappingTransformation) {
    Translation translation = new Translation();
    translation.setFunction(functionName);
    return translation;
  }
}
