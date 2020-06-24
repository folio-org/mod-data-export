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
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

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
  SET_CONTRIBUTOR() {
    @Override
    public String apply(String identifierValue, int currentIndex, Translation translation, ReferenceData referenceData, Metadata metadata) {
      Object metadataContributorNameTypeIds = metadata.getData().get("contributorNameTypeId").getData();
      if (metadataContributorNameTypeIds != null) {
        List<String> contributorNameTypeIds = (List<String>) metadataContributorNameTypeIds;
        if (!contributorNameTypeIds.isEmpty()) {
          String contributorNameTypeId = contributorNameTypeIds.get(currentIndex);
          JsonObject contributorNameType = referenceData.getContributorNameTypes().get(contributorNameTypeId);
          if (contributorNameType != null && contributorNameType.getString("name").equals(translation.getParameter("type"))) {
            return identifierValue;
          }
        }
      }
      return StringUtils.EMPTY;
    }
  },
  SET_LOCATION() {
    @Override
    public String apply(String locationId, int currentIndex, Translation translation, ReferenceData referenceData, Metadata metadata) {
      JsonObject entry = referenceData.getLocations().get(locationId);
      if (entry == null) {
        LOGGER.error("Location is not found by the given id: {}", locationId);
        return StringUtils.EMPTY;
      } else {
        return entry.getString("name");
      }
    }
  },
  SET_MATERIAL_TYPE() {
    @Override
    public String apply(String materialTypeId, int currentIndex, Translation translation, ReferenceData referenceData, Metadata metadata) {
      JsonObject entry = referenceData.getMaterialTypes().get(materialTypeId);
      if (entry == null) {
        LOGGER.error("Material type is not found by the given id: {}", materialTypeId);
        return StringUtils.EMPTY;
      } else {
        return entry.getString("name");
      }
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
    private transient DateTimeFormatter originFormatter = new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
      .toFormatter();
    private transient DateTimeFormatter targetFormatter = new DateTimeFormatterBuilder()
      .appendPattern("yyyyMMddhhmmss")
      .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 1, true)
      .toFormatter();

    @Override
    public String apply(String updatedDate, int currentIndex, Translation translation, ReferenceData referenceData, Metadata metadata) {
      ZonedDateTime originDateTime = ZonedDateTime.parse(updatedDate, originFormatter);
      return targetFormatter.format(originDateTime);
    }
  },

  /**
   * Forty character positions (00-39) that provide coded information about the record as a whole and about special
   * bibliographic aspects of the item being cataloged.
   * These coded data elements are potentially useful for retrieval and data management purposes.
   * Format:
   * 00-05 - Metadata.createdDate field in yymmdd format
   * 06 is set to | (pipe character)
   * 07-10 to publication[0] dateOfPublication if can be formatted else |||| (four pipe characters)
   * 11-14 to publication[1] dateOfPublication if can be formatted else |||| (four pipe characters)
   * 18-22 - each field set to |
   * 23-29 - each field to be blank
   * 30-34 - each field set to |
   * 35-37 - if languages array is empty set it to "und",
   * if one element, use it to populate the field (it should be 3 letter language code),
   * if the array contains more than one language, then set it to "mul"
   * 38-39 - each field set to |
   */
  SET_FIXED_LENGTH_DATA_ELEMENTS() {
    private transient DateTimeFormatter originCreatedDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private transient DateTimeFormatter targetCreatedDateFormatter = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String DATES_OF_PUBLICATION = "datesOfPublication";
    private static final String LANGUAGES = "languages";
    private static final String FIELD_PATTERN = "%s|%s%s||||||||       |||||%s||";

    @Override
    public String apply(String originCreatedDate, int currentIndex, Translation translation, ReferenceData referenceData, Metadata metadata) {
      String createdDateParam;
      if (isNotEmpty(originCreatedDate)) {
        try {
          createdDateParam = targetCreatedDateFormatter.format(ZonedDateTime.parse(originCreatedDate, originCreatedDateFormatter));
        } catch (DateTimeParseException e) {
          LOGGER.error("Failed to parse createdDate field, the current time value will be used");
          createdDateParam = targetCreatedDateFormatter.format(ZonedDateTime.now());
        }
      } else {
        createdDateParam = targetCreatedDateFormatter.format(ZonedDateTime.now());
      }

      String publicationDate0Param = "||||";
      String publicationDate1Param = "||||";
      if (metadata != null && metadata.getData().containsKey(DATES_OF_PUBLICATION)
        && metadata.getData().get(DATES_OF_PUBLICATION) != null) {
        List<String> publicationDates = (List<String>) metadata.getData().get(DATES_OF_PUBLICATION).getData();
        if (publicationDates.size() == 1 && isNotEmpty(publicationDates.get(0)) && publicationDates.get(0).length() == 4) {
          publicationDate0Param = publicationDates.get(0);
        } else if (publicationDates.size() > 1) {
          String publicationDate0 = publicationDates.get(0);
          if (isNotEmpty(publicationDate0) && publicationDate0.length() == 4) {
            publicationDate0Param = publicationDate0;
          }
          String publicationDate1 = publicationDates.get(1);
          if (isNotEmpty(publicationDate1) && publicationDate1.length() == 4) {
            publicationDate1Param = publicationDate1;
          }
        }
      }

      String languageParam = "und";
      if (metadata != null && metadata.getData().containsKey(LANGUAGES)
        && metadata.getData().get(LANGUAGES) != null) {
        List<String> languages = (List<String>) metadata.getData().get(LANGUAGES).getData();
        if (languages.size() == 1 && isNotEmpty(languages.get(0))) {
          languageParam = languages.get(0);
        } else if (languages.size() > 1) {
          languageParam = "mul";
        }
      }
      return format(FIELD_PATTERN, createdDateParam, publicationDate0Param, publicationDate1Param, languageParam);
    }
  },

  SET_INSTANCE_FORMAT_ID() {
    private static final String REGEX = "--";

    @Override
    public String apply(String instanceFormatId, int currentIndex, Translation translation, ReferenceData referenceData, Metadata metadata) {
      JsonObject entry = referenceData.getInstanceFormats().get(instanceFormatId);
      if (entry == null) {
        LOGGER.error("Instance format is not found by the given id: {}", instanceFormatId);
        return StringUtils.EMPTY;
      } else {
        String instanceFormatIdValue = entry.getString("name");
        List<String> instanceFormatsResult = Arrays.asList(instanceFormatIdValue.split(REGEX));
        if (translation.getParameter("value").equals("0") && isNotBlank(instanceFormatsResult.get(0))) {
          return Arrays.asList(instanceFormatIdValue.split(REGEX)).get(0).trim();
        } else if (translation.getParameter("value").equals("1")) {
          return Arrays.asList(instanceFormatIdValue.split(REGEX)).get(1).trim();
        } else {
          return StringUtils.EMPTY;
        }
      }
    }
  };

  public static TranslationFunction lookup(String function) {
    return valueOf(function.toUpperCase());
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


}
