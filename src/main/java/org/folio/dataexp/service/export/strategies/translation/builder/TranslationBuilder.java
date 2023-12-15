package org.folio.dataexp.service.export.strategies.translation.builder;

import org.folio.dataexp.domain.dto.Transformations;
import org.folio.processor.translations.Translation;

public interface TranslationBuilder {

  Translation build(String functionName, Transformations mappingTransformation);
}
