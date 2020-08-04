package org.folio.service.transformationfields.builder;

import org.folio.rest.jaxrs.model.TransformationField.RecordType;
import org.folio.service.transformationfields.TransformationFieldsConfig;
import org.springframework.stereotype.Component;

@Component
public class JsonPathBuilder implements PathBuilder {

  @Override
  public String build(RecordType recordType, TransformationFieldsConfig transformationFieldsConfig) {
    return transformationFieldsConfig.getPath().replace("{recordType}", recordType.toString().toLowerCase());
  }

  @Override
  public String build(RecordType recordType, TransformationFieldsConfig transformationFieldsConfig, String referenceDataId) {
    return build(recordType, transformationFieldsConfig).replace("{id}", referenceDataId);
  }

}
