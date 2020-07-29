package org.folio.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.TestUtil;

import java.util.HashMap;
import java.util.Map;

import static org.folio.TestUtil.readFileContentFromResources;

public class ReferenceDataResponseUtil {

  public static Map<String, JsonObject> getNatureOfContentTerms() {
    JsonArray natureOfContentTermArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_nature_of_content_terms_response.json"))
        .getJsonArray("natureOfContentTerms");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : natureOfContentTermArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  public static Map<String, JsonObject> getIdentifierTypes() {
    JsonArray identifierTypesArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_identifier_types_response.json"))
        .getJsonArray("identifierTypes");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : identifierTypesArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  public static Map<String, JsonObject> getLocations() {
    JsonArray identifierTypesArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_locations_response.json"))
        .getJsonArray("locations");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : identifierTypesArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  public static Map<String, JsonObject> getContributorNameTypes() {
    JsonArray identifierTypesArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_contributor_name_types_response.json"))
        .getJsonArray("contributorNameTypes");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : identifierTypesArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  public static Map<String, JsonObject> getMaterialTypes() {
    JsonArray identifierTypesArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_material_types_response.json"))
        .getJsonArray("mtypes");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : identifierTypesArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  public static Map<String, JsonObject> getInstanceTypes() {
    JsonArray identifierTypesArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_instance_types_response.json"))
        .getJsonArray("instanceTypes");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : identifierTypesArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  public static Map<String, JsonObject> getInstanceFormats() {
    JsonArray identifierTypesArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_instance_formats_response.json"))
        .getJsonArray("instanceFormats");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : identifierTypesArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  public static Map<String, JsonObject> getElectronicAccessRelationships() {
    Map<String, JsonObject> stringJsonObjectMap = new HashMap<>();
    JsonArray electronicAccessRelationships =
      new JsonObject(TestUtil.readFileContentFromResources("mockData/inventory/get_electronic_access_relationships_response.json"))
        .getJsonArray("electronicAccessRelationships");
    electronicAccessRelationships.stream().forEach(electronicAccessRelationship -> {
      JsonObject jsonObject = new JsonObject(electronicAccessRelationship.toString());
      stringJsonObjectMap.put(jsonObject.getString("id"), jsonObject);
    });
    return stringJsonObjectMap;
  }
}
