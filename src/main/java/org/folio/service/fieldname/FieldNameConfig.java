package org.folio.service.fieldname;

import org.folio.service.fieldname.loader.IdentifierTypesLoader;
import org.folio.service.fieldname.loader.ReferenceDataLoader;

import java.util.Map;

public enum FieldNameConfig {

  IDENTIFIERS("identifiers", "$.{recordType}[*].identifiers[?(@identifierTypeId=={id})].value", new IdentifierTypesLoader()),
  FIXED_LENGTH_DATA_ELEMENT("metadata.createdDate", "$.{recordType}.metadata.createdDate", MetadataParametersConstants.getFixedLengthDataElement()),
  HR_ID("hrid", "$.{recordType}.hrid");

  private final String id;
  private final String path;
  private final boolean isReferenceData;
  private ReferenceDataLoader referenceDataLoader;
  private Map<String, String> metadataParameters;

  FieldNameConfig(String id, String path) {
    this.id = id;
    this.path = path;
    this.isReferenceData = false;
  }

  FieldNameConfig(String id, String path, Map<String, String> metadataParameters) {
    this.id = id;
    this.path = path;
    this.isReferenceData = false;
    this.metadataParameters = metadataParameters;
  }

  FieldNameConfig(String id, String path, ReferenceDataLoader referenceDataLoader) {
    this.id = id;
    this.path = path;
    this.isReferenceData = true;
    this.referenceDataLoader = referenceDataLoader;
  }

  public String getId() {
    return id;
  }

  public String getPath() {
    return path;
  }

  public boolean isReferenceData() {
    return isReferenceData;
  }

  public ReferenceDataLoader getReferenceDataLoader() {
    return referenceDataLoader;
  }

  public Map<String, String> getMetadataParameters() {
    return metadataParameters;
  }
}
