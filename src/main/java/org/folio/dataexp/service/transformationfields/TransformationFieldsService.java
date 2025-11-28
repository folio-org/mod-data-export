package org.folio.dataexp.service.transformationfields;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.dataexp.domain.dto.RecordTypes.HOLDINGS;
import static org.folio.dataexp.domain.dto.RecordTypes.INSTANCE;
import static org.folio.dataexp.domain.dto.RecordTypes.ITEM;
import static org.folio.dataexp.service.transformationfields.TransformationConfigConstants.HOLDINGS_FIELDS_CONFIGS;
import static org.folio.dataexp.service.transformationfields.TransformationConfigConstants.INSTANCE_FIELDS_CONFIGS;
import static org.folio.dataexp.service.transformationfields.TransformationConfigConstants.ITEM_FIELDS_CONFIGS;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.TransformationField;
import org.folio.dataexp.domain.dto.TransformationFieldCollection;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.exception.TransformationValidationException;
import org.folio.processor.referencedata.JsonObjectWrapper;
import org.folio.processor.referencedata.ReferenceDataWrapper;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

/** Service for building and validating transformation fields for export. */
@Service
@RequiredArgsConstructor
public class TransformationFieldsService {
  private static final String REFERENCE_DATA_NAME_KEY = "name";
  private static final String ITEM_EMPTY_TRANSFORMATION_ERROR_MESSAGE =
      "Transformations for fields with item record type cannot be empty. Please provide a value.";

  private final JsonPathBuilder pathBuilder;
  private final DisplayNameKeyBuilder displayNameKeyBuilder;
  private final FieldIdBuilder fieldIdBuilder;
  private final ReferenceDataProvider referenceDataProvider;
  private final FolioExecutionContext folioExecutionContext;

  /**
   * Gets all transformation fields for all record types.
   *
   * @return TransformationFieldCollection containing all fields
   */
  public TransformationFieldCollection getTransformationFields() {
    List<TransformationField> transformationFields = new ArrayList<>();
    var referenceData =
        referenceDataProvider.getReferenceDataForTransformationFields(
            folioExecutionContext.getTenantId());
    transformationFields.addAll(
        buildTransformationFields(INSTANCE, INSTANCE_FIELDS_CONFIGS, referenceData));
    transformationFields.addAll(
        buildTransformationFields(HOLDINGS, HOLDINGS_FIELDS_CONFIGS, referenceData));
    transformationFields.addAll(
        buildTransformationFields(ITEM, ITEM_FIELDS_CONFIGS, referenceData));
    transformationFields.sort(Comparator.comparing(TransformationField::getFieldId));
    return new TransformationFieldCollection()
        .transformationFields(transformationFields)
        .totalRecords(transformationFields.size());
  }

  /**
   * Validates that item transformations are not empty.
   *
   * @param transformations list of transformations
   * @throws TransformationValidationException if an item transformation is empty
   */
  public void validateTransformations(List<Transformations> transformations) {
    var invalidTransformation =
        transformations.stream()
            .filter(elem -> StringUtils.isEmpty(elem.getTransformation()))
            .filter(elem -> ITEM.equals(elem.getRecordType()))
            .findFirst();
    if (invalidTransformation.isPresent()) {
      throw new TransformationValidationException(ITEM_EMPTY_TRANSFORMATION_ERROR_MESSAGE);
    }
  }

  /**
   * Builds transformation fields for a record type.
   *
   * @param recordType the record type
   * @param transformationFieldsConfigs set of transformation field configs
   * @param wrapper reference data wrapper
   * @return list of transformation fields
   */
  private List<TransformationField> buildTransformationFields(
      RecordTypes recordType,
      Set<TransformationFieldsConfig> transformationFieldsConfigs,
      ReferenceDataWrapper wrapper) {
    List<TransformationField> transformationFields = new ArrayList<>();
    for (TransformationFieldsConfig transformationFieldsConfig : transformationFieldsConfigs) {
      if (transformationFieldsConfig.isReferenceData()) {
        List<TransformationField> fieldNamesWithReferenceData =
            buildTransformationFieldsByReferenceData(
                recordType, transformationFieldsConfig, wrapper);
        transformationFields.addAll(fieldNamesWithReferenceData);
      } else {
        transformationFields.add(
            buildSimpleTransformationFields(recordType, transformationFieldsConfig));
      }
    }
    return transformationFields;
  }

  /**
   * Builds a simple transformation field (not reference data).
   *
   * @param recordType the record type
   * @param transformationFieldsConfig the transformation field config
   * @return TransformationField
   */
  private TransformationField buildSimpleTransformationFields(
      RecordTypes recordType, TransformationFieldsConfig transformationFieldsConfig) {
    return new TransformationField()
        .recordType(recordType)
        .path(pathBuilder.build(recordType, transformationFieldsConfig))
        .displayNameKey(
            displayNameKeyBuilder.build(recordType, transformationFieldsConfig.getFieldId()))
        .fieldId(fieldIdBuilder.build(recordType, transformationFieldsConfig.getFieldId()))
        .metadataParameters(
            isEmpty(transformationFieldsConfig.getMetadataParameters())
                ? null
                : transformationFieldsConfig.getMetadataParameters());
  }

  /**
   * Builds transformation fields for reference data.
   *
   * @param recordType the record type
   * @param transformationFieldsConfig the transformation field config
   * @param wrapper reference data wrapper
   * @return list of TransformationField
   */
  private List<TransformationField> buildTransformationFieldsByReferenceData(
      RecordTypes recordType,
      TransformationFieldsConfig transformationFieldsConfig,
      ReferenceDataWrapper wrapper) {
    var referenceDataEntries = wrapper.get(transformationFieldsConfig.getReferenceDataKey());
    List<TransformationField> subTransformationFields = new ArrayList<>();
    for (Map.Entry<String, JsonObjectWrapper> referenceDataEntry :
        referenceDataEntries.entrySet()) {
      var referenceDataValue =
          referenceDataEntry.getValue().getMap().get(REFERENCE_DATA_NAME_KEY).toString();
      var transformationField =
          new TransformationField()
              .recordType(recordType)
              .path(pathBuilder.build(recordType, transformationFieldsConfig, referenceDataEntry))
              .fieldId(
                  fieldIdBuilder.build(
                      recordType, transformationFieldsConfig.getFieldId(), referenceDataValue))
              .displayNameKey(
                  displayNameKeyBuilder.build(recordType, transformationFieldsConfig.getFieldId()))
              .referenceDataValue(referenceDataValue)
              .metadataParameters(
                  isEmpty(transformationFieldsConfig.getMetadataParameters())
                      ? null
                      : transformationFieldsConfig.getMetadataParameters());
      subTransformationFields.add(transformationField);
    }
    return subTransformationFields;
  }
}
