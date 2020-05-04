package org.folio.service.mapping.settings;

import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class Settings {
  private Map<String, JsonObject> natureOfContentTerms = new HashMap<>();
  private Map<String, JsonObject> materialTypes = new HashMap<>();
  private Map<String, JsonObject> electronicAccessRelationships = new HashMap<>();

  public void addNatureOfContentTerms(Map<String, JsonObject> natureOfContentTerms) {
    this.natureOfContentTerms.putAll(natureOfContentTerms);
  }

  public Map<String, JsonObject> getNatureOfContentTerms() {
    return natureOfContentTerms;
  }

  public Map<String, JsonObject> getMaterialTypes() {
    return materialTypes;
  }

  public void addMaterialTypes(Map<String, JsonObject> materialTypes) {
    this.materialTypes.putAll(materialTypes);
  }

  public Map<String, JsonObject> getElectronicAccessRelationships() {
    return electronicAccessRelationships;
  }

  public void addElectronicAccessRelationships(Map<String, JsonObject> electronicAccessRelationships) {
    this.electronicAccessRelationships.putAll(electronicAccessRelationships);
  }
}
