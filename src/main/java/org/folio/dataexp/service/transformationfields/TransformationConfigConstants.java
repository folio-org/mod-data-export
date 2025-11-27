package org.folio.dataexp.service.transformationfields;

import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ALTERNATIVE_TITLES;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.BARCODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.CHRONOLOGY;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.CONTRIBUTOR_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.CONTRIBUTOR_NAME_PRIMARY;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.COPYNUMBER;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.DESCRIPTION_OF_PIECES;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.EDITIONS;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ELECTRONIC_ACCESS_LINKTEXT;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ELECTRONIC_ACCESS_LINKTEXT_DEFAULT;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ELECTRONIC_ACCESS_MATERIALS_SPECIFIED;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ELECTRONIC_ACCESS_MATERIALS_SPECIFIED_DEFAULT;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ELECTRONIC_ACCESS_PUBLICNOTE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ELECTRONIC_ACCESS_PUBLICNOTE_DEFAULT;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ELECTRONIC_ACCESS_URI;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ELECTRONIC_ACCESS_URI_DEFAULT;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ENUMERATION;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDINGS_CALL_NUMBER;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDINGS_CALL_NUMBER_PREFIX;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDINGS_CALL_NUMBER_SUFFIX;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDINGS_CALL_NUMBER_TYPE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDINGS_ID;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDINGS_STATEMENT;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDINGS_STATEMENT_INDEXES;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDINGS_STATEMENT_INDEXES_NOTE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDINGS_STATEMENT_INDEXES_NOTE_STAFF;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDINGS_STATEMENT_NOTE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDINGS_STATEMENT_NOTE_STAFF;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDINGS_STATEMENT_SUPPLEMENTS;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDINGS_STATEMENT_SUPPLEMENTS_NOTE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDINGS_STATEMENT_SUPPLEMENTS_NOTE_STAFF;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_NOTE_TYPE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_NOTE_TYPE_STAFF_ONLY;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_PERMANENT_LOCATION_CAMPUS_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_PERMANENT_LOCATION_CAMPUS_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_PERMANENT_LOCATION_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_PERMANENT_LOCATION_INSTITUTION_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_PERMANENT_LOCATION_INSTITUTION_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_PERMANENT_LOCATION_LIBRARY_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_PERMANENT_LOCATION_LIBRARY_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_PERMANENT_LOCATION_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_TEMPORARY_LOCATION_CAMPUS_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_TEMPORARY_LOCATION_CAMPUS_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_TEMPORARY_LOCATION_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_TEMPORARY_LOCATION_INSTITUTION_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_TEMPORARY_LOCATION_INSTITUTION_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_TEMPORARY_LOCATION_LIBRARY_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_TEMPORARY_LOCATION_LIBRARY_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HOLDING_TEMPORARY_LOCATION_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.HR_ID;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ID;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.IDENTIFIERS;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.INSTANCE_ID;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.INSTANCE_TYPE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_CALL_NUMBER;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_CALL_NUMBER_PREFIX;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_CALL_NUMBER_SUFFIX;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_CALL_NUMBER_TYPE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_EFFECTIVE_LOCATION_CAMPUS_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_EFFECTIVE_LOCATION_CAMPUS_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_EFFECTIVE_LOCATION_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_EFFECTIVE_LOCATION_INSTITUTION_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_EFFECTIVE_LOCATION_INSTITUTION_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_EFFECTIVE_LOCATION_LIBRARY_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_EFFECTIVE_LOCATION_LIBRARY_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_EFFECTIVE_LOCATION_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_NOTE_TYPE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_NOTE_TYPE_STAFF_ONLY;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_PERMANENT_LOCATION_CAMPUS_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_PERMANENT_LOCATION_CAMPUS_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_PERMANENT_LOCATION_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_PERMANENT_LOCATION_INSTITUTION_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_PERMANENT_LOCATION_INSTITUTION_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_PERMANENT_LOCATION_LIBRARY_CODE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_PERMANENT_LOCATION_LIBRARY_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.ITEM_PERMANENT_LOCATION_NAME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.LANGUAGES;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.MATERIAL_TYPE_ID;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.METADATA_CREATED_BY_USER_ID;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.METADATA_CREATED_DATE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.METADATA_UPDATED_BY_USER_ID;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.METADATA_UPDATED_DATE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.MODE_OF_ISSUANCE_ID;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.NUMBER_OF_PIECES;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.PERMANENT_LOAN_TYPE_ID;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.SOURCE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.STATUS;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.SUBJECTS;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.TITLE;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.VOLUME;
import static org.folio.dataexp.service.transformationfields.TransformationFieldsConfig.YEARCAPTION;

