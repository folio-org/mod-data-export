package org.folio.service.mapping.translationbuilder;

import org.folio.processor.translations.Translation;
import org.folio.rest.jaxrs.model.Transformations;

public interface TranslationBuilder {

  Translation build(String functionName, Transformations mappingTransformation);
}
