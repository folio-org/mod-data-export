package org.folio.service.transformationfields;

import java.util.EnumSet;
import java.util.Set;

import static org.folio.service.transformationfields.TransformationFieldsConfig.HR_ID;
import static org.folio.service.transformationfields.TransformationFieldsConfig.ID;
import static org.folio.service.transformationfields.TransformationFieldsConfig.IDENTIFIERS;
import static org.folio.service.transformationfields.TransformationFieldsConfig.INSTANCE_ID;
import static org.folio.service.transformationfields.TransformationFieldsConfig.METADATA_CREATED_BY_USER_ID;
import static org.folio.service.transformationfields.TransformationFieldsConfig.METADATA_CREATED_DATE;
import static org.folio.service.transformationfields.TransformationFieldsConfig.METADATA_UPDATED_BY_USER_ID;
import static org.folio.service.transformationfields.TransformationFieldsConfig.METADATA_UPDATED_DATE;

public class TransformationConfigConstants {

  protected static final Set<TransformationFieldsConfig> INSTANCE_FIELDS_CONFIGS = EnumSet.of(
    HR_ID,
    IDENTIFIERS
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
