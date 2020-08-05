package org.folio.service.transformationfields;

import java.util.Map;

import static org.folio.service.mapping.referencedata.ReferenceDataImpl.IDENTIFIER_TYPES;

/**
 * Initial data for the transformation field. While extending the enum, put new values in alphabetical order
 */
public enum TransformationFieldsConfig {
  //Common Fields
  ID("id", "$.{recordType}.id"),
  HR_ID("hrid", "$.{recordType}.hrid"),
  IDENTIFIERS("identifiers", "$.{recordType}[*].identifiers[?(@identifierTypeId=={id})].value", IDENTIFIER_TYPES),
  SOURCE("source", "$.{recordType}.source"),
  METADATA_CREATED_DATE("metadata.createdDate", "$.{recordType}.metadata.createdDate", MetadataParametersConstants.getFixedLengthDataElement()),
  METADATA_UPDATED_DATE("metadata.updatedDate", "$.{recordType}.metadata.updatedDate"),
  METADATA_CREATED_BY_USER_ID("metadata.createdByUserId", "$.{recordType}.metadata.createdByUserId"),
  METADATA_UPDATED_BY_USER_ID("metadata.updatedByUserId", "$.{recordType}.metadata.updatedByUserId"),

  //Instance specific fields
  EDITIONS("editions", "$.instance.editions"),
  LANGUAGES("languages", "$.instance.languages"),
  SUBJECTS("subjects", "$.instance.subjects"),
  TITLE("title", "$.instance.title"),

  //Holdings specific Fields
  INSTANCE_ID("instanceId", "$.holdings.instanceId"),

  //Item specific fields
  BARCODE("barcode", "$.item.barcode"),
  CHRONOLOGY("chronology", "$.item.chronology"),
  COPYNUMBER("copyNumber", "$.item.copyNumber"),
  DESCRIPTION_OF_PIECES("descriptionOfPieces", "$.item.descriptionOfPieces"),
  ENUMERATION("enumeration", "$.item.enumeration"),
  HOLDINGS_ID("holdingsRecordId", "$.item.holdingsRecordId"),
  NUMBER_OF_PIECES("numberOfPieces", "$.item.numberOfPieces"),
  STATUS("status", "$.item.status.name"),
  VOLUME("volume", "$.item.volume"),
  YEARCAPTION("yearCaption", "$.item.yearCaption");

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
