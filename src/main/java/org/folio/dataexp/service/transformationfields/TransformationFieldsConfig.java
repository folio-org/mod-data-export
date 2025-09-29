package org.folio.dataexp.service.transformationfields;

import static org.folio.dataexp.service.transformationfields.Constants.HOLDINGS_ITEMS_EFFECTIVE_LOCATION_ID_PATH;
import static org.folio.dataexp.service.transformationfields.Constants.HOLDINGS_ITEMS_PERMANENT_LOCATION_ID_PATH;
import static org.folio.dataexp.util.ExternalPathResolver.ALTERNATIVE_TITLE_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.CONTRIBUTOR_NAME_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.ELECTRONIC_ACCESS_RELATIONSHIPS;
import static org.folio.dataexp.util.ExternalPathResolver.HOLDING_NOTE_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.IDENTIFIER_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.INSTANCE_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.ISSUANCE_MODES;
import static org.folio.dataexp.util.ExternalPathResolver.ITEM_NOTE_TYPES;

import java.util.Map;

/**
 * Enum for initial data for transformation fields.
 * Conventions:
 * 1) Default fields must have ".default" appended to the field name.
 * 2) JSON filter for comparing Strings (including UUID) must be enclosed in quotes.
 */
public enum TransformationFieldsConfig {


  // Common Fields
  ID("id", "$.{recordType}.id"),
  HR_ID("hrid", "$.{recordType}.hrid"),
  SOURCE("source", "$.{recordType}.source"),
  METADATA_CREATED_DATE("metadata.createdDate", "$.{recordType}.metadata.createdDate"),
  METADATA_UPDATED_DATE("metadata.updatedDate", "$.{recordType}.metadata.updatedDate"),
  METADATA_CREATED_BY_USER_ID("metadata.createdByUserId",
    "$.{recordType}.metadata.createdByUserId"),
  METADATA_UPDATED_BY_USER_ID("metadata.updatedByUserId",
    "$.{recordType}.metadata.updatedByUserId"),
  ELECTRONIC_ACCESS_URI("electronic.access.uri",
    "$.{recordType}.electronicAccess[?(@.relationshipId=='{id}')].uri",
    ELECTRONIC_ACCESS_RELATIONSHIPS),
  ELECTRONIC_ACCESS_LINKTEXT("electronic.access.linkText",
    "$.{recordType}.electronicAccess[?(@.relationshipId=='{id}')].linkText",
    ELECTRONIC_ACCESS_RELATIONSHIPS),
  ELECTRONIC_ACCESS_MATERIALS_SPECIFIED("electronic.access.materialsSpecification",
    "$.{recordType}.electronicAccess[?(@.relationshipId=='{id}')].materialsSpecification",
    ELECTRONIC_ACCESS_RELATIONSHIPS),
  ELECTRONIC_ACCESS_PUBLICNOTE("electronic.access.publicNote",
    "$.{recordType}.electronicAccess[?(@.relationshipId=='{id}')].publicNote",
    ELECTRONIC_ACCESS_RELATIONSHIPS),
  ELECTRONIC_ACCESS_URI_DEFAULT("electronic.access.uri.default",
    "$.{recordType}.electronicAccess[?(!(@.relationshipId) || @.relationshipId == null)].uri"),
  ELECTRONIC_ACCESS_LINKTEXT_DEFAULT("electronic.access.linkText.default",
    "$.{recordType}.electronicAccess[?(!(@.relationshipId) || @.relationshipId == null)].linkText"),
  ELECTRONIC_ACCESS_MATERIALS_SPECIFIED_DEFAULT(
      "electronic.access.materialsSpecification.default",
    "$.{recordType}.electronicAccess[?(!(@.relationshipId) ||"
      + " @.relationshipId == null)].materialsSpecification"),
  ELECTRONIC_ACCESS_PUBLICNOTE_DEFAULT("electronic.access.publicNote.default",
    "$.{recordType}.electronicAccess[?(!(@.relationshipId) ||"
      + " @.relationshipId == null)].publicNote"),

