package org.folio.service.transformationfields;

import java.util.EnumSet;
import java.util.Set;

import static org.folio.service.transformationfields.TransformationFieldsConfig.*;

public class TransformationConfigConstants {

  protected static final Set<TransformationFieldsConfig> INSTANCE_FIELDS_CONFIGS = EnumSet.of(
    ALTERNATIVE_TITLES,
    EDITIONS,
    HR_ID,
    IDENTIFIERS,
    ID,
    INSTANCE_TYPE,
    MODE_OF_ISSUANCE_ID,
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
    METADATA_CREATED_BY_USER_ID,
    METADATA_UPDATED_DATE,
    METADATA_UPDATED_BY_USER_ID
  );

  protected static final Set<TransformationFieldsConfig> ITEM_FIELDS_CONFIGS = EnumSet.of(
    HR_ID,
    ID,
    HOLDINGS_ID,
    BARCODE,
    VOLUME,
    ENUMERATION,
    CHRONOLOGY,
    YEARCAPTION,
    STATUS,
    DESCRIPTION_OF_PIECES,
    COPYNUMBER,
    NUMBER_OF_PIECES,
    MATERIAL_TYPE_ID,
    PERMANENT_LOAN_TYPE_ID,
    CALLNUMBER,
    CALLNUMBER_PREFIX,
    CALL_NUMBER_SUFFIX,
    CALL_NUMBER_TYPE,
    METADATA_CREATED_DATE,
    METADATA_UPDATED_DATE,
    METADATA_CREATED_BY_USER_ID,
    METADATA_UPDATED_BY_USER_ID
  );

  private TransformationConfigConstants() {
  }

}
