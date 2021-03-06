package org.folio.service.transformationfields;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.TransformationField;
import org.folio.rest.jaxrs.model.TransformationField.RecordType;
import org.folio.rest.jaxrs.model.TransformationFieldCollection;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.mapping.referencedata.ReferenceData;
import org.folio.service.mapping.referencedata.ReferenceDataProvider;
import org.folio.service.transformationfields.builder.DisplayNameKeyBuilder;
import org.folio.service.transformationfields.builder.FieldIdBuilder;
import org.folio.service.transformationfields.builder.PathBuilder;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.folio.HttpStatus.HTTP_UNPROCESSABLE_ENTITY;
import static org.folio.rest.jaxrs.model.TransformationField.RecordType.HOLDINGS;
import static org.folio.rest.jaxrs.model.TransformationField.RecordType.INSTANCE;
import static org.folio.rest.jaxrs.model.TransformationField.RecordType.ITEM;

@Service
public class TransformationFieldsServiceImpl implements TransformationFieldsService {

  private static final String REFERENCE_DATA_NAME_KEY = "name";
  private static final String ITEM_EMPTY_TRANSFORMATION_ERROR_MESSAGE = "Transformations for fields with item record type cannot be empty. Please provide a value.";

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
    transformationFields.addAll(buildTransformationFields(INSTANCE, TransformationConfigConstants.INSTANCE_FIELDS_CONFIGS, referenceData));
    transformationFields.addAll(buildTransformationFields(HOLDINGS, TransformationConfigConstants.HOLDINGS_FIELDS_CONFIGS, referenceData));
    transformationFields.addAll(buildTransformationFields(ITEM, TransformationConfigConstants.ITEM_FIELDS_CONFIGS, referenceData));
    transformationFields.sort(Comparator.comparing(TransformationField::getFieldId));
    promise.complete(new TransformationFieldCollection().withTransformationFields(transformationFields).withTotalRecords(transformationFields.size()));
    return promise.future();
  }

  @Override
  public Future<Void> validateTransformations(List<Transformations> transformations) {
    Promise<Void> promise = Promise.promise();
    Optional<Transformations> invalidTransformation = transformations.stream()
      .filter(elem -> StringUtils.isEmpty(elem.getTransformation()))
      .filter(elem -> elem.getRecordType().value().equals(ITEM.value()))
      .findFirst();
    if (invalidTransformation.isEmpty()) {
      promise.complete();
    } else {
      promise.fail(new ServiceException(HTTP_UNPROCESSABLE_ENTITY, ITEM_EMPTY_TRANSFORMATION_ERROR_MESSAGE));
    }
    return promise.future();
  }

  private List<TransformationField> buildTransformationFields(RecordType recordType, Set<TransformationFieldsConfig> transformationFieldsConfigs, ReferenceData referenceData) {
    List<TransformationField> transformationFields = new ArrayList<>();
    for (TransformationFieldsConfig transformationFieldsConfig : transformationFieldsConfigs) {
      if (transformationFieldsConfig.isReferenceData()) {
        List<TransformationField> fieldNamesWithReferenceData = buildTransformationFieldsByReferenceData(recordType, transformationFieldsConfig, referenceData);
        transformationFields.addAll(fieldNamesWithReferenceData);
      } else {
        transformationFields.add(buildSimpleTransformationFields(recordType, transformationFieldsConfig));
      }
    }
    return transformationFields;
  }

  private List<TransformationField> buildTransformationFieldsByReferenceData(RecordType recordType, TransformationFieldsConfig transformationFieldsConfig, ReferenceData referenceData) {
    Map<String, JsonObject> referenceDataEntries = referenceData.get(transformationFieldsConfig.getReferenceDataKey());
    List<TransformationField> subTransformationFields = new ArrayList<>();
    for (Map.Entry<String, JsonObject> referenceDataEntry : referenceDataEntries.entrySet()) {
      String referenceDataValue = referenceDataEntry.getValue().getString(REFERENCE_DATA_NAME_KEY);
      TransformationField transformationField = new TransformationField();
      transformationField.setRecordType(recordType);
      transformationField.setPath(pathBuilder.build(recordType, transformationFieldsConfig, referenceDataEntry));
      transformationField.setFieldId(fieldIdBuilder.build(recordType, transformationFieldsConfig.getFieldId(), referenceDataValue));
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
    transformationField.setFieldId(fieldIdBuilder.build(recordType, transformationFieldsConfig.getFieldId()));
    setMetadataParameters(transformationField, transformationFieldsConfig);
    return transformationField;
  }

  private void setMetadataParameters(TransformationField transformationField, TransformationFieldsConfig transformationFieldsConfig) {
    if ((MapUtils.isNotEmpty(transformationFieldsConfig.getMetadataParameters()))) {
      transformationField.setMetadataParameters(transformationFieldsConfig.getMetadataParameters());
    }
  }

}
