package org.folio.service.fieldname.builder;

import org.folio.rest.jaxrs.model.FieldName;
import org.folio.rest.jaxrs.model.FieldName.RecordType;
import org.folio.service.fieldname.FieldNameConfig;

/**
 * Builder is responsible to build jsonPath of the {@link FieldName}.
 */
public interface PathBuilder {

  /**
   * Build the jsonPath of the {@link FieldName}
   *
   * @param recordType      record type of the field
   * @param fieldNameConfig {@link FieldNameConfig}
   * @return string with the jsonPath result
   */
  String build(RecordType recordType, FieldNameConfig fieldNameConfig);

  /**
   * Build the jsonPath of the {@link FieldName} with reference data
   *
   * @param recordType      record type of the field
   * @param fieldNameConfig {@link FieldNameConfig}
   * @param referenceDataId id of the reference data
   * @return string with the jsonPath result
   */
  String build(RecordType recordType, FieldNameConfig fieldNameConfig, String referenceDataId);

}
