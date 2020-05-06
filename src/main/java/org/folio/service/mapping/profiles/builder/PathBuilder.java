package org.folio.service.mapping.profiles.builder;

import org.folio.service.mapping.profiles.TransformationConfig;

public interface PathBuilder {

  String build(TransformationConfig transformationConfig, String settingsId);

}