import java.util.EnumSet;
import java.util.Set;

/** Constants for sets of transformation field configs for each record type. */
public class TransformationConfigConstants {

  /** Instance record type transformation fields. */
  protected static final Set<TransformationFieldsConfig> INSTANCE_FIELDS_CONFIGS =
      EnumSet.of(
          ALTERNATIVE_TITLES,
          CONTRIBUTOR_NAME,
          CONTRIBUTOR_NAME_PRIMARY,
          EDITIONS,
          ELECTRONIC_ACCESS_URI,
          ELECTRONIC_ACCESS_LINKTEXT,
          ELECTRONIC_ACCESS_MATERIALS_SPECIFIED,
          ELECTRONIC_ACCESS_PUBLICNOTE,
          ELECTRONIC_ACCESS_URI_DEFAULT,
          ELECTRONIC_ACCESS_LINKTEXT_DEFAULT,
          ELECTRONIC_ACCESS_MATERIALS_SPECIFIED_DEFAULT,
          ELECTRONIC_ACCESS_PUBLICNOTE_DEFAULT,
          HR_ID,
          IDENTIFIERS,
          ID,
          INSTANCE_TYPE,
          MODE_OF_ISSUANCE_ID,
          LANGUAGES,
          SOURCE,
          SUBJECTS,
          TITLE,
          LANGUAGES,
          METADATA_CREATED_DATE,
          METADATA_UPDATED_DATE,
          METADATA_CREATED_BY_USER_ID,
          METADATA_UPDATED_BY_USER_ID);

  /** Holdings record type transformation fields. */
  protected static final Set<TransformationFieldsConfig> HOLDINGS_FIELDS_CONFIGS =
      EnumSet.of(
          ELECTRONIC_ACCESS_URI,
          ELECTRONIC_ACCESS_LINKTEXT,
          ELECTRONIC_ACCESS_MATERIALS_SPECIFIED,
          ELECTRONIC_ACCESS_PUBLICNOTE,
          ELECTRONIC_ACCESS_URI_DEFAULT,
          ELECTRONIC_ACCESS_LINKTEXT_DEFAULT,
          ELECTRONIC_ACCESS_MATERIALS_SPECIFIED_DEFAULT,
          ELECTRONIC_ACCESS_PUBLICNOTE_DEFAULT,
          HR_ID,
          ID,
          INSTANCE_ID,
          HOLDINGS_CALL_NUMBER,
          HOLDINGS_CALL_NUMBER_PREFIX,
          HOLDINGS_CALL_NUMBER_SUFFIX,
          HOLDINGS_CALL_NUMBER_TYPE,
          HOLDING_NOTE_TYPE,
          HOLDING_NOTE_TYPE_STAFF_ONLY,
          METADATA_CREATED_DATE,
          METADATA_CREATED_BY_USER_ID,
          METADATA_UPDATED_DATE,
          METADATA_UPDATED_BY_USER_ID,
          HOLDING_PERMANENT_LOCATION_NAME,
          HOLDING_PERMANENT_LOCATION_CODE,
          HOLDING_PERMANENT_LOCATION_LIBRARY_NAME,
          HOLDING_PERMANENT_LOCATION_LIBRARY_CODE,
          HOLDING_PERMANENT_LOCATION_CAMPUS_NAME,
          HOLDING_PERMANENT_LOCATION_CAMPUS_CODE,
          HOLDING_PERMANENT_LOCATION_INSTITUTION_NAME,
          HOLDING_PERMANENT_LOCATION_INSTITUTION_CODE,
          HOLDING_TEMPORARY_LOCATION_NAME,
          HOLDING_TEMPORARY_LOCATION_CODE,
          HOLDING_TEMPORARY_LOCATION_LIBRARY_NAME,
          HOLDING_TEMPORARY_LOCATION_LIBRARY_CODE,
          HOLDING_TEMPORARY_LOCATION_CAMPUS_NAME,
          HOLDING_TEMPORARY_LOCATION_CAMPUS_CODE,
          HOLDING_TEMPORARY_LOCATION_INSTITUTION_NAME,
          HOLDING_TEMPORARY_LOCATION_INSTITUTION_CODE,
          HOLDINGS_STATEMENT,
          HOLDINGS_STATEMENT_NOTE_STAFF,
          HOLDINGS_STATEMENT_NOTE,
          HOLDINGS_STATEMENT_SUPPLEMENTS,
          HOLDINGS_STATEMENT_SUPPLEMENTS_NOTE_STAFF,
          HOLDINGS_STATEMENT_SUPPLEMENTS_NOTE,
          HOLDINGS_STATEMENT_INDEXES,
          HOLDINGS_STATEMENT_INDEXES_NOTE_STAFF,
          HOLDINGS_STATEMENT_INDEXES_NOTE);

