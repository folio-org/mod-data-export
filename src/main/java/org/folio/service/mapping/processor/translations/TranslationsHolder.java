package org.folio.service.mapping.processor.translations;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.service.mapping.processor.rule.Metadata;
import org.folio.service.mapping.referencedata.ReferenceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

public enum TranslationsHolder implements TranslationFunction {

  SET_VALUE() {
    @Override
    public String apply(String value, int currentIndex, Translation translation, ReferenceData referenceData, Metadata metadata) {
      return translation.getParameter("value");
    }
  },
  SET_NATURE_OF_CONTENT_TERM() {
    @Override
    public String apply(String id, int currentIndex, Translation translation, ReferenceData referenceData, Metadata metadata) {
      JsonObject entry = referenceData.getNatureOfContentTerms().get(id);
      if (entry == null) {
        LOGGER.error("Nature of content term is not found by the given id: {}", id);
        return StringUtils.EMPTY;
      } else {
        return entry.getString("name");
      }
    }
  },
  SET_IDENTIFIER() {
    @Override
    public String apply(String identifierValue, int currentIndex, Translation translation, ReferenceData referenceData, Metadata metadata) {
      List<String> identifierTypeIds = (List<String>) metadata.getData().get("identifierTypeId").getData();
      if (!identifierTypeIds.isEmpty()) {
        String identifierTypeId = identifierTypeIds.get(currentIndex);
        JsonObject identifierType = referenceData.getIdentifierTypes().get(identifierTypeId);
        if (identifierType != null && identifierType.getString("name").equals(translation.getParameter("type"))) {
          return identifierValue;
        }
      }
      return StringUtils.EMPTY;
    }
  };

  public static TranslationFunction lookup(String function) {
    return valueOf(function.toUpperCase());
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


}
