package org.folio.service.mapping.profiles;

public class MappingProfileRule {
  private String id;
  private RecordType recordType;
  private boolean enabled;
  private String path;
  private String transformation;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public RecordType getRecordType() {
    return recordType;
  }

  public void setRecordType(RecordType recordType) {
    this.recordType = recordType;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getTransformation() {
    return transformation;
  }

  public void setTransformation(String transformation) {
    this.transformation = transformation;
  }

}
