package org.folio.service.mapping.referencedata;

import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ReferenceData {
  private Map<String, JsonObject> natureOfContentTerms = new HashMap<>();
  private Map<String, JsonObject> identifierTypes = new HashMap<>();

  public void addNatureOfContentTerms(Map<String, JsonObject> natureOfContentTerms) {
    this.natureOfContentTerms.putAll(natureOfContentTerms);
  }

  public void addIdentifierTypes(Map<String, JsonObject> identifierTypes) {
    this.identifierTypes.putAll(identifierTypes);
  }

  public Map<String, JsonObject> getNatureOfContentTerms() {
    return natureOfContentTerms;
  }

  public Map<String, JsonObject> getIdentifierTypes() {
    return identifierTypes;
  }
}
