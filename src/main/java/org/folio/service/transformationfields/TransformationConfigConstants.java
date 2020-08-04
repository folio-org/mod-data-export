package org.folio.service.transformationfields;

import java.util.EnumSet;
import java.util.Set;

import static org.folio.service.transformationfields.TransformationFieldsConfig.*;


public class TransformationConfigConstants {

  protected static final Set<TransformationFieldsConfig> INSTANCE_FIELDS_CONFIGS = EnumSet.of(
      EDITIONS,
      HR_ID,
      IDENTIFIERS,
      ID,
      LANGUAGES,
      SOURCE,
      SUBJECTS,
      LANGUAGES,
      METADATA_CREATED_DATE,
      METADATA_UPDATED_DATE,
      METADATA_CREATED_BY_USER_ID,
      METADATA_UPDATED_BY_USER_ID
  );

  protected static final Set<TransformationFieldsConfig> HOLDINGS_FIELDS_CONFIGS = EnumSet.of(
    ID,
    HR_ID,
    INSTANCE_ID,
    METADATA_CREATED_DATE,
    METADATA_UPDATED_DATE,
    METADATA_CREATED_BY_USER_ID,
    METADATA_UPDATED_BY_USER_ID
  );

  protected static final Set<TransformationFieldsConfig> ITEM_FIELDS_CONFIGS = EnumSet.of(
    HR_ID
  );

  private TransformationConfigConstants() {
  }

}
