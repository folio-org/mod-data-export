package org.folio.service.fieldname.builder;

import org.folio.rest.jaxrs.model.FieldName;
import org.folio.service.fieldname.FieldNameConfig;

/**
 * Builder is responsible to build id of the {@link FieldName}
 * that will be used to get mapping rules by this id for current field
 */
public interface FieldIdBuilder {

  /**
   * Build the id of the {@link FieldName}
   *
   * @param recordType    record type of the field
   * @param fieldConfigId given id from the {@link FieldNameConfig}
   * @return string with the id result
   */
  String build(FieldName.RecordType recordType, String fieldConfigId);

  /**
   * Build the id for the {@link FieldName} with the reference data name
   *
   * @param recordType    record type of the field
   * @param fieldConfigId given id from the {@link FieldNameConfig}
   * @return string with the id result
   */
  String build(FieldName.RecordType recordType, String fieldConfigId, String referenceDataName);

}
