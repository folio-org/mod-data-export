package org.folio.service.fieldname;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.MapUtils;
import org.folio.processor.ReferenceData;
import org.folio.rest.jaxrs.model.TransformationField;
import org.folio.rest.jaxrs.model.TransformationField.RecordType;
import org.folio.rest.jaxrs.model.TransformationFieldCollection;
import org.folio.service.fieldname.builder.DisplayNameKeyBuilder;
import org.folio.service.fieldname.builder.FieldIdBuilder;
import org.folio.service.fieldname.builder.PathBuilder;
import org.folio.service.mapping.referencedata.ReferenceDataProvider;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.folio.rest.jaxrs.model.TransformationField.RecordType.HOLDINGS;
import static org.folio.rest.jaxrs.model.TransformationField.RecordType.INSTANCE;
import static org.folio.rest.jaxrs.model.TransformationField.RecordType.ITEM;
import static org.folio.service.fieldname.TransformationFieldsConfig.HR_ID;
import static org.folio.service.fieldname.TransformationFieldsConfig.IDENTIFIERS;

@Service
public class TransformationFieldsServiceImpl implements TransformationFieldsService {

  public static final String REFERENCE_DATA_NAME_KEY = "name";
  private static final Set<TransformationFieldsConfig> INSTANCE_FIELD_NAME_CONFIGS = EnumSet.of(
    HR_ID,
    IDENTIFIERS
  );
  private static final Set<TransformationFieldsConfig> HOLDINGS_FIELD_NAME_CONFIGS = EnumSet.of(
    HR_ID
  );
  private static final Set<TransformationFieldsConfig> ITEM_FIELD_NAME_CONFIGS = EnumSet.of(
    HR_ID
  );

  @Autowired
  private PathBuilder pathBuilder;
  @Autowired
  private DisplayNameKeyBuilder displayNameKeyBuilder;
  @Autowired
  private FieldIdBuilder fieldIdBuilder;
  @Autowired
  private ReferenceDataProvider referenceDataProvider;

  @Override
  public Future<TransformationFieldCollection> getTransformationFields(OkapiConnectionParams okapiConnectionParams) {
    Promise<TransformationFieldCollection> promise = Promise.promise();
    List<TransformationField> transformationFields = new ArrayList<>();
    ReferenceData referenceData = referenceDataProvider.getReferenceDataForTransformationFields(okapiConnectionParams);
    transformationFields.addAll(buildTransformationFields(INSTANCE, INSTANCE_FIELD_NAME_CONFIGS, referenceData, okapiConnectionParams));
    transformationFields.addAll(buildTransformationFields(HOLDINGS, HOLDINGS_FIELD_NAME_CONFIGS, referenceData, okapiConnectionParams));
    transformationFields.addAll(buildTransformationFields(ITEM, ITEM_FIELD_NAME_CONFIGS, referenceData, okapiConnectionParams));
    promise.complete(new TransformationFieldCollection().withTransformationFields(transformationFields));
    return promise.future();
  }

  private List<TransformationField> buildTransformationFields(RecordType recordType, Set<TransformationFieldsConfig> transformationFieldsConfigs, ReferenceData referenceData, OkapiConnectionParams okapiConnectionParams) {
    List<TransformationField> transformationFields = new ArrayList<>();
    for (TransformationFieldsConfig transformationFieldsConfig : transformationFieldsConfigs) {
      if (transformationFieldsConfig.isReferenceData()) {
        List<TransformationField> fieldNamesWithReferenceData = buildTransformationFieldsByReferenceData(recordType, transformationFieldsConfig, referenceData, okapiConnectionParams);
        transformationFields.addAll(fieldNamesWithReferenceData);
      } else {
        transformationFields.add(buildSimpleTransformationFields(recordType, transformationFieldsConfig));
      }
    }
    return transformationFields;
  }

  private List<TransformationField> buildTransformationFieldsByReferenceData(RecordType recordType, TransformationFieldsConfig transformationFieldsConfig, ReferenceData referenceData, OkapiConnectionParams okapiConnectionParams) {
    Map<String, JsonObject> referenceDataEntries = referenceData.get(transformationFieldsConfig.getReferenceDataKey());
    List<TransformationField> subTransformationFields = new ArrayList<>();
    for (Map.Entry<String, JsonObject> referenceDataEntry : referenceDataEntries.entrySet()) {
      String referenceDataValue = referenceDataEntry.getValue().getString(REFERENCE_DATA_NAME_KEY);
      TransformationField transformationField = new TransformationField();
      transformationField.setRecordType(recordType);
      transformationField.setPath(pathBuilder.build(recordType, transformationFieldsConfig, referenceDataEntry.getKey()));
      transformationField.setRuleId(fieldIdBuilder.build(recordType, transformationFieldsConfig.getFieldId(), referenceDataValue));
      transformationField.setDisplayNameKey(displayNameKeyBuilder.build(recordType, transformationFieldsConfig.getFieldId()));
      transformationField.setReferenceDataValue(referenceDataValue);
      setMetadataParameters(transformationField, transformationFieldsConfig);
      subTransformationFields.add(transformationField);
    }
    return subTransformationFields;
  }

  private TransformationField buildSimpleTransformationFields(RecordType recordType, TransformationFieldsConfig transformationFieldsConfig) {
    TransformationField transformationField = new TransformationField();
    transformationField.setRecordType(recordType);
    transformationField.setPath(pathBuilder.build(recordType, transformationFieldsConfig));
    transformationField.setDisplayNameKey(displayNameKeyBuilder.build(recordType, transformationFieldsConfig.getFieldId()));
    transformationField.setRuleId(fieldIdBuilder.build(recordType, transformationFieldsConfig.getFieldId()));
    setMetadataParameters(transformationField, transformationFieldsConfig);
    return transformationField;
  }

  private void setMetadataParameters(TransformationField transformationField, TransformationFieldsConfig transformationFieldsConfig) {
    if ((MapUtils.isNotEmpty(transformationFieldsConfig.getMetadataParameters()))) {
      transformationField.setMetadataParameters(transformationFieldsConfig.getMetadataParameters());
    }
  }

}