  // Instance specific fields
  ALTERNATIVE_TITLES("alternativeTitleTypeName",
    "$.instance.alternativeTitles[?(@.alternativeTitleTypeId=='{id}')].alternativeTitle",
    ALTERNATIVE_TITLE_TYPES),
  CONTRIBUTOR_NAME("contributorName",
    "$.instance.contributors[?(@.contributorNameTypeId=='{id}' &&"
    + " (!(@.primary) || @.primary == false))].name", CONTRIBUTOR_NAME_TYPES),
  CONTRIBUTOR_NAME_PRIMARY("contributorName.primary",
    "$.instance.contributors[?(@.contributorNameTypeId=='{id}' &&"
    + " ((@.primary) && @.primary == true))].name", CONTRIBUTOR_NAME_TYPES),
  EDITIONS("editions", "$.instance.editions"),
  IDENTIFIERS("identifiers",
    "$.instance.identifiers[?(@.identifierTypeId=='{id}')].value", IDENTIFIER_TYPES),
  LANGUAGES("languages", "$.instance.languages"),
  MODE_OF_ISSUANCE_ID("modeOfIssuanceId",
    "$.instance[?(@.modeOfIssuanceId=='{id}')].modeOfIssuanceId", ISSUANCE_MODES),
  INSTANCE_TYPE("instanceTypeId",
    "$.instance[?(@.instanceTypeId=='{id}')].instanceTypeId", INSTANCE_TYPES),
  SUBJECTS("subjects", "$.instance.subjects[*].value"),
  TITLE("title", "$.instance.title"),

  // Holdings specific Fields
  INSTANCE_ID("instanceId", "$.holdings[*].instanceId"),
  HOLDINGS_CALL_NUMBER("callNumber", "$.holdings[*].callNumber"),
  HOLDINGS_CALL_NUMBER_PREFIX("callNumberPrefix", "$.holdings[*].callNumberPrefix"),
  HOLDINGS_CALL_NUMBER_SUFFIX("callNumberSuffix", "$.holdings[*].callNumberSuffix"),
  HOLDINGS_CALL_NUMBER_TYPE("callNumberType", "$.holdings[*].callNumberTypeId"),
  HOLDING_NOTE_TYPE("holdingNoteTypeId",
    "$.holdings[*].notes[?(@.holdingsNoteTypeId=='{id}' && (!(@.staffOnly) ||"
      + " @.staffOnly == false))].note", HOLDING_NOTE_TYPES),
  HOLDING_NOTE_TYPE_STAFF_ONLY("holdingNoteTypeId.staffOnly",
    "$.holdings[*].notes[?(@.holdingsNoteTypeId=='{id}' && ((@.staffOnly) &&"
      + " @.staffOnly == true))].note", HOLDING_NOTE_TYPES),
  HOLDING_PERMANENT_LOCATION_NAME("permanentLocation.name", Constants.PERMANENT_LOCATION_ID),
  HOLDING_PERMANENT_LOCATION_CODE("permanentLocation.code", Constants.PERMANENT_LOCATION_ID),
  HOLDING_PERMANENT_LOCATION_LIBRARY_NAME("permanentLocation.library.name",
    Constants.PERMANENT_LOCATION_ID),
  HOLDING_PERMANENT_LOCATION_LIBRARY_CODE("permanentLocation.library.code",
    Constants.PERMANENT_LOCATION_ID),
  HOLDING_PERMANENT_LOCATION_CAMPUS_NAME("permanentLocation.campus.name",
    Constants.PERMANENT_LOCATION_ID),
  HOLDING_PERMANENT_LOCATION_CAMPUS_CODE("permanentLocation.campus.code",
    Constants.PERMANENT_LOCATION_ID),
  HOLDING_PERMANENT_LOCATION_INSTITUTION_NAME("permanentLocation.institution.name",
    Constants.PERMANENT_LOCATION_ID),
  HOLDING_PERMANENT_LOCATION_INSTITUTION_CODE("permanentLocation.institution.code",
    Constants.PERMANENT_LOCATION_ID),
  HOLDING_TEMPORARY_LOCATION_NAME("temporaryLocation.name",
    Constants.TEMPORARY_LOCATION_ID),
  HOLDING_TEMPORARY_LOCATION_CODE("temporaryLocation.code",
    Constants.TEMPORARY_LOCATION_ID),
  HOLDING_TEMPORARY_LOCATION_LIBRARY_NAME("temporaryLocation.library.name",
    Constants.TEMPORARY_LOCATION_ID),
  HOLDING_TEMPORARY_LOCATION_LIBRARY_CODE("temporaryLocation.library.code",
    Constants.TEMPORARY_LOCATION_ID),
  HOLDING_TEMPORARY_LOCATION_CAMPUS_NAME("temporaryLocation.campus.name",
    Constants.TEMPORARY_LOCATION_ID),
  HOLDING_TEMPORARY_LOCATION_CAMPUS_CODE("temporaryLocation.campus.code",
    Constants.TEMPORARY_LOCATION_ID),
  HOLDING_TEMPORARY_LOCATION_INSTITUTION_NAME("temporaryLocation.institution.name",
    Constants.TEMPORARY_LOCATION_ID),
  HOLDING_TEMPORARY_LOCATION_INSTITUTION_CODE("temporaryLocation.institution.code",
    Constants.TEMPORARY_LOCATION_ID),
  HOLDINGS_STATEMENT("holdingsStatements.statement",
    "$.holdings[*].holdingsStatements[*].statement"),
  HOLDINGS_STATEMENT_NOTE("holdingsStatements.note",
    "$.holdings[*].holdingsStatements[*].note"),
  HOLDINGS_STATEMENT_NOTE_STAFF("holdingsStatements.staffNote",
    "$.holdings[*].holdingsStatements[*].staffNote"),
  HOLDINGS_STATEMENT_SUPPLEMENTS("holdingsStatementsForSupplements.statement",
    "$.holdings[*].holdingsStatementsForSupplements[*].statement"),
  HOLDINGS_STATEMENT_SUPPLEMENTS_NOTE("holdingsStatementsForSupplements.note",
    "$.holdings[*].holdingsStatementsForSupplements[*].note"),
  HOLDINGS_STATEMENT_SUPPLEMENTS_NOTE_STAFF("holdingsStatementsForSupplements.staffNote",
    "$.holdings[*].holdingsStatementsForSupplements[*].staffNote"),
  HOLDINGS_STATEMENT_INDEXES("holdingsStatementsForIndexes.statement",
    "$.holdings[*].holdingsStatementsForIndexes[*].statement"),
  HOLDINGS_STATEMENT_INDEXES_NOTE("holdingsStatementsForIndexes.note",
    "$.holdings[*].holdingsStatementsForIndexes[*].note"),
  HOLDINGS_STATEMENT_INDEXES_NOTE_STAFF("holdingsStatementsForIndexes.staffNote",
    "$.holdings[*].holdingsStatementsForIndexes[*].staffNote"),



