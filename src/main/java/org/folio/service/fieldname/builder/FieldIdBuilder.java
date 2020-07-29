package org.folio.service.fieldname.builder;

import org.folio.rest.jaxrs.model.FieldName;

public interface FieldIdBuilder {

  String build(FieldName.RecordType recordType, String fieldConfigId);

  String build(FieldName.RecordType recordType, String fieldConfigId, String settingsValue);

}
