package org.folio.service.transformationfields.builder;

import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.TransformationField.RecordType;
import org.folio.service.transformationfields.TransformationFieldsConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.folio.service.mapping.referencedata.ReferenceDataImpl.ALTERNATIVE_TITLE_TYPES;

@Component
public class JsonPathBuilder implements PathBuilder {

  private static final String REFERENCE_DATA_NAME_KEY = "name";

  @Override
  public String build(RecordType recordType, TransformationFieldsConfig transformationFieldsConfig) {
    return transformationFieldsConfig.getPath().replace("{recordType}", recordType.toString().toLowerCase());
  }

  @Override
  public String build(RecordType recordType, TransformationFieldsConfig transformationFieldsConfig, Map.Entry<String, JsonObject> referenceDataEntry) {
    if (transformationFieldsConfig.getReferenceDataKey().equals(ALTERNATIVE_TITLE_TYPES)) {
      return build(recordType, transformationFieldsConfig).replace("{value}", referenceDataEntry.getValue().getString(REFERENCE_DATA_NAME_KEY));
    } else {
      return build(recordType, transformationFieldsConfig).replace("{id}", referenceDataEntry.getKey());
    }
  }

}
