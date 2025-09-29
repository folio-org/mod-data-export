package org.folio.dataexp.service.transformationfields;

import java.util.Map;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.processor.referencedata.JsonObjectWrapper;
import org.springframework.stereotype.Component;

/**
 * Builder for JSON paths for transformation fields.
 */
@Component
public class JsonPathBuilder {

  /**
   * Builds a JSON path for a transformation field.
   *
   * @param recordType the record type
   * @param transformationFieldsConfig the transformation field config
   * @return the JSON path
   */
  public String build(RecordTypes recordType,
      TransformationFieldsConfig transformationFieldsConfig) {
    return transformationFieldsConfig.getPath().replace("{recordType}",
        getFormattedRecordType(recordType));
  }

  /**
   * Builds a JSON path for a transformation field with reference data.
   *
   * @param recordType the record type
   * @param transformationFieldsConfig the transformation field config
   * @param referenceDataEntry the reference data entry
   * @return the JSON path
   */
  public String build(RecordTypes recordType,
      TransformationFieldsConfig transformationFieldsConfig,
      Map.Entry<String, JsonObjectWrapper> referenceDataEntry) {
    return build(recordType, transformationFieldsConfig).replace("{id}",
        referenceDataEntry.getKey());
  }

  /**
   * Formats the record type for JSON path usage.
   *
   * @param recordType the record type
   * @return formatted record type string
   */
  private String getFormattedRecordType(RecordTypes recordType) {
    return switch (recordType) {
      case INSTANCE -> "instance";
      case HOLDINGS -> "holdings[*]";
      default -> "holdings[*].items[*]";
    };
  }
}
