package org.folio.service.mapping.profiles.builder;

import org.folio.service.mapping.profiles.RecordType;
import org.folio.service.mapping.profiles.TransformationConfig;

public class JsonPathBuilder implements PathBuilder {

  @Override
  public String build(RecordType recordType, TransformationConfig transformationConfig, String settingsId) {
    return build(recordType, transformationConfig).replace("{id}", settingsId);
  }

  @Override
  public String build(RecordType recordType, TransformationConfig transformationConfig) {
    return transformationConfig.getPath().replace("{recordType}", recordType.toString().toLowerCase());
  }

}
