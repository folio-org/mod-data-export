package org.folio.service.fieldname.builder;

import org.folio.rest.jaxrs.model.TransformationField;
import org.folio.rest.jaxrs.model.TransformationField.RecordType;
import org.folio.service.fieldname.TransformationFieldsConfig;

/**
 * Builder is responsible to build id of the {@link TransformationField}
 * that will be used to get mapping rules by this id for current field
 */
public interface FieldIdBuilder {

  /**
   * Build the id of the {@link TransformationField}
   *
   * @param recordType    record type of the field
   * @param fieldConfigId given id from the {@link TransformationFieldsConfig}
   * @return string with the id result
   */
  String build(RecordType recordType, String fieldConfigId);

  /**
   * Build the id for the {@link TransformationField} with the reference data name
   *
   * @param recordType    record type of the field
   * @param fieldConfigId given id from the {@link TransformationFieldsConfig}
   * @return string with the id result
   */
  String build(RecordType recordType, String fieldConfigId, String referenceDataName);

}
