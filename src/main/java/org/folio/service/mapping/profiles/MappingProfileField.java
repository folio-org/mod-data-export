package org.folio.service.mapping.profiles;

public class MappingProfileField {
  private String id;
  private String name;
  private String description;
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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
