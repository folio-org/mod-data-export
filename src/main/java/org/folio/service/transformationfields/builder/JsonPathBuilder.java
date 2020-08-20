package org.folio.service.transformationfields.builder;

import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.TransformationField.RecordType;
import org.folio.service.transformationfields.TransformationFieldsConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JsonPathBuilder implements PathBuilder {

  @Override
  public String build(RecordType recordType, TransformationFieldsConfig transformationFieldsConfig) {
    return transformationFieldsConfig.getPath().replace("{recordType}", recordType.toString().toLowerCase());
  }

  @Override
  public String build(RecordType recordType, TransformationFieldsConfig transformationFieldsConfig, Map.Entry<String, JsonObject> referenceDataEntry) {
    return build(recordType, transformationFieldsConfig).replace("{id}", referenceDataEntry.getKey());
  }

}
