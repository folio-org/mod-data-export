package org.folio.service.fieldname.builder;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.FieldName;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
public class FieldIdBuilderImpl implements FieldIdBuilder {

  public static final String DOT_DELIMITER = ".";

  @Override
  public String build(FieldName.RecordType recordType, String fieldConfigId) {
    return new StringJoiner(DOT_DELIMITER)
      .add(recordType.toString().toLowerCase())
      .add(getFormattedName(fieldConfigId))
      .toString();
  }

  @Override
  public String build(FieldName.RecordType recordType, String fieldConfigId, String settingsValue) {
    return new StringJoiner(DOT_DELIMITER)
      .add(recordType.toString().toLowerCase())
      .add(getFormattedName(fieldConfigId))
      .add(getFormattedName(settingsValue))
      .toString();
  }

  private String getFormattedName(String settingsName) {
    if (StringUtils.isEmpty(settingsName)) {
      return StringUtils.EMPTY;
    }
    List<String> wordsInSettings = Arrays.asList(settingsName.split(StringUtils.SPACE));
    if (wordsInSettings.size() > 1) {
      return wordsInSettings.stream().map(String::toLowerCase).collect(Collectors.joining(DOT_DELIMITER));
    } else if (wordsInSettings.size() == 1) {
      return wordsInSettings.get(0).toLowerCase();
    } else {
      return StringUtils.EMPTY;
    }
  }

}
