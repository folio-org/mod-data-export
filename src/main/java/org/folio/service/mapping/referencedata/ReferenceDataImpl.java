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
  public static final String LIBRARIES = "loclibs";
  public static final String CAMPUSES = "loccamps";
  public static final String INSTITUTIONS = "locinsts";
  public static final String MATERIAL_TYPES = "materialTypes";
  public static final String INSTANCE_TYPES = "instanceTypes";
  public static final String INSTANCE_FORMATS = "instanceFormats";
  public static final String ELECTRONIC_ACCESS_RELATIONSHIPS = "electronicAccessRelationships";
  public static final String ALTERNATIVE_TITLE_TYPES = "alternativeTitleTypes";
  public static final String MODES_OF_ISSUANCE = "issuanceModes";
  public static final String LOAN_TYPES = "loantypes";
  public static final String HOLDING_NOTE_TYPES = "holdingsNoteTypes";
  public static final String ITEM_NOTE_TYPES = "itemNoteTypes";

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
