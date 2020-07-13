package org.folio.service.mapping.referencedata;

import io.vertx.core.json.JsonObject;
import org.folio.processor.ReferenceData;

import java.util.HashMap;
import java.util.Map;

public class ReferenceDataImpl implements ReferenceData {

  public static final String NATURE_OF_CONTENT_TERMS = "natureOfContentTerms";
  public static final String IDENTIFIER_TYPES = "identifierTypes";
  public static final String CONTRIBUTOR_NAME_TYPES = "contributorNameTypes";
  public static final String LOCATIONS = "locations";
  public static final String MATERIAL_TYPES = "materialTypes";
  public static final String INSTANCE_TYPES = "instanceTypes";
  public static final String INSTANCE_FORMATS = "instanceFormats";
  public static final String ELECTRONIC_ACCESS_RELATIONSHIPS = "electronicAccessRelationships";

  private final Map<String, Map<String, JsonObject>> referenceDataMap = new HashMap<>();

  @Override
  public Map<String, JsonObject> get(String key) {
    return referenceDataMap.get(key);
  }

  @Override
  public void put(String key, Map<String, JsonObject> value) {
    referenceDataMap.put(key, value);
  }

}
