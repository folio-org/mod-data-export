package org.folio.service.mapping.profiles;

import java.util.List;

public class MappingProfile {
  private String id;
  private String name;
  private String description;
  private List<RecordType> recordTypes;
  private String userFirstName;
  private String userLastName;
  private String userId;
  private List<MappingProfileRule> mappingProfileRules;

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

  public List<RecordType> getRecordTypes() {
    return recordTypes;
  }

  public void setRecordTypes(List<RecordType> recordTypes) {
    this.recordTypes = recordTypes;
  }

  public String getUserFirstName() {
    return userFirstName;
  }

  public void setUserFirstName(String userFirstName) {
    this.userFirstName = userFirstName;
  }

  public String getUserLastName() {
    return userLastName;
  }

  public void setUserLastName(String userLastName) {
    this.userLastName = userLastName;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public List<MappingProfileRule> getMappingProfileRules() {
    return mappingProfileRules;
  }

  public void setMappingProfileRules(List<MappingProfileRule> mappingProfileRules) {
    this.mappingProfileRules = mappingProfileRules;
  }

}
