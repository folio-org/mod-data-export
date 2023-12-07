package org.folio.dataexp.domain.bean;

import net.minidev.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ReferenceData {

  private final Map<String, Map<String, JSONObject>> referenceDataMap = new HashMap<>();

  public Map<String, JSONObject> get(String key) {
    return referenceDataMap.get(key);
  }

  public void put(String key, Map<String, JSONObject> value) {
    referenceDataMap.put(key, value);
  }

  public Map<String, Map<String, JSONObject>> getReferenceData() {
    return referenceDataMap;
  }
}
