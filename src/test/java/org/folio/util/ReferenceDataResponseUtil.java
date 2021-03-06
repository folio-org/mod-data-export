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
    JsonArray locationsArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_locations_response.json"))
        .getJsonArray("locations");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : locationsArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  public static Map<String, JsonObject> getLibraries() {
    JsonArray librariesArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_libraries_response.json"))
        .getJsonArray("loclibs");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : librariesArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  public static Map<String, JsonObject> getCampuses() {
    JsonArray campusesArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_campuses_response.json"))
        .getJsonArray("loccamps");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : campusesArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  public static Map<String, JsonObject> getCallNumberTypes() {
    JsonArray callNumberTypes =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_call_number_types_response.json"))
        .getJsonArray("callNumberTypes");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : callNumberTypes) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  public static Map<String, JsonObject> getInstitutions() {
    JsonArray institutionsArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_institutions_response.json"))
        .getJsonArray("locinsts");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : institutionsArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  public static Map<String, JsonObject> getContributorNameTypes() {
    JsonArray contributorNameArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_contributor_name_types_response.json"))
        .getJsonArray("contributorNameTypes");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : contributorNameArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  public static Map<String, JsonObject> getMaterialTypes() {
    JsonArray materialTypeArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_material_types_response.json"))
        .getJsonArray("mtypes");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : materialTypeArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  public static Map<String, JsonObject> getInstanceTypes() {
    JsonArray instanceTypesArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_instance_types_response.json"))
        .getJsonArray("instanceTypes");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : instanceTypesArray) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      map.put(jsonObject.getString("id"), jsonObject);
    }
    return map;
  }

  public static Map<String, JsonObject> getInstanceFormats() {
    JsonArray instanceFormatsArray =
      new JsonObject(readFileContentFromResources("mockData/inventory/get_instance_formats_response.json"))
        .getJsonArray("instanceFormats");
    Map<String, JsonObject> map = new HashMap<>();
    for (Object object : instanceFormatsArray) {
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

  public static Map<String, JsonObject> getAlternativeTitleTypes() {
    Map<String, JsonObject> stringJsonObjectMap = new HashMap<>();
    JsonArray alternativeTitleTypes =
      new JsonObject(TestUtil.readFileContentFromResources("mockData/inventory/get_alternative_titles_response.json"))
        .getJsonArray("alternativeTitleTypes");
    alternativeTitleTypes.stream().forEach(alternativeTitleType -> {
      JsonObject jsonObject = new JsonObject(alternativeTitleType.toString());
      stringJsonObjectMap.put(jsonObject.getString("id"), jsonObject);
    });
    return stringJsonObjectMap;
  }

  public static Map<String, JsonObject> getModeOfIssuance() {
    Map<String, JsonObject> stringJsonObjectMap = new HashMap<>();
    JsonArray modeOfIssuances =
      new JsonObject(TestUtil.readFileContentFromResources("mockData/inventory/get_mode_of_issuance_response.json"))
        .getJsonArray("issuanceModes");
    modeOfIssuances.stream().forEach(modeOfIssuance -> {
      JsonObject jsonObject = new JsonObject(modeOfIssuance.toString());
      stringJsonObjectMap.put(jsonObject.getString("id"), jsonObject);
    });
    return stringJsonObjectMap;
  }

  public static Map<String, JsonObject> getLoanTypes() {
    Map<String, JsonObject> stringJsonObjectMap = new HashMap<>();
    JsonArray loantypes =
      new JsonObject(TestUtil.readFileContentFromResources("mockData/inventory/get_loan_types_response.json"))
        .getJsonArray("loantypes");
    loantypes.stream().forEach(loanType -> {
      JsonObject jsonObject = new JsonObject(loanType.toString());
      stringJsonObjectMap.put(jsonObject.getString("id"), jsonObject);
    });
    return stringJsonObjectMap;
  }

  public static Map<String, JsonObject> getHoldingNoteTypes() {
    Map<String, JsonObject> stringJsonObjectMap = new HashMap<>();
    JsonArray holdingNoteTypes = new JsonObject(TestUtil.readFileContentFromResources("mockData/inventory/get_holding_note_types_response.json"))
        .getJsonArray("holdingsNoteTypes");
    holdingNoteTypes.stream().forEach(type -> {
      JsonObject jsonObject = new JsonObject(type.toString());
      stringJsonObjectMap.put(jsonObject.getString("id"), jsonObject);
    });
    return stringJsonObjectMap;
  }

  public static Map<String, JsonObject> getItemNoteTypes() {
    Map<String, JsonObject> stringJsonObjectMap = new HashMap<>();
    JsonArray itemNoteTypes = new JsonObject(TestUtil.readFileContentFromResources("mockData/inventory/get_item_note_types_response.json"))
      .getJsonArray("itemNoteTypes");
    itemNoteTypes.stream().forEach(type -> {
      JsonObject jsonObject = new JsonObject(type.toString());
      stringJsonObjectMap.put(jsonObject.getString("id"), jsonObject);
    });
    return stringJsonObjectMap;
  }

}
