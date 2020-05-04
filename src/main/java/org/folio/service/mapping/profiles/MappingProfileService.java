package org.folio.service.mapping.profiles;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.json.JsonObject;
import org.folio.clients.InventoryClient;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;

public class MappingProfileService {

  private static final List<String> itemFieldKeys = ImmutableList.of(
    "electronicAccess.linkText",
    "electronicAccess.materialsSpecification",
    "electronicAccess.publicNote",
    "electronicAccess.uri",
    "electronicAccess.relationshipId",
    "materialTypeId"
  );

  private static final List<String> holdingsFieldKeys = ImmutableList.of(
    "electronicAccess.linkText",
    "electronicAccess.materialsSpecification",
    "electronicAccess.publicNote",
    "electronicAccess.uri",
    "electronicAccess.relationshipId",
    "permanentLocationId"
  );

  private static final Map<String, SettingsLoader> settingsLoaderMap = ImmutableMap.of(
    "electronicAccess.relationshipId", new ElectronicAccessRelationshipsLoader()
  );

  @Autowired
  private InventoryClient inventoryClient;
  private Properties messageProperties;

  public List<MappingFieldName> getMappingFieldNames(OkapiConnectionParams okapiConnectionParams) throws IOException {
    loadMessageProperties();
    List<MappingFieldName> fieldNames = new ArrayList<>();
    fieldNames.addAll(buildMappingFieldNames(RecordType.ITEM, itemFieldKeys, okapiConnectionParams));
    fieldNames.addAll(buildMappingFieldNames(RecordType.HOLDINGS, holdingsFieldKeys, okapiConnectionParams));
    return fieldNames;
  }

  private void loadMessageProperties() throws IOException {
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("messages/mapping_field_names.properties");
    Properties messageProperties = new Properties();
    messageProperties.load(inputStream);
    this.messageProperties = messageProperties;
  }

  private List<MappingFieldName> buildMappingFieldNames(RecordType recordType, List<String> fieldKeys, OkapiConnectionParams okapiConnectionParams) {
    List<MappingFieldName> fieldNames = new ArrayList<>();
    for (String fieldKey : fieldKeys) {
      if (settingsLoaderMap.containsKey(fieldKey)) {
        List<MappingFieldName> settingsFieldNames = buildSettingsFieldNames(recordType, okapiConnectionParams, fieldKey);
        fieldNames.addAll(settingsFieldNames);
      } else {
        fieldNames.add(buildSimpleFieldName(recordType, fieldKey));
      }
    }
    return fieldNames;
  }

  private List<MappingFieldName> buildSettingsFieldNames(RecordType recordType, OkapiConnectionParams okapiConnectionParams, String fieldKey) {
    Map<String, JsonObject> settingsEntries = settingsLoaderMap.get(fieldKey).load(okapiConnectionParams);
    List<MappingFieldName> subfieldNames = new ArrayList<>();
    for (Map.Entry<String, JsonObject> settingsEntry : settingsEntries.entrySet()) {
      MappingFieldName mappingFieldName = new MappingFieldName();
      mappingFieldName.setRecordType(recordType);
      mappingFieldName.setId(fieldKey);
      mappingFieldName.setPath(buildSettingsFieldPath(fieldKey, settingsEntry.getKey()));
      mappingFieldName.setDisplayName(buildSettingsDisplayName(recordType, fieldKey, settingsEntry.getValue()));
      subfieldNames.add(mappingFieldName);
    }
    return subfieldNames;
  }

  private MappingFieldName buildSimpleFieldName(RecordType recordType, String fieldKey) {
    MappingFieldName mappingFieldName = new MappingFieldName();
    mappingFieldName.setRecordType(recordType);
    mappingFieldName.setId(fieldKey);
    mappingFieldName.setPath(buildSimpleFieldPath(fieldKey));
    mappingFieldName.setDisplayName(buildSimpleDisplayName(recordType, fieldKey));
    return mappingFieldName;
  }

  private String buildSimpleFieldPath(String fieldKey) {
    List<String> fieldParts = Splitter.on(".").omitEmptyStrings().splitToList(fieldKey);
    if (fieldParts.size() == 1) {
      return fieldParts.get(0);
    } else {
      return "$." + fieldParts.get(0) + "[*]." + fieldParts.get(1);
    }
  }

  private String buildSettingsFieldPath(String fieldKey, String settingsId) {
    List<String> fieldParts = Splitter.on(".").omitEmptyStrings().splitToList(fieldKey);
    if (fieldParts.size() == 1) {
      return fieldParts.get(0);
    } else {
      return "$." + fieldParts.get(0) + "[?(@" + fieldParts.get(1) + " == '" + settingsId + "')]." + fieldParts.get(1);
    }
  }

  private String buildSimpleDisplayName(RecordType recordType, String fieldKey) {
    return new StringJoiner(" - ")
      .add(messageProperties.getProperty(recordType.toString().toLowerCase()))
      .add(messageProperties.getProperty(fieldKey))
      .toString();
  }

  private String buildSettingsDisplayName(RecordType recordType, String fieldKey, JsonObject settingsValue) {
    return new StringJoiner(" - ")
      .add(buildSimpleDisplayName(recordType, fieldKey))
      .add(settingsValue.getString("name"))
      .toString();
  }

}
