package org.folio.service.transformationfields;

import java.util.Map;

import static org.folio.service.mapping.referencedata.ReferenceDataImpl.ALTERNATIVE_TITLE_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.IDENTIFIER_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.INSTANCE_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.LOAN_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.MATERIAL_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.MODES_OF_ISSUANCE;

/**
 * Initial data for the transformation field. While extending the enum, put new values in alphabetical order
 */
public enum TransformationFieldsConfig {

  ALTERNATIVE_TITLES("alternativeTitles", "$.{recordType}[*].alternativeTitles[?(@.alternativeTitle=={value})]", ALTERNATIVE_TITLE_TYPES),
  HR_ID("hrid", "$.{recordType}.hrid"),
  ID("id", "$.{recordType}.id"),
  IDENTIFIERS("identifiers", "$.{recordType}[*].identifiers[?(@.identifierTypeId=={id})].value", IDENTIFIER_TYPES),
  INSTANCE_ID("instanceId", "$.{recordType}.instanceId"),
  INSTANCE_TYPE("instanceTypeId", "$.{recordType}.instanceTypeId", INSTANCE_TYPES),

  MATERIAL_TYPE_ID("materialTypeId", "$.{recordType}[*].materialTypeId", MATERIAL_TYPES),
  MODE_OF_ISSUANCE_ID("modeOfIssuanceId", "$.{recordType}.modeOfIssuanceId", MODES_OF_ISSUANCE),

  METADATA_CREATED_DATE("metadata.createdDate", "$.{recordType}.metadata.createdDate", MetadataParametersConstants.getFixedLengthDataElement()),
  METADATA_CREATED_BY_USER_ID("metadata.createdByUserId", "$.{recordType}.metadata.createdByUserId"),
  METADATA_UPDATED_DATE("metadata.updatedDate", "$.{recordType}.metadata.updatedDate"),
  METADATA_UPDATED_BY_USER_ID("metadata.updatedByUserId", "$.{recordType}.metadata.updatedByUserId"),

  PERMANENT_LOAN_TYPE_ID("permanentLoanTypeId", "$.{recordType}.permanentLoanTypeId", LOAN_TYPES);


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