  // Item specific fields
  BARCODE("barcode", "$.holdings[*].items[*].barcode"),
  CHRONOLOGY("chronology", "$.holdings[*].items[*].chronology"),
  COPYNUMBER("copyNumber", "$.holdings[*].items[*].copyNumber"),
  DESCRIPTION_OF_PIECES("descriptionOfPieces",
    "$.holdings[*].items[*].descriptionOfPieces"),
  ENUMERATION("enumeration", "$.holdings[*].items[*].enumeration"),
  HOLDINGS_ID("holdingsRecordId", "$.holdings[*].items[*].holdingsRecordId"),
  MATERIAL_TYPE_ID("materialTypeId", "$.holdings[*].items[*].materialTypeId"),
  NUMBER_OF_PIECES("numberOfPieces", "$.holdings[*].items[*].numberOfPieces"),
  PERMANENT_LOAN_TYPE_ID("permanentLoanTypeId",
    "$.holdings[*].items[*].permanentLoanTypeId"),
  STATUS("status", "$.holdings[*].items[*].status.name"),
  VOLUME("volume", "$.holdings[*].items[*].volume"),
  YEARCAPTION("yearCaption", "$.holdings[*].items[*].yearCaption[*]"),
  ITEM_CALL_NUMBER("callNumber",
    "$.holdings[*].items[*].effectiveCallNumberComponents.callNumber"),
  ITEM_CALL_NUMBER_PREFIX("callNumberPrefix",
    "$.holdings[*].items[*].effectiveCallNumberComponents.prefix"),
  ITEM_CALL_NUMBER_SUFFIX("callNumberSuffix",
    "$.holdings[*].items[*].effectiveCallNumberComponents.suffix"),
  ITEM_CALL_NUMBER_TYPE("callNumberType",
    "$.holdings[*].items[*].effectiveCallNumberComponents.typeId"),
  ITEM_NOTE_TYPE("itemNoteTypeId",
    "$.holdings[*].items[*].notes[?(@.itemNoteTypeId=='{id}' && (!(@.staffOnly) ||"
      + " @.staffOnly == false))].note", ITEM_NOTE_TYPES),
  ITEM_NOTE_TYPE_STAFF_ONLY("itemNoteTypeId.staffOnly",
    "$.holdings[*].items[*].notes[?(@.itemNoteTypeId=='{id}' && ((@.staffOnly) &&"
      + " @.staffOnly == true))].note", ITEM_NOTE_TYPES),
  ITEM_PERMANENT_LOCATION_NAME("permanentLocation.name",
    HOLDINGS_ITEMS_PERMANENT_LOCATION_ID_PATH),
  ITEM_PERMANENT_LOCATION_CODE("permanentLocation.code",
    HOLDINGS_ITEMS_PERMANENT_LOCATION_ID_PATH),
  ITEM_PERMANENT_LOCATION_LIBRARY_NAME("permanentLocation.library.name",
    HOLDINGS_ITEMS_PERMANENT_LOCATION_ID_PATH),
  ITEM_PERMANENT_LOCATION_LIBRARY_CODE("permanentLocation.library.code",
    HOLDINGS_ITEMS_PERMANENT_LOCATION_ID_PATH),
  ITEM_PERMANENT_LOCATION_CAMPUS_NAME("permanentLocation.campus.name",
    HOLDINGS_ITEMS_PERMANENT_LOCATION_ID_PATH),
  ITEM_PERMANENT_LOCATION_CAMPUS_CODE("permanentLocation.campus.code",
    HOLDINGS_ITEMS_PERMANENT_LOCATION_ID_PATH),
  ITEM_PERMANENT_LOCATION_INSTITUTION_NAME("permanentLocation.institution.name",
    HOLDINGS_ITEMS_PERMANENT_LOCATION_ID_PATH),
  ITEM_PERMANENT_LOCATION_INSTITUTION_CODE("permanentLocation.institution.code",
    HOLDINGS_ITEMS_PERMANENT_LOCATION_ID_PATH),
  ITEM_EFFECTIVE_LOCATION_NAME("effectiveLocation.name",
    HOLDINGS_ITEMS_EFFECTIVE_LOCATION_ID_PATH),
  ITEM_EFFECTIVE_LOCATION_CODE("effectiveLocation.code",
    HOLDINGS_ITEMS_EFFECTIVE_LOCATION_ID_PATH),
  ITEM_EFFECTIVE_LOCATION_LIBRARY_NAME("effectiveLocation.library.name",
    HOLDINGS_ITEMS_EFFECTIVE_LOCATION_ID_PATH),
  ITEM_EFFECTIVE_LOCATION_LIBRARY_CODE("effectiveLocation.library.code",
    HOLDINGS_ITEMS_EFFECTIVE_LOCATION_ID_PATH),
  ITEM_EFFECTIVE_LOCATION_CAMPUS_NAME("effectiveLocation.campus.name",
    HOLDINGS_ITEMS_EFFECTIVE_LOCATION_ID_PATH),
  ITEM_EFFECTIVE_LOCATION_CAMPUS_CODE("effectiveLocation.campus.code",
    HOLDINGS_ITEMS_EFFECTIVE_LOCATION_ID_PATH),
  ITEM_EFFECTIVE_LOCATION_INSTITUTION_NAME("effectiveLocation.institution.name",
    HOLDINGS_ITEMS_EFFECTIVE_LOCATION_ID_PATH),
  ITEM_EFFECTIVE_LOCATION_INSTITUTION_CODE("effectiveLocation.institution.code",
    HOLDINGS_ITEMS_EFFECTIVE_LOCATION_ID_PATH);

  private final String fieldId;
  private final String path;
  private final boolean isReferenceData;
  private String referenceDataKey;
  private Map<String, String> metadataParameters;

  TransformationFieldsConfig(String fieldId, String path) {
    this.fieldId = fieldId;
    this.path = path;
    this.isReferenceData = false;
  }

  TransformationFieldsConfig(String fieldId, String path, String referenceDataKey) {
    this.fieldId = fieldId;
    this.path = path;
    this.isReferenceData = true;
    this.referenceDataKey = referenceDataKey;
  }

  public String getFieldId() {
    return fieldId;
  }

  public String getPath() {
    return path;
  }

  public boolean isReferenceData() {
    return isReferenceData;
  }

  public String getReferenceDataKey() {
    return referenceDataKey;
  }

  public Map<String, String> getMetadataParameters() {
    return metadataParameters;
  }


  /**
   * Constants for location ID paths used in transformation fields.
   */
  public class Constants {
    private Constants() {}

    static final String TEMPORARY_LOCATION_ID = "$.holdings[*].temporaryLocationId";
    static final String PERMANENT_LOCATION_ID = "$.holdings[*].permanentLocationId";
  }
}
