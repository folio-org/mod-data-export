package org.folio.service.fieldname.builder;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.TransformationField.RecordType;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Component
public class FieldIdBuilderImpl implements FieldIdBuilder {

  public static final String DOT_DELIMITER = ".";

  @Override
  public String build(RecordType recordType, String fieldConfigId) {
    return new StringJoiner(DOT_DELIMITER)
      .add(recordType.toString().toLowerCase())
      .add(getFormattedName(fieldConfigId))
      .toString();
  }

  @Override
  public String build(RecordType recordType, String fieldConfigId, String referenceDataName) {
    StringJoiner stringJoiner = new StringJoiner(DOT_DELIMITER)
      .add(recordType.toString().toLowerCase())
      .add(getFormattedName(fieldConfigId));
    return StringUtils.isNotEmpty(referenceDataName)
      ? stringJoiner.add(getFormattedName(referenceDataName)).toString()
      : stringJoiner.toString();

  }

  private String getFormattedName(String settingsName) {
    List<String> wordsInSettings = Arrays.asList(settingsName.split(StringUtils.SPACE));
    return wordsInSettings.size() > 1
      ? wordsInSettings.stream().map(String::toLowerCase).collect(Collectors.joining(DOT_DELIMITER))
      : wordsInSettings.get(0).toLowerCase();
  }

}
