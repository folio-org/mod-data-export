package org.folio.service.fieldname;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.MapUtils;
import org.folio.rest.jaxrs.model.FieldName;
import org.folio.rest.jaxrs.model.FieldName.RecordType;
import org.folio.rest.jaxrs.model.FieldNameCollection;
import org.folio.service.fieldname.builder.DisplayNameKeyBuilder;
import org.folio.service.fieldname.builder.FieldIdBuilder;
import org.folio.service.fieldname.builder.PathBuilder;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.folio.rest.jaxrs.model.FieldName.RecordType.HOLDINGS;
import static org.folio.rest.jaxrs.model.FieldName.RecordType.INSTANCE;
import static org.folio.rest.jaxrs.model.FieldName.RecordType.ITEM;
import static org.folio.service.fieldname.FieldNameConfig.FIXED_LENGTH_DATA_ELEMENT;
import static org.folio.service.fieldname.FieldNameConfig.HR_ID;
import static org.folio.service.fieldname.FieldNameConfig.IDENTIFIERS;

@Service
public class FiledNamesServiceImpl implements FieldNamesService {

  public static final String SETTINGS_NAME_KEY = "name";
  private static final Set<FieldNameConfig> INSTANCE_FIELD_NAME_CONFIGS = EnumSet.of(
    HR_ID,
    IDENTIFIERS,
    FIXED_LENGTH_DATA_ELEMENT
  );
  private static final Set<FieldNameConfig> HOLDINGS_FIELD_NAME_CONFIGS = EnumSet.of(
    HR_ID
  );
  private static final Set<FieldNameConfig> ITEM_FIELD_NAME_CONFIGS = EnumSet.of(
    HR_ID
  );

  @Autowired
  private PathBuilder pathBuilder;
  @Autowired
  private DisplayNameKeyBuilder displayNameKeyBuilder;
  @Autowired
  private FieldIdBuilder fieldIdBuilder;

  @Override
  public Future<FieldNameCollection> getFieldNames(OkapiConnectionParams okapiConnectionParams) {
    Promise<FieldNameCollection> fieldNamesPromise = Promise.promise();
    List<FieldName> fieldNames = new ArrayList<>();
    fieldNames.addAll(buildFieldNames(INSTANCE, INSTANCE_FIELD_NAME_CONFIGS, okapiConnectionParams));
    fieldNames.addAll(buildFieldNames(HOLDINGS, HOLDINGS_FIELD_NAME_CONFIGS, okapiConnectionParams));
    fieldNames.addAll(buildFieldNames(ITEM, ITEM_FIELD_NAME_CONFIGS, okapiConnectionParams));
    fieldNamesPromise.complete(new FieldNameCollection().withFieldNames(fieldNames));
    return fieldNamesPromise.future();
  }

  private List<FieldName> buildFieldNames(RecordType recordType, Set<FieldNameConfig> fieldNameConfigs, OkapiConnectionParams okapiConnectionParams) {
    List<FieldName> fieldNames = new ArrayList<>();
    for (FieldNameConfig fieldNameConfig : fieldNameConfigs) {
      if (fieldNameConfig.isReferenceData()) {
        List<FieldName> settingsFieldNames = buildFieldNamesByReferenceData(recordType, okapiConnectionParams, fieldNameConfig);
        fieldNames.addAll(settingsFieldNames);
      } else {
        fieldNames.add(buildSimpleFieldNames(recordType, fieldNameConfig));
      }
    }
    return fieldNames;
  }

  private List<FieldName> buildFieldNamesByReferenceData(RecordType recordType, OkapiConnectionParams okapiConnectionParams, FieldNameConfig fieldNameConfig) {
    Map<String, JsonObject> referenceDataEntries = fieldNameConfig.getReferenceDataLoader().load(okapiConnectionParams);
    List<FieldName> subFieldNames = new ArrayList<>();
    for (Map.Entry<String, JsonObject> referenceDataEntry : referenceDataEntries.entrySet()) {
      String referenceDataValue = referenceDataEntry.getValue().getString(SETTINGS_NAME_KEY);
      FieldName fieldName = new FieldName();
      fieldName.setRecordType(recordType);
      fieldName.setPath(pathBuilder.build(recordType, fieldNameConfig, referenceDataEntry.getKey()));
      String formattedName = displayNameKeyBuilder.build(recordType, fieldNameConfig.getId());
      fieldName.setId(fieldIdBuilder.build(recordType, fieldNameConfig.getId(), referenceDataValue));
      fieldName.setDisplayNameKey(formattedName);
      fieldName.setReferenceDataValue(referenceDataValue);
      setMetadataParameters(fieldName, fieldNameConfig);
      subFieldNames.add(fieldName);
    }
    return subFieldNames;
  }

  private FieldName buildSimpleFieldNames(RecordType recordType, FieldNameConfig fieldNameConfig) {
    FieldName fieldName = new FieldName();
    fieldName.setRecordType(recordType);
    fieldName.setPath(pathBuilder.build(recordType, fieldNameConfig));
    fieldName.setDisplayNameKey(displayNameKeyBuilder.build(recordType, fieldNameConfig.getId()));
    fieldName.setId(fieldIdBuilder.build(recordType, fieldNameConfig.getId()));
    setMetadataParameters(fieldName, fieldNameConfig);
    return fieldName;
  }

  private void setMetadataParameters(FieldName fieldName, FieldNameConfig fieldNameConfig) {
    if ((MapUtils.isNotEmpty(fieldNameConfig.getMetadataParameters()))) {
      fieldName.setMetadataParameters(fieldNameConfig.getMetadataParameters());
    }
  }

}
