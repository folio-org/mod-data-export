package org.folio.service.fieldname.builder;

import org.folio.rest.jaxrs.model.FieldName.RecordType;
import org.folio.service.fieldname.FieldNameConfig;
import org.springframework.stereotype.Service;

@Service
public class JsonPathBuilder implements PathBuilder {

  @Override
  public String build(RecordType recordType, FieldNameConfig fieldNameConfig) {
    return fieldNameConfig.getPath().replace("{recordType}", recordType.toString().toLowerCase());
  }

  @Override
  public String build(RecordType recordType, FieldNameConfig fieldNameConfig, String referenceDataId) {
    return build(recordType, fieldNameConfig).replace("{id}", referenceDataId);
  }

}
