package org.folio.dataexp.service.transformationfields;

import static java.lang.String.join;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Arrays;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.springframework.stereotype.Component;

/**
 * Builder for field IDs for transformation fields.
 */
@Component
public class FieldIdBuilder {

  /** Delimiter for field IDs. */
  public static final String DOT_DELIMITER = ".";

  /**
   * Builds a field ID from record type and field config ID.
   *
   * @param recordType the record type
   * @param fieldConfigId the field config ID
   * @return the field ID
   */
  public String build(RecordTypes recordType, String fieldConfigId) {
    return join(DOT_DELIMITER, recordType.toString().toLowerCase(),
        getFormattedName(fieldConfigId));
  }

  /**
   * Builds a field ID from record type, field config ID, and reference data name.
   *
   * @param recordType the record type
   * @param fieldConfigId the field config ID
   * @param referenceDataName the reference data name
   * @return the field ID
   */
  public String build(RecordTypes recordType, String fieldConfigId, String referenceDataName) {
    return isNotEmpty(referenceDataName)
        ? join(DOT_DELIMITER, build(recordType, fieldConfigId), getFormattedName(referenceDataName))
        : build(recordType, fieldConfigId);
  }

  /**
   * Formats a settings name by lowercasing and joining with delimiter.
   *
   * @param settingsName the settings name
   * @return formatted name
   */
  private String getFormattedName(String settingsName) {
    return Arrays.stream(settingsName.split(SPACE))
        .map(String::toLowerCase)
        .collect(joining(DOT_DELIMITER));
  }
}
