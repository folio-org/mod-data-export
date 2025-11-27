package org.folio.dataexp.domain.bean;

import java.util.HashMap;
import java.util.Map;
import net.minidev.json.JSONObject;

/** Stores reference data mapped by key. */
public class ReferenceData {

  private final Map<String, Map<String, JSONObject>> referenceDataMap = new HashMap<>();

  /**
   * Gets the reference data map for the specified key.
   *
   * @param key the key to look up
   * @return the map of reference data for the key
   */
  public Map<String, JSONObject> get(String key) {
    return referenceDataMap.get(key);
  }

  /**
   * Puts a reference data map for the specified key.
   *
   * @param key the key to associate with the value
   * @param value the reference data map
   */
  public void put(String key, Map<String, JSONObject> value) {
    referenceDataMap.put(key, value);
  }

  /**
   * Gets the entire reference data map.
   *
   * @return the reference data map
   */
  public Map<String, Map<String, JSONObject>> getReferenceData() {
    return referenceDataMap;
  }
}
