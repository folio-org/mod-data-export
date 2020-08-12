package org.folio.service.transformationfields;

import java.util.Map;

import static org.folio.service.mapping.referencedata.ReferenceDataImpl.ALTERNATIVE_TITLE_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.CALLNUMBER_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.IDENTIFIER_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.INSTANCE_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.LOAN_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.MATERIAL_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.MODES_OF_ISSUANCE;

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

  //Instance specific fields
  ALTERNATIVE_TITLES("alternativeTitleTypeId", "$.instance[*].alternativeTitles[?(@.alternativeTitleTypeId=={id})]", ALTERNATIVE_TITLE_TYPES),
  EDITIONS("editions", "$.instance.editions"),
  IDENTIFIERS("identifiers", "$.instance[*].identifiers[?(@.identifierTypeId=={id})].value", IDENTIFIER_TYPES),
  LANGUAGES("languages", "$.instance.languages"),
  MODE_OF_ISSUANCE_ID("modeOfIssuanceId", "$.instance.modeOfIssuanceId", MODES_OF_ISSUANCE),
  INSTANCE_TYPE("instanceTypeId", "$.instance.instanceTypeId", INSTANCE_TYPES),
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
  MATERIAL_TYPE_ID("materialTypeId", "$.item[*].materialTypeId", MATERIAL_TYPES),
  NUMBER_OF_PIECES("numberOfPieces", "$.item.numberOfPieces"),
  PERMANENT_LOAN_TYPE_ID("permanentLoanTypeId", "$.item.permanentLoanTypeId", LOAN_TYPES),
  STATUS("status", "$.item.status.name"),
  VOLUME("volume", "$.item.volume"),
  YEARCAPTION("yearCaption", "$.item.yearCaption"),
  CALL_NUMBER("callNumber", "$.items[*].effectiveCallNumberComponents.callNumber"),
  CALL_NUMBER_PREFIX("callNumberPrefix", "$.items[*].effectiveCallNumberComponents.prefix"),
  CALL_NUMBER_SUFFIX("callNumberSuffix", "$.items[*].effectiveCallNumberComponents.suffix"),
  CALL_NUMBER_TYPE("callNumberType", "$.items[?(@.effectiveCallNumberComponents.typeId=={id})].effectiveCallNumberComponents.typeId", CALLNUMBER_TYPES);

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
