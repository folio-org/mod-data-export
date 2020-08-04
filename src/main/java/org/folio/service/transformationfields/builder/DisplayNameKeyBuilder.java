package org.folio.service.transformationfields.builder;

import org.folio.rest.jaxrs.model.TransformationField;
import org.folio.rest.jaxrs.model.TransformationField.RecordType;
import org.folio.service.transformationfields.TransformationFieldsConfig;

/**
 * Builder is responsible to build displayNameKey of the {@link TransformationField}
 * that will be used for the localization at UI side
 */
public interface DisplayNameKeyBuilder {

  /**
   * Build the displayNameKey of the {@link TransformationField}
   *
   * @param recordType    record type of the field
   * @param fieldConfigId given id from the {@link TransformationFieldsConfig}
   * @return string with the displayNameKey result
   */
  String build(RecordType recordType, String fieldConfigId);

}
