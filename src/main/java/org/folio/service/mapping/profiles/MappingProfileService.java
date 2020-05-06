package org.folio.service.mapping.profiles;

import io.vertx.core.json.JsonObject;
import org.folio.service.mapping.profiles.builder.PathBuilder;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.folio.service.mapping.profiles.RecordType.HOLDINGS;
import static org.folio.service.mapping.profiles.RecordType.ITEM;
import static org.folio.service.mapping.profiles.TransformationConfig.ELECTRONIC_ACCESS_LINK_TEXT;
import static org.folio.service.mapping.profiles.TransformationConfig.ELECTRONIC_ACCESS_MATERIALS_SPECIFICATION;
import static org.folio.service.mapping.profiles.TransformationConfig.ELECTRONIC_ACCESS_PUBLIC_NOTE;
import static org.folio.service.mapping.profiles.TransformationConfig.ELECTRONIC_ACCESS_RELATIONSHIP_ID;
import static org.folio.service.mapping.profiles.TransformationConfig.ELECTRONIC_ACCESS_URI;
import static org.folio.service.mapping.profiles.TransformationConfig.MATERIAL_TYPE_ID;
import static org.folio.service.mapping.profiles.TransformationConfig.PERMANENT_LOCATION_ID;

public class MappingProfileService {

  private static final Set<TransformationConfig> itemTransformationConfigs = EnumSet.of(
    MATERIAL_TYPE_ID,
    ELECTRONIC_ACCESS_LINK_TEXT,
    ELECTRONIC_ACCESS_MATERIALS_SPECIFICATION,
    ELECTRONIC_ACCESS_PUBLIC_NOTE,
    ELECTRONIC_ACCESS_URI,
    ELECTRONIC_ACCESS_RELATIONSHIP_ID
  );

  private static final Set<TransformationConfig> holdingsTransformationConfigs = EnumSet.of(
    PERMANENT_LOCATION_ID,
    ELECTRONIC_ACCESS_LINK_TEXT,
    ELECTRONIC_ACCESS_MATERIALS_SPECIFICATION,
    ELECTRONIC_ACCESS_PUBLIC_NOTE,
    ELECTRONIC_ACCESS_URI,
    ELECTRONIC_ACCESS_RELATIONSHIP_ID
  );

  public static final String SETTINGS_NAME_KEY = "name";

  @Autowired
  private PathBuilder pathBuilder;
  @Autowired
  private TrasnformationNameFormatter nameFormatter;

  public List<MappingProfileTransformation> getMappingProfileTrasnformations(OkapiConnectionParams okapiConnectionParams) throws IOException {
    List<MappingProfileTransformation> fieldNames = new ArrayList<>();
    fieldNames.addAll(buildTransformations(ITEM, itemTransformationConfigs, okapiConnectionParams));
    fieldNames.addAll(buildTransformations(HOLDINGS, holdingsTransformationConfigs, okapiConnectionParams));
    return fieldNames;
  }

  private List<MappingProfileTransformation> buildTransformations(RecordType recordType, Set<TransformationConfig> transformationConfigs, OkapiConnectionParams okapiConnectionParams) {
    List<MappingProfileTransformation> fieldNames = new ArrayList<>();
    for (TransformationConfig transformationConfig : transformationConfigs) {
      if (transformationConfig.isSettings()) {
        List<MappingProfileTransformation> settingsFieldNames = buildTransformationsBySettings(recordType, okapiConnectionParams, transformationConfig);
        fieldNames.addAll(settingsFieldNames);
      } else {
        fieldNames.add(buildSimpleTransformation(recordType, transformationConfig));
      }
    }
    return fieldNames;
  }

  private List<MappingProfileTransformation> buildTransformationsBySettings(RecordType recordType, OkapiConnectionParams okapiConnectionParams, TransformationConfig transformationConfig) {
    Map<String, JsonObject> settingsEntries = transformationConfig.getSettingsLoader().load(okapiConnectionParams);
    List<MappingProfileTransformation> subfieldNames = new ArrayList<>();
    for (Map.Entry<String, JsonObject> settingsEntry : settingsEntries.entrySet()) {
      MappingProfileTransformation mappingProfileTransformation = new MappingProfileTransformation();
      mappingProfileTransformation.setRecordType(recordType);
      mappingProfileTransformation.setId(transformationConfig.getId());
      mappingProfileTransformation.setPath(pathBuilder.build(recordType, transformationConfig, settingsEntry.getKey()));
      String formattedName = nameFormatter.format(recordType, transformationConfig.getFormattedName(), settingsEntry.getValue().getString(SETTINGS_NAME_KEY));
      mappingProfileTransformation.setDisplayName(formattedName);
      subfieldNames.add(mappingProfileTransformation);
    }
    return subfieldNames;
  }

  private MappingProfileTransformation buildSimpleTransformation(RecordType recordType, TransformationConfig transformationConfig) {
    MappingProfileTransformation mappingProfileTransformation = new MappingProfileTransformation();
    mappingProfileTransformation.setRecordType(recordType);
    mappingProfileTransformation.setId(transformationConfig.getId());
    mappingProfileTransformation.setPath(pathBuilder.build(recordType, transformationConfig));
    mappingProfileTransformation.setDisplayName(nameFormatter.format(recordType, transformationConfig.getFormattedName()));
    return mappingProfileTransformation;
  }


}
