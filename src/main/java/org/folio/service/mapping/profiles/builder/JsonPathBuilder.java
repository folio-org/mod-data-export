package org.folio.service.mapping.profiles.builder;

import org.folio.service.mapping.profiles.TransformationConfig;

public class JsonPathBuilder implements PathBuilder {

  @Override
  public String build(TransformationConfig transformationConfig, String settingsId) {
    return transformationConfig.getPath().replace("{id}", settingsId);
  }

}
