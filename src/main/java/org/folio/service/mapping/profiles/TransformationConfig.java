package org.folio.service.mapping.profiles;

import org.folio.service.mapping.profiles.loader.ElectronicAccessRelationshipsLoader;
import org.folio.service.mapping.profiles.loader.SettingsLoader;

public enum TransformationConfig {

  PERMANENT_LOCATION_ID("permanentLocationId",
                        "$.{recordType}[*].permanentLocationId",
                        "Permanent location"),
  MATERIAL_TYPE_ID("materialTypeId",
                   "$.{recordType}[*].materialTypeId",
                   "Material types"),
  ELECTRONIC_ACCESS_LINK_TEXT("electronicAccess.linkText",
                              "$.{recordType}[*].electronicAccess[*].linkText",
                              "Electronic access - Link text"),
  ELECTRONIC_ACCESS_MATERIALS_SPECIFICATION("electronicAccess.materialsSpecification",
                                            "$.{recordType}[*].electronicAccess[*].materialsSpecification",
                                            "Electronic access - Materials specified"),
  ELECTRONIC_ACCESS_PUBLIC_NOTE("electronicAccess.publicNote",
                                "$.{recordType}[*].electronicAccess[*].publicNote",
                                "Electronic access - Public note"),
  ELECTRONIC_ACCESS_URI("electronicAccess.uri",
                        "$.{recordType}[*].electronicAccess[*].uri",
                        "Electronic access - URI"),
  ELECTRONIC_ACCESS_RELATIONSHIP_ID("electronicAccess.relationshipId",
                                    "$.{recordType}[*].electronicAccess[?(@relationshipId=={id})].relationshipId",
                                    "Electronic access - URL relationship",
                                    new ElectronicAccessRelationshipsLoader());

  private String id;
  private String path;
  private String formattedName;
  private boolean isSettings;
  private SettingsLoader settingsLoader;

  TransformationConfig(String id, String path, String formattedName) {
    this.id = id;
    this.path = path;
    this.formattedName = formattedName;
    this.isSettings = false;
  }

  TransformationConfig(String id, String path, String formattedName, SettingsLoader settingsLoader) {
    this.id = id;
    this.path = path;
    this.formattedName = formattedName;
    this.isSettings = true;
    this.settingsLoader = settingsLoader;
  }

  public String getId() {
    return id;
  }

  public String getPath() {
    return path;
  }

  public String getFormattedName() {
    return formattedName;
  }

  public boolean isSettings() {
    return isSettings;
  }

  public SettingsLoader getSettingsLoader() {
    return settingsLoader;
  }
}
