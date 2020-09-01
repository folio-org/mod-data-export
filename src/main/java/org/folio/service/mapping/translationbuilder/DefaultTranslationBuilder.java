package org.folio.service.mapping.translationbuilder;

import org.folio.processor.translations.Translation;
import org.folio.rest.jaxrs.model.Transformations;

public class DefaultTranslationBuilder implements TranslationBuilder {

  @Override
  public Translation build(String functionName, Transformations mappingTransformation) {
    Translation translation = new Translation();
    translation.setFunction(functionName);
    return translation;
  }
}
