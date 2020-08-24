package org.folio.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ExternalPathResolver {

  private ExternalPathResolver() {
  }

  public static final String SRS = "srs";
  public static final String INSTANCE = "instance";
  public static final String CONTENT_TERMS = "natureOfContentTerms";
  public static final String IDENTIFIER_TYPES = "identifierTypes";
  public static final String CONTRIBUTOR_NAME_TYPES = "contributorNameTypes";
  public static final String LOCATIONS = "locations";
  public static final String MATERIAL_TYPES = "mtypes";
  public static final String INSTANCE_TYPES = "instanceTypes";
  public static final String INSTANCE_FORMATS = "instanceFormats";
  public static final String ELECTRONIC_ACCESS_RELATIONSHIPS = "electronicAccessRelationships";
  public static final String ALTERNATIVE_TITLE_TYPES = "alternativeTitleTypes";
  public static final String ISSUANCE_MODES = "issuanceModes";
  public static final String LOAN_TYPES = "loantypes";
  public static final String HOLDING_NOTE_TYPES = "holdingsNoteTypes";
  public static final String ITEM_NOTE_TYPES = "itemNoteTypes";
  public static final String USERS = "users";
  public static final String HOLDING = "holding";
  public static final String ITEM = "item";
  public static final String CONFIGURATIONS = "configurations";


  private static final Map<String, String> EXTERNAL_APIS;
  private static final Map<String, String> EXTERNAL_APIS_WITH_PREFIX;
  private static final Map<String, String> EXTERNAL_APIS_WITH_SUFFIX;
  private static final Map<String, String> EXTERNAL_APIS_WITH_ID;

  static {
    Map<String, String> apis = new HashMap<>();
    apis.put(SRS, "/source-storage/source-records");
    apis.put(INSTANCE, "/instance-storage/instances");
    apis.put(HOLDING, "/holdings-storage/holdings");
    apis.put(ITEM, "/item-storage/items");
    apis.put(CONTENT_TERMS, "/nature-of-content-terms");
    apis.put(IDENTIFIER_TYPES, "/identifier-types");
    apis.put(CONTRIBUTOR_NAME_TYPES, "/contributor-name-types");
    apis.put(LOCATIONS, "/locations");
    apis.put(MATERIAL_TYPES, "/material-types");
    apis.put(INSTANCE_TYPES, "/instance-types");
    apis.put(INSTANCE_FORMATS, "/instance-formats");
    apis.put(ELECTRONIC_ACCESS_RELATIONSHIPS, "/electronic-access-relationships");
    apis.put(ALTERNATIVE_TITLE_TYPES, "/alternative-title-types");
    apis.put(LOAN_TYPES, "/loan-types");
    apis.put(ISSUANCE_MODES, "/modes-of-issuance");
    apis.put(HOLDING_NOTE_TYPES, "/holdings-note-types");
    apis.put(ITEM_NOTE_TYPES, "/item-note-types");

    apis.put(USERS, "/users");
    apis.put(CONFIGURATIONS, "/configurations/entries");

    EXTERNAL_APIS = Collections.unmodifiableMap(apis);
    EXTERNAL_APIS_WITH_PREFIX = Collections.unmodifiableMap(apis.entrySet()
      .stream()
      .collect(Collectors.toMap(Map.Entry::getKey, v -> "%s" + v.getValue())));
    EXTERNAL_APIS_WITH_SUFFIX = Collections.unmodifiableMap(apis.entrySet()
      .stream()
      .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue() + "/%s")));
    EXTERNAL_APIS_WITH_ID = Collections.unmodifiableMap(apis.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, v -> "%s" + v.getValue() + "/%s")));

  }

  public static String resourcesPath(String field) {
    return EXTERNAL_APIS.get(field);
  }

  public static String resourcesPathWithPrefix(String field) {
    return EXTERNAL_APIS_WITH_PREFIX.get(field);
  }

  public static String resourcesPathWithSuffix(String field) {
    return EXTERNAL_APIS_WITH_SUFFIX.get(field);
  }

  public static String resourcesPathWithId(String field) {
    return EXTERNAL_APIS_WITH_ID.get(field);
  }

}
