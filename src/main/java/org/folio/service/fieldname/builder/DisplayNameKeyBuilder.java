package org.folio.service.fieldname.builder;

import org.folio.rest.jaxrs.model.FieldName;

public interface DisplayNameKeyBuilder {

  String build(FieldName.RecordType recordType, String fieldName);

}
