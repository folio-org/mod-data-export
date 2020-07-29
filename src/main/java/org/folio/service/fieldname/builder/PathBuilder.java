package org.folio.service.fieldname.builder;

import org.folio.rest.jaxrs.model.FieldName.RecordType;
import org.folio.service.fieldname.FieldNameConfig;

public interface PathBuilder {

  String build(RecordType recordType, FieldNameConfig fieldNameConfig, String settingsId);

  String build(RecordType recordType, FieldNameConfig fieldNameConfig);

}
