package org.folio.service.mapping.profiles.builder;

import org.folio.service.mapping.profiles.RecordType;
import org.folio.service.mapping.profiles.TransformationConfig;

public interface PathBuilder {

  String build(RecordType recordType, TransformationConfig transformationConfig, String settingsId);

  String build(RecordType recordType, TransformationConfig transformationConfig);

}
