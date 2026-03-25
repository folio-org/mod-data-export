package org.folio.dataexp.service.transformationfields;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.util.ExternalPathResolver.ALTERNATIVE_TITLE_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.CONTRIBUTOR_NAME_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.ELECTRONIC_ACCESS_RELATIONSHIPS;
import static org.folio.dataexp.util.ExternalPathResolver.HOLDING_NOTE_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.IDENTIFIER_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.INSTANCE_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.ISSUANCE_MODES;
import static org.folio.dataexp.util.ExternalPathResolver.ITEM_NOTE_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.LOAN_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.MATERIAL_TYPES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.TransformationField;
import org.folio.dataexp.domain.dto.TransformationFieldCollection;
import org.folio.dataexp.util.ReferenceDataResponseUtil;
import org.folio.processor.referencedata.JsonObjectWrapper;
import org.folio.processor.referencedata.ReferenceDataWrapperImpl;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.exception.TransformationValidationException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.folio.dataexp.service.transformationfields.DisplayNameKeyBuilder;
import org.folio.dataexp.service.transformationfields.FieldIdBuilder;
import org.folio.dataexp.service.transformationfields.JsonPathBuilder;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.dataexp.service.transformationfields.TransformationFieldsService;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class TransformationFieldsServiceTest {
  private static final String TRANSFORMATION_FIELDS_MOCK_PATH =
      "src/test/resources/mockdata/expectedTransformationFields.json";

  @Mock private JsonPathBuilder pathBuilder;
  @Mock private FieldIdBuilder fieldIdBuilder;
  @Mock private DisplayNameKeyBuilder displayNameKeyBuilder;
  @Mock private ReferenceDataProvider referenceDataProvider;
  @Mock private FolioExecutionContext folioExecutionContext;
  @InjectMocks private TransformationFieldsService transformationFieldsService;

  @Test
  @SneakyThrows
  void getFieldNamesShouldReturnValidFields() {
    mocReferenceData();
    when(folioExecutionContext.getTenantId()).thenReturn("tenantId");

    var res = transformationFieldsService.getTransformationFields();

    assertThat(res.getTransformationFields()).isNotEmpty();
    var mapper = new ObjectMapper();
    var fieldsCollection =
        mapper.readValue(
            new FileInputStream(TRANSFORMATION_FIELDS_MOCK_PATH),
            TransformationFieldCollection.class);
    var expectedFields =
        fieldsCollection.getTransformationFields().stream()
            .collect(Collectors.toMap(TransformationField::getFieldId, Function.identity()));
    res.getTransformationFields()
        .forEach(
            transformationField ->
                checkIfActualFieldEqualToExpected(
                    expectedFields.get(transformationField.getFieldId()), transformationField));
  }

    @Test
  void validateTransformationsShouldThrowExceptionWhenItemTransformationIsEmpty() {
    // TestMate-b424022897ffd88ebe709e492aa95807
    // Given
    var invalidItemTransformation = new Transformations();
    invalidItemTransformation.setRecordType(RecordTypes.ITEM);
    invalidItemTransformation.setTransformation("");
    var validInstanceTransformation = new Transformations();
    validInstanceTransformation.setRecordType(RecordTypes.INSTANCE);
    validInstanceTransformation.setTransformation("");
    var transformations = List.of(validInstanceTransformation, invalidItemTransformation);
    // When
    var exception = assertThrows(TransformationValidationException.class, () ->
        transformationFieldsService.validateTransformations(transformations));
    // Then
    assertEquals("Transformations for fields with item record type cannot be empty. Please provide a value.", exception.getMessage());
  }

    @Test
  void validateTransformationsShouldPassWhenItemTransformationIsNotEmpty() {
    // TestMate-b7fa61c11a7f0239fa46089ddcf3fcd1
    // Given
    var validItemTransformation = new Transformations();
    validItemTransformation.setRecordType(RecordTypes.ITEM);
    validItemTransformation.setTransformation("100  $a");
    var emptyInstanceTransformation = new Transformations();
    emptyInstanceTransformation.setRecordType(RecordTypes.INSTANCE);
    emptyInstanceTransformation.setTransformation("");
    var transformations = List.of(validItemTransformation, emptyInstanceTransformation);
    // When & Then
    assertDoesNotThrow(() -> transformationFieldsService.validateTransformations(transformations));
  }

    @Test
void validateTransformationsShouldIgnoreEmptyTransformationsForNonItemTypes() {
  // TestMate-1b0355446d359fc1b056762377f19c1a
  // Given
  var emptyInstanceTransformation = new Transformations();
  emptyInstanceTransformation.setRecordType(RecordTypes.INSTANCE);
  emptyInstanceTransformation.setTransformation("");
  var nullHoldingsTransformation = new Transformations();
  nullHoldingsTransformation.setRecordType(RecordTypes.HOLDINGS);
  nullHoldingsTransformation.setTransformation(null);
  var transformations = List.of(emptyInstanceTransformation, nullHoldingsTransformation);
  // When & Then
  assertDoesNotThrow(() -> transformationFieldsService.validateTransformations(transformations));
}

    @Test
  void validateTransformationsShouldHandleEmptyList() {
    // TestMate-682ecc2f93c89abafcccc937415dba3a
    // Given
    List<Transformations> transformations = Collections.emptyList();
    // When & Then
    assertDoesNotThrow(() -> transformationFieldsService.validateTransformations(transformations));
  }

    @Test
  void validateTransformationsShouldThrowExceptionOnFirstInvalidItemInMixedList() {
    // TestMate-0adc210f6e35bb7516de13f92e4f44de
    // Given
    var validInstanceTransformation = new Transformations();
    validInstanceTransformation.setRecordType(RecordTypes.INSTANCE);
    validInstanceTransformation.setTransformation("");
    var validItemTransformation = new Transformations();
    validItemTransformation.setRecordType(RecordTypes.ITEM);
    validItemTransformation.setTransformation("123");
    var invalidItemTransformation = new Transformations();
    invalidItemTransformation.setRecordType(RecordTypes.ITEM);
    invalidItemTransformation.setTransformation("");
    var validHoldingsTransformation = new Transformations();
    validHoldingsTransformation.setRecordType(RecordTypes.HOLDINGS);
    validHoldingsTransformation.setTransformation("456");
    var transformations = List.of(
        validInstanceTransformation,
        validItemTransformation,
        invalidItemTransformation,
        validHoldingsTransformation
    );
    // When
    var exception = assertThrows(TransformationValidationException.class, () ->
        transformationFieldsService.validateTransformations(transformations));
    // Then
    assertEquals("Transformations for fields with item record type cannot be empty. Please provide a value.", exception.getMessage());
  }

  private void mocReferenceData() {
    HashMap<String, Map<String, JsonObjectWrapper>> map = new HashMap<>();
    map.put(IDENTIFIER_TYPES, ReferenceDataResponseUtil.getIdentifierTypes());
    map.put(ALTERNATIVE_TITLE_TYPES, ReferenceDataResponseUtil.getAlternativeTitleTypes());
    map.put(CONTRIBUTOR_NAME_TYPES, ReferenceDataResponseUtil.getContributorNameTypes());
    map.put(INSTANCE_TYPES, ReferenceDataResponseUtil.getInstanceTypes());
    map.put(ISSUANCE_MODES, ReferenceDataResponseUtil.getModeOfIssuance());
    map.put(MATERIAL_TYPES, ReferenceDataResponseUtil.getMaterialTypes());
    map.put(LOAN_TYPES, ReferenceDataResponseUtil.getLoanTypes());
    map.put(
        ELECTRONIC_ACCESS_RELATIONSHIPS,
        ReferenceDataResponseUtil.getElectronicAccessRelationships());
    map.put(HOLDING_NOTE_TYPES, ReferenceDataResponseUtil.getHoldingNoteTypes());
    map.put(ITEM_NOTE_TYPES, ReferenceDataResponseUtil.getItemNoteTypes());
    when(referenceDataProvider.getReferenceDataForTransformationFields(isA(String.class)))
        .thenReturn(new ReferenceDataWrapperImpl(map));

    doCallRealMethod()
        .when(pathBuilder)
        .build(any(RecordTypes.class), any(TransformationFieldsConfig.class));
    doCallRealMethod()
        .when(pathBuilder)
        .build(any(RecordTypes.class), any(TransformationFieldsConfig.class), any());
    doCallRealMethod().when(displayNameKeyBuilder).build(any(RecordTypes.class), anyString());
    doCallRealMethod().when(fieldIdBuilder).build(any(RecordTypes.class), anyString());
    doCallRealMethod().when(fieldIdBuilder).build(any(RecordTypes.class), anyString(), anyString());
  }

  void checkIfActualFieldEqualToExpected(
      TransformationField expectedField, TransformationField actualField) {
    assertEquals(expectedField.getFieldId(), actualField.getFieldId());
    assertEquals(expectedField.getDisplayNameKey(), actualField.getDisplayNameKey());
    assertEquals(expectedField.getPath(), actualField.getPath());
    assertEquals(expectedField.getReferenceDataValue(), actualField.getReferenceDataValue());
    assertEquals(expectedField.getMetadataParameters(), actualField.getMetadataParameters());
    assertEquals(expectedField.getRecordType(), actualField.getRecordType());
  }
}
