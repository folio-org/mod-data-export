package org.folio.service.transformationfields;

import java.util.Map;

import static org.folio.service.mapping.referencedata.ReferenceDataImpl.*;

/**
 * Initial data for the transformation field. While extending the enum, put new values in alphabetical order
 */
public enum TransformationFieldsConfig {

  //Common Fields
  ID("id", "$.{recordType}.id"),
  HR_ID("hrid", "$.{recordType}.hrid"),
  SOURCE("source", "$.{recordType}.source"),
  METADATA_CREATED_DATE("metadata.createdDate", "$.{recordType}.metadata.createdDate", MetadataParametersConstants.getFixedLengthDataElement()),
  METADATA_UPDATED_DATE("metadata.updatedDate", "$.{recordType}.metadata.updatedDate"),
  METADATA_CREATED_BY_USER_ID("metadata.createdByUserId", "$.{recordType}.metadata.createdByUserId"),
  METADATA_UPDATED_BY_USER_ID("metadata.updatedByUserId", "$.{recordType}.metadata.updatedByUserId"),
  ELECTRONIC_ACCESS_URI("electronic.access.uri", "$.{recordType}.electronicAccess[?(@.relationshipId=={id})].uri", ELECTRONIC_ACCESS_RELATIONSHIPS),
  ELECTRONIC_ACCESS_LINKTEXT("electronic.access.linkText", "$.{recordType}.electronicAccess[?(@.relationshipId=={id})].linkText", ELECTRONIC_ACCESS_RELATIONSHIPS),
  ELECTRONIC_ACCESS_MATERIALS_SPECIFIED("electronic.access.materialsSpecification", "$.{recordType}.electronicAccess[?(@.relationshipId=={id})].materialsSpecification", ELECTRONIC_ACCESS_RELATIONSHIPS),
  ELECTRONIC_ACCESS_PUBLICNOTE("electronic.access.publicNote", "$.{recordType}.electronicAccess[?(@.relationshipId=={id})].publicNote", ELECTRONIC_ACCESS_RELATIONSHIPS),
  ELECTRONIC_ACCESS_URI_DEFAULT("electronic.access.uri", "$.{recordType}.electronicAccess[?(!(@.relationshipId) || @.relationshipId == null)].uri"),
  ELECTRONIC_ACCESS_LINKTEXT_DEFAULT("electronic.access.linkText", "$.{recordType}.electronicAccess[?(!(@.relationshipId) || @.relationshipId == null)].linkText"),
  ELECTRONIC_ACCESS_MATERIALS_SPECIFIED_DEFAULT("electronic.access.materialsSpecification", "$.{recordType}.electronicAccess[?(!(@.relationshipId) || @.relationshipId == null)].materialsSpecification"),
  ELECTRONIC_ACCESS_PUBLICNOTE_DEFAULT("electronic.access.publicNote", "$.{recordType}.electronicAccess[?(!(@.relationshipId) || @.relationshipId == null)].publicNote"),

  //Instance specific fields
  ALTERNATIVE_TITLES("alternativeTitleTypeId", "$.instance[*].alternativeTitles[?(@.alternativeTitleTypeId=={id})]", ALTERNATIVE_TITLE_TYPES),
  CONTRIBUTOR_NAME_TYPE("contributorNameTypeId", "$.instance[*].contributorNameTypeId[?(@.contributorNameTypeId=={id} && ?(!(@.primary) || @.primary == false))]", CONTRIBUTOR_NAME_TYPES),
  CONTRIBUTOR_NAME_TYPE_PRIMARY("contributorNameTypeId.primary", "$.instance[*].contributorNameTypeId[?(@.contributorNameTypeId=={id} && ?((@.primary) && @.primary == true))]", CONTRIBUTOR_NAME_TYPES),
  EDITIONS("editions", "$.instance.editions"),
  IDENTIFIERS("identifiers", "$.instance[*].identifiers[?(@.identifierTypeId=={id})].value", IDENTIFIER_TYPES),
  LANGUAGES("languages", "$.instance.languages"),
  MODE_OF_ISSUANCE_ID("modeOfIssuanceId", "$.instance.modeOfIssuanceId", MODES_OF_ISSUANCE),
  INSTANCE_TYPE("instanceTypeId", "$.instance.instanceTypeId", INSTANCE_TYPES),
  SUBJECTS("subjects", "$.instance.subjects"),
  TITLE("title", "$.instance.title"),

  //Holdings specific Fields
  INSTANCE_ID("instanceId", "$.holdings.instanceId"),
  HOLDING_NOTE_TYPE("holdingNoteTypeId", "$.holdings.notes[?(@.holdingsNoteTypeId=={id}) && ?(!(@.staffOnly) || @.staffOnly == false))]", HOLDING_NOTE_TYPES),
  HOLDING_NOTE_TYPE_STAFF_ONLY("holdingNoteTypeId.staffOnly", "$.holdings.notes[?(@.holdingsNoteTypeId=={id}) && ?(!(@.staffOnly) || @.staffOnly == true))]", HOLDING_NOTE_TYPES, " - Staff only"),

  //Item specific fields
  BARCODE("barcode", "$.item.barcode"),
  CHRONOLOGY("chronology", "$.item.chronology"),
  COPYNUMBER("copyNumber", "$.item.copyNumber"),
  DESCRIPTION_OF_PIECES("descriptionOfPieces", "$.item.descriptionOfPieces"),
  ENUMERATION("enumeration", "$.item.enumeration"),
  HOLDINGS_ID("holdingsRecordId", "$.item.holdingsRecordId"),
  MATERIAL_TYPE_ID("materialTypeId", "$.item[*].materialTypeId", MATERIAL_TYPES),
  NUMBER_OF_PIECES("numberOfPieces", "$.item.numberOfPieces"),
  PERMANENT_LOAN_TYPE_ID("permanentLoanTypeId", "$.item.permanentLoanTypeId", LOAN_TYPES),
  STATUS("status", "$.item.status.name"),
  VOLUME("volume", "$.item.volume"),
  YEARCAPTION("yearCaption", "$.item.yearCaption"),
  ITEM_NOTE_TYPE("itemNoteTypeId", "$.item.notes[?(@.itemNoteTypeId=={id}) && ?(!(@.staffOnly) || @.staffOnly == false))]", ITEM_NOTE_TYPES),
  ITEM_NOTE_TYPE_STAFF_ONLY("itemNoteTypeId.staffOnly", "$.item.notes[?(@.itemNoteTypeId=={id}) && ?(!(@.staffOnly) || @.staffOnly == true))]", ITEM_NOTE_TYPES, " - Staff only");

  private final String fieldId;
  private final String path;
  private final boolean isReferenceData;
  private String referenceDataKey;
  private Map<String, String> metadataParameters;
  private String displayNameCondition;

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

  TransformationFieldsConfig(String fieldId, String path, String referenceDataKey, String displayNameCondition) {
    this.fieldId = fieldId;
    this.path = path;
    this.isReferenceData = true;
    this.referenceDataKey = referenceDataKey;
    this.displayNameCondition = displayNameCondition;
  }

  TransformationFieldsConfig(String fieldId, String path, Map<String, String> metadataParameters) {
    this.fieldId = fieldId;
    this.path = path;
    this.isReferenceData = false;
    this.metadataParameters = metadataParameters;
  }

  public String getDisplayNameCondition() {
    return displayNameCondition;
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
}