  /** Item record type transformation fields. */
  protected static final Set<TransformationFieldsConfig> ITEM_FIELDS_CONFIGS =
      EnumSet.of(
          ELECTRONIC_ACCESS_URI,
          ELECTRONIC_ACCESS_LINKTEXT,
          ELECTRONIC_ACCESS_MATERIALS_SPECIFIED,
          ELECTRONIC_ACCESS_PUBLICNOTE,
          ELECTRONIC_ACCESS_URI_DEFAULT,
          ELECTRONIC_ACCESS_LINKTEXT_DEFAULT,
          ELECTRONIC_ACCESS_MATERIALS_SPECIFIED_DEFAULT,
          ELECTRONIC_ACCESS_PUBLICNOTE_DEFAULT,
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
          ITEM_CALL_NUMBER,
          ITEM_CALL_NUMBER_PREFIX,
          ITEM_CALL_NUMBER_SUFFIX,
          ITEM_CALL_NUMBER_TYPE,
          ITEM_NOTE_TYPE,
          ITEM_NOTE_TYPE_STAFF_ONLY,
          METADATA_CREATED_DATE,
          METADATA_UPDATED_DATE,
          METADATA_CREATED_BY_USER_ID,
          METADATA_UPDATED_BY_USER_ID,
          ITEM_PERMANENT_LOCATION_NAME,
          ITEM_PERMANENT_LOCATION_CODE,
          ITEM_PERMANENT_LOCATION_LIBRARY_NAME,
          ITEM_PERMANENT_LOCATION_LIBRARY_CODE,
          ITEM_PERMANENT_LOCATION_CAMPUS_NAME,
          ITEM_PERMANENT_LOCATION_CAMPUS_CODE,
          ITEM_PERMANENT_LOCATION_INSTITUTION_NAME,
          ITEM_PERMANENT_LOCATION_INSTITUTION_CODE,
          ITEM_EFFECTIVE_LOCATION_NAME,
          ITEM_EFFECTIVE_LOCATION_CODE,
          ITEM_EFFECTIVE_LOCATION_LIBRARY_NAME,
          ITEM_EFFECTIVE_LOCATION_LIBRARY_CODE,
          ITEM_EFFECTIVE_LOCATION_CAMPUS_NAME,
          ITEM_EFFECTIVE_LOCATION_CAMPUS_CODE,
          ITEM_EFFECTIVE_LOCATION_INSTITUTION_NAME,
          ITEM_EFFECTIVE_LOCATION_INSTITUTION_CODE);

  private TransformationConfigConstants() {}
}
