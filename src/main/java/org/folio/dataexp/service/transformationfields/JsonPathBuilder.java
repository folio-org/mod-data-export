package org.folio.dataexp.service.transformationfields;

import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.processor.referencedata.JsonObjectWrapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JsonPathBuilder {
  public String build(RecordTypes recordType, TransformationFieldsConfig transformationFieldsConfig) {
    return transformationFieldsConfig.getPath().replace("{recordType}", getFormattedRecordType(recordType));
  }

  public String build(RecordTypes recordType, TransformationFieldsConfig transformationFieldsConfig, Map.Entry<String, JsonObjectWrapper> referenceDataEntry) {
    return build(recordType, transformationFieldsConfig).replace("{id}", referenceDataEntry.getKey());
  }

  private String getFormattedRecordType(RecordTypes recordType) {
    return switch (recordType) {
      case INSTANCE -> "instance";
      case HOLDINGS -> "holdings[*]";
      default -> "holdings[*].items[*]";
    };
  }

}
