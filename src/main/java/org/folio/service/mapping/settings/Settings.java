package org.folio.service.mapping.settings;

import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class Settings {
  private Map<String, JsonObject> natureOfContentTerms = new HashMap<>();

  public void addNatureOfContentTerms(Map<String, JsonObject> natureOfContentTerms) {
    this.natureOfContentTerms.putAll(natureOfContentTerms);
  }

  public Map<String, JsonObject> getNatureOfContentTerms() {
    return natureOfContentTerms;
  }
}
