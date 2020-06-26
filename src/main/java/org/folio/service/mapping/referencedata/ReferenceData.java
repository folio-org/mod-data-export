package org.folio.service.mapping.referencedata;

import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ReferenceData {
  private Map<String, JsonObject> natureOfContentTerms = new HashMap<>();
  private Map<String, JsonObject> identifierTypes = new HashMap<>();
  private Map<String, JsonObject> contributorNameTypes = new HashMap<>();
  private Map<String, JsonObject> locations = new HashMap<>();
  private Map<String, JsonObject> materialTypes = new HashMap<>();
  private Map<String, JsonObject> instanceTypes = new HashMap<>();



  public void addContributorNameTypes(Map<String, JsonObject> contributorNameTypes) {
    this.contributorNameTypes = contributorNameTypes;
  }

  public void addNatureOfContentTerms(Map<String, JsonObject> natureOfContentTerms) {
    this.natureOfContentTerms.putAll(natureOfContentTerms);
  }

  public void addIdentifierTypes(Map<String, JsonObject> identifierTypes) {
    this.identifierTypes.putAll(identifierTypes);
  }

  public void addLocations(Map<String, JsonObject> locations) {
    this.locations.putAll(locations);
  }

  public void addInstanceTypes(Map<String, JsonObject> instanceTypes) {
    this.instanceTypes.putAll(instanceTypes);
  }

  public void addMaterialTypes(Map<String, JsonObject> materialTypes) {
    this.materialTypes.putAll(materialTypes);
  }

  public Map<String, JsonObject> getNatureOfContentTerms() {
    return natureOfContentTerms;
  }

  public Map<String, JsonObject> getIdentifierTypes() {
    return identifierTypes;
  }

  public Map<String, JsonObject> getContributorNameTypes() {
    return contributorNameTypes;
  }

  public Map<String, JsonObject> getLocations() {
    return locations;
  }

  public Map<String, JsonObject> getMaterialTypes() { return materialTypes; }

  public Map<String, JsonObject> getInstanceTypes() { return instanceTypes; }
}
