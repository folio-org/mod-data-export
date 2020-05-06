package org.folio.service.mapping.profiles;

import java.util.StringJoiner;

import static org.folio.service.mapping.profiles.RecordType.HOLDINGS;

public class TrasnformationNameFormatter {

  public static final String DASH_DELIMITER = " - ";

  public String format(RecordType recordType, String fieldName) {
    return new StringJoiner(DASH_DELIMITER)
      .add(getFormattedRecordType(recordType))
      .add(fieldName)
      .toString();
  }

  public String format(RecordType recordType, String fieldName, String settingsName) {
    return new StringJoiner(DASH_DELIMITER)
      .add(format(recordType, fieldName))
      .add(settingsName)
      .toString();
  }

  private String getFormattedRecordType(RecordType recordType) {
    if(HOLDINGS.equals(recordType))
      return "Holdings";
    return "Items";
  }

}
