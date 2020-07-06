package org.folio.service.mapping.referencedata;

import io.vertx.core.json.JsonObject;
import org.folio.processor.ReferenceData;

import java.util.HashMap;
import java.util.Map;

public class ReferenceDataImpl implements ReferenceData {

  private Map<String, Map<String, JsonObject>> referenceDataMap = new HashMap<>();


  @Override
  public Map<String, JsonObject> get(String key) {
    return referenceDataMap.get(key);
  }

  @Override
  public void put(String key, Map<String, JsonObject> value) {
    referenceDataMap.put(key, value);
  }
}
