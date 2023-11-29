package org.folio.dataexp.service.export.strategies.transformation.fields;

import java.util.Map;


/**
 * Initial data for the transformation field. While extending the enum, put new values in alphabetical order
 * Few conventions to follow while creating fields:
 * 1) default fields must have ".default" appeneded to the fieldname at the end
 * 2) json filter that is used for comparing Strings(UUID included) must be enclosed in quotes
 */
public enum TransformationFieldsConfig {

  HOLDINGS_CALL_NUMBER_TYPE("callNumberType", "$.holdings[*].callNumberTypeId");

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


  public class Constants{
    private Constants() {
    }
    static final String TEMPORARY_LOCATION_ID = "$.holdings[*].temporaryLocationId";
    static final String PERMANENT_LOCATION_ID = "$.holdings[*].permanentLocationId";
  }
}
