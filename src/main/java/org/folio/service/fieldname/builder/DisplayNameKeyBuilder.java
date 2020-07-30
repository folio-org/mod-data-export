package org.folio.service.fieldname.builder;

import org.folio.rest.jaxrs.model.FieldName;
import org.folio.service.fieldname.FieldNameConfig;

/**
 * Builder is responsible to build displayNameKey of the {@link FieldName}
 * that will be used for the localization at UI side
 */
public interface DisplayNameKeyBuilder {

  /**
   * Build the displayNameKey of the {@link FieldName}
   *
   * @param recordType    record type of the field
   * @param fieldConfigId given id from the {@link FieldNameConfig}
   * @return string with the displayNameKey result
   */
  String build(FieldName.RecordType recordType, String fieldConfigId);

}
