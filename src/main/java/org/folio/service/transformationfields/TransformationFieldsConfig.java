package org.folio.service.transformationfields;

import java.util.Map;

import static org.folio.service.mapping.referencedata.ReferenceDataImpl.*;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.HOLDING_NOTE_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.ITEM_NOTE_TYPES;

/**
 * Initial data for the transformation field. While extending the enum, put new values in alphabetical order
 * Few conventions to follow while creating fields:
 * 1) default fields must have ".default" appeneded to the fieldname at the end
 * 2) json filter that is used for comparing Strings(UUID included) must be enclosed in quotes
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
  ELECTRONIC_ACCESS_URI("electronic.access.uri", "$.{recordType}.electronicAccess[?(@.relationshipId=='{id}')].uri", ELECTRONIC_ACCESS_RELATIONSHIPS),
  ELECTRONIC_ACCESS_LINKTEXT("electronic.access.linkText", "$.{recordType}.electronicAccess[?(@.relationshipId=='{id}')].linkText", ELECTRONIC_ACCESS_RELATIONSHIPS),
  ELECTRONIC_ACCESS_MATERIALS_SPECIFIED("electronic.access.materialsSpecification", "$.{recordType}.electronicAccess[?(@.relationshipId=='{id}')].materialsSpecification", ELECTRONIC_ACCESS_RELATIONSHIPS),
  ELECTRONIC_ACCESS_PUBLICNOTE("electronic.access.publicNote", "$.{recordType}.electronicAccess[?(@.relationshipId=='{id}')].publicNote", ELECTRONIC_ACCESS_RELATIONSHIPS),
  ELECTRONIC_ACCESS_URI_DEFAULT("electronic.access.uri.default", "$.{recordType}.electronicAccess[?(!(@.relationshipId) || @.relationshipId == null)].uri"),
  ELECTRONIC_ACCESS_LINKTEXT_DEFAULT("electronic.access.linkText.default", "$.{recordType}.electronicAccess[?(!(@.relationshipId) || @.relationshipId == null)].linkText"),
  ELECTRONIC_ACCESS_MATERIALS_SPECIFIED_DEFAULT("electronic.access.materialsSpecification.default", "$.{recordType}.electronicAccess[?(!(@.relationshipId) || @.relationshipId == null)].materialsSpecification"),
  ELECTRONIC_ACCESS_PUBLICNOTE_DEFAULT("electronic.access.publicNote.default", "$.{recordType}.electronicAccess[?(!(@.relationshipId) || @.relationshipId == null)].publicNote"),

  //Instance specific fields
  ALTERNATIVE_TITLES("alternativeTitleTypeName", "$.instance.alternativeTitles[?(@.alternativeTitleTypeId=='{id}')].name", ALTERNATIVE_TITLE_TYPES),
  CONTRIBUTOR_NAME("contributorName", "$.instance.contributors[?(@.contributorNameTypeId=='{id}' && (!(@.primary) || @.primary == false))].name", CONTRIBUTOR_NAME_TYPES),
  CONTRIBUTOR_NAME_PRIMARY("contributorName.primary", "$.instance.contributors[?(@.contributorNameTypeId=='{id}' && ((@.primary) && @.primary == true))].name", CONTRIBUTOR_NAME_TYPES),
  EDITIONS("editions", "$.instance.editions"),
  IDENTIFIERS("identifiers", "$.instance.identifiers[?(@.identifierTypeId=='{id}')].value", IDENTIFIER_TYPES),
  LANGUAGES("languages", "$.instance.languages"),
  MODE_OF_ISSUANCE_ID("modeOfIssuanceId", "$.instance[?(@.modeOfIssuanceId=='{id}')].modeOfIssuanceId", MODES_OF_ISSUANCE),
  INSTANCE_TYPE("instanceTypeId", "$.instance[?(@.instanceTypeId=='{id}')].instanceTypeId", INSTANCE_TYPES),
  SUBJECTS("subjects", "$.instance.subjects"),
  TITLE("title", "$.instance.title"),

  //Holdings specific Fields
  INSTANCE_ID("instanceId", "$.holdings[*].instanceId"),
  HOLDINGS_CALL_NUMBER("callNumber", "$.holdings[*].callNumber"),
  HOLDINGS_CALL_NUMBER_PREFIX("callNumberPrefix", "$.holdings[*].callNumberPrefix"),
  HOLDINGS_CALL_NUMBER_SUFFIX("callNumberSuffix", "$.holdings[*].callNumberSuffix"),
  HOLDINGS_CALL_NUMBER_TYPE("callNumberType", "$.holdings[*].callNumberTypeId"),
  HOLDING_NOTE_TYPE("holdingNoteTypeId", "$.holdings[*].notes[?(@.holdingsNoteTypeId=='{id}' && (!(@.staffOnly) || @.staffOnly == false))].note", HOLDING_NOTE_TYPES),
  HOLDING_NOTE_TYPE_STAFF_ONLY("holdingNoteTypeId.staffOnly", "$.holdings[*].notes[?(@.holdingsNoteTypeId=='{id}' && ((@.staffOnly) && @.staffOnly == true))].note", HOLDING_NOTE_TYPES),

  //Item specific fields
  BARCODE("barcode", "$.items[*].barcode"),
  CHRONOLOGY("chronology", "$.items[*].chronology"),
  COPYNUMBER("copyNumber", "$.items[*].copyNumber"),
  DESCRIPTION_OF_PIECES("descriptionOfPieces", "$.items[*].descriptionOfPieces"),
  ENUMERATION("enumeration", "$.items[*].enumeration"),
  HOLDINGS_ID("holdingsRecordId", "$.items[*].holdingsRecordId"),
  MATERIAL_TYPE_ID("materialTypeId", "$.items[*].materialTypeId"),
  NUMBER_OF_PIECES("numberOfPieces", "$.items[*].numberOfPieces"),
  PERMANENT_LOAN_TYPE_ID("permanentLoanTypeId", "$.items[*].permanentLoanTypeId"),
  STATUS("status", "$.items[*].status.name"),
  VOLUME("volume", "$.items[*].volume"),
  YEARCAPTION("yearCaption", "$.items[*].yearCaption"),
  ITEM_CALL_NUMBER("callNumber", "$.item[*].effectiveCallNumberComponents.callNumber"),
  ITEM_CALL_NUMBER_PREFIX("callNumberPrefix", "$.item[*].effectiveCallNumberComponents.prefix"),
  ITEM_CALL_NUMBER_SUFFIX("callNumberSuffix", "$.item[*].effectiveCallNumberComponents.suffix"),
  ITEM_CALL_NUMBER_TYPE("callNumberType", "$.item[*].effectiveCallNumberComponents.typeId"),
  ITEM_NOTE_TYPE("itemNoteTypeId", "$.item[*].notes[?(@.itemNoteTypeId=='{id}' && (!(@.staffOnly) || @.staffOnly == false))].note", ITEM_NOTE_TYPES),
  ITEM_NOTE_TYPE_STAFF_ONLY("itemNoteTypeId.staffOnly", "$.item[*].notes[?(@.itemNoteTypeId=='{id}' && ((@.staffOnly) && @.staffOnly == true))].note", ITEM_NOTE_TYPES);


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

  TransformationFieldsConfig(String fieldId, String path, Map<String, String> metadataParameters) {
    this.fieldId = fieldId;
    this.path = path;
    this.isReferenceData = false;
    this.metadataParameters = metadataParameters;
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
