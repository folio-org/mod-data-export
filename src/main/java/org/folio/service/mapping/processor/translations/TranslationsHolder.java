package org.folio.service.mapping.processor.translations;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.service.mapping.processor.rule.Metadata;
import org.folio.service.mapping.referencedata.ReferenceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
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
      Object metadataIdentifierTypeIds = metadata.getData().get("identifierTypeId").getData();
      if (metadataIdentifierTypeIds != null) {
        List<String> identifierTypeIds = (List<String>) metadataIdentifierTypeIds;
        if (!identifierTypeIds.isEmpty()) {
          String identifierTypeId = identifierTypeIds.get(currentIndex);
          JsonObject identifierType = referenceData.getIdentifierTypes().get(identifierTypeId);
          if (identifierType != null && identifierType.getString("name").equals(translation.getParameter("type"))) {
            return identifierValue;
          }
        }
      }
      return StringUtils.EMPTY;
    }
  },

  /**
   * Sixteen characters that indicate the date and time of the latest record transaction
   * and serve as a version identifier for the record.
   * They are recorded according to Representation of Dates and Times (ISO 8601).
   * The date requires 8 numeric characters in the pattern yyyymmdd.
   * The time requires 8 numeric characters in the pattern hhmmss.f, expressed in terms of the 24-hour (00-23) clock.
   */
  SET_TRANSACTION_DATETIME() {
    private final DateTimeFormatter ORIGIN_FORMATTER = new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
      .toFormatter();
    private final DateTimeFormatter TARGET_FORMATTER = new DateTimeFormatterBuilder()
      .appendPattern("yyyyMMddhhmmss")
      .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 1, true)
      .toFormatter();

    @Override
    public String apply(String updatedDate, int currentIndex, Translation translation, ReferenceData referenceData, Metadata metadata) {
      ZonedDateTime originDateTime = ZonedDateTime.parse(updatedDate, ORIGIN_FORMATTER);
      return TARGET_FORMATTER.format(originDateTime);
    }
  };

  public static TranslationFunction lookup(String function) {
    return valueOf(function.toUpperCase());
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


}
