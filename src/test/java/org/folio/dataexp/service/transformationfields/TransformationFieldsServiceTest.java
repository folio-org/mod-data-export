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
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.TransformationField;
import org.folio.dataexp.domain.dto.TransformationFieldCollection;
import org.folio.dataexp.util.ReferenceDataResponseUtil;
import org.folio.processor.referencedata.JsonObjectWrapper;
import org.folio.processor.referencedata.ReferenceDataWrapperImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
class TransformationFieldsServiceTest {
  private static final String TRANSFORMATION_FIELDS_MOCK_PATH = "src/test/resources/mockdata/expectedTransformationFields.json";

  @Mock
  private JsonPathBuilder pathBuilder;
  @Mock
  private FieldIdBuilder fieldIdBuilder;
  @Mock
  private DisplayNameKeyBuilder displayNameKeyBuilder;
  @Mock
  private ReferenceDataProvider referenceDataProvider;
  @InjectMocks
  private TransformationFieldsService transformationFieldsService;
  @Test
  @SneakyThrows
  void getFieldNamesShouldReturnValidFields() {
    var mapper = new ObjectMapper();
    var fieldsCollection = mapper.readValue(new FileInputStream(TRANSFORMATION_FIELDS_MOCK_PATH),
      TransformationFieldCollection.class);
    var expectedFields = fieldsCollection.getTransformationFields().stream()
      .collect(Collectors.toMap(TransformationField::getFieldId, Function.identity()));
    mocReferenceData();

    var res = transformationFieldsService.getTransformationFields();

    assertThat(res.getTransformationFields()).isNotEmpty();
    res.getTransformationFields().forEach(transformationField ->
      checkIfActualFieldEqualToExpected(expectedFields.get(transformationField.getFieldId()), transformationField));
  }

  private void mocReferenceData() throws IOException {
    HashMap<String, Map<String, JsonObjectWrapper>> map = new HashMap<>() ;
    map.put(IDENTIFIER_TYPES, ReferenceDataResponseUtil.getIdentifierTypes());
    map.put(ALTERNATIVE_TITLE_TYPES, ReferenceDataResponseUtil.getAlternativeTitleTypes());
    map.put(CONTRIBUTOR_NAME_TYPES, ReferenceDataResponseUtil.getContributorNameTypes());
    map.put(INSTANCE_TYPES, ReferenceDataResponseUtil.getInstanceTypes());
    map.put(ISSUANCE_MODES, ReferenceDataResponseUtil.getModeOfIssuance());
    map.put(MATERIAL_TYPES, ReferenceDataResponseUtil.getMaterialTypes());
    map.put(LOAN_TYPES, ReferenceDataResponseUtil.getLoanTypes());
    map.put(ELECTRONIC_ACCESS_RELATIONSHIPS, ReferenceDataResponseUtil.getElectronicAccessRelationships());
    map.put(HOLDING_NOTE_TYPES, ReferenceDataResponseUtil.getHoldingNoteTypes());
    map.put(ITEM_NOTE_TYPES, ReferenceDataResponseUtil.getItemNoteTypes());
    when(referenceDataProvider.getReferenceDataForTransformationFields()).thenReturn(new ReferenceDataWrapperImpl(map));

    doCallRealMethod().when(pathBuilder).build(any(RecordTypes.class), any(TransformationFieldsConfig.class));
    doCallRealMethod().when(pathBuilder).build(any(RecordTypes.class), any(TransformationFieldsConfig.class), any());
    doCallRealMethod().when(displayNameKeyBuilder).build(any(RecordTypes.class), anyString());
    doCallRealMethod().when(fieldIdBuilder).build(any(RecordTypes.class), anyString());
    doCallRealMethod().when(fieldIdBuilder).build(any(RecordTypes.class), anyString(), anyString());
  }

  void checkIfActualFieldEqualToExpected(TransformationField expectedField, TransformationField actualField) {
    assertEquals(expectedField.getFieldId(), actualField.getFieldId());
    assertEquals(expectedField.getDisplayNameKey(), actualField.getDisplayNameKey());
    assertEquals(expectedField.getPath(), actualField.getPath());
    assertEquals(expectedField.getReferenceDataValue(), actualField.getReferenceDataValue());
    assertEquals(expectedField.getMetadataParameters(), actualField.getMetadataParameters());
    assertEquals(expectedField.getRecordType(), actualField.getRecordType());
  }
}
