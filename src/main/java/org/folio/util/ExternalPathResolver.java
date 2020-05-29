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
  public static final String USERS = "users";
  public static final String HOLDING = "holding";
  public static final String ITEM = "item";


  private static final Map<String, String> EXTERNAL_APIS;
  private static final Map<String, String> EXTERNAL_APIS_WITH_PREFIX;
  private static final Map<String, String> EXTERNAL_APIS_WITH_ID;

  static {
    Map<String, String> apis = new HashMap<>();
    apis.put(SRS, "/source-storage/records");
    apis.put(INSTANCE, "/instance-storage/instances");
    apis.put(HOLDING, "/holdings-storage/holdings");
    apis.put(ITEM, "/item-storage/items");
    apis.put(CONTENT_TERMS, "/nature-of-content-terms");
    apis.put(IDENTIFIER_TYPES, "/identifier-types");
    apis.put(CONTRIBUTOR_NAME_TYPES, "/contributor-name-types");
    apis.put(USERS, "/users");

    EXTERNAL_APIS = Collections.unmodifiableMap(apis);
    EXTERNAL_APIS_WITH_PREFIX = Collections.unmodifiableMap(apis.entrySet()
      .stream()
      .collect(Collectors.toMap(Map.Entry::getKey, v -> "%s" + v.getValue())));
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

  public static String resourcesPathWithId(String field) {
    return EXTERNAL_APIS_WITH_ID.get(field);
  }

}
