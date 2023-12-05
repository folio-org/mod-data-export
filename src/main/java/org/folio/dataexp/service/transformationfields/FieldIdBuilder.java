package org.folio.dataexp.service.transformationfields;

import static java.lang.String.join;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import org.folio.dataexp.domain.dto.RecordTypes;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class FieldIdBuilder {

  public static final String DOT_DELIMITER = ".";

  public String build(RecordTypes recordType, String fieldConfigId) {
    return join(DOT_DELIMITER, recordType.toString().toLowerCase(), getFormattedName(fieldConfigId));
  }

  public String build(RecordTypes recordType, String fieldConfigId, String referenceDataName) {
    return isNotEmpty(referenceDataName) ?
      join(DOT_DELIMITER, build(recordType, fieldConfigId), getFormattedName(referenceDataName)) :
      build(recordType, fieldConfigId);
  }

  private String getFormattedName(String settingsName) {
    return Arrays.stream(settingsName.split(SPACE))
      .map(String::toLowerCase)
      .collect(joining(DOT_DELIMITER));
  }

}
