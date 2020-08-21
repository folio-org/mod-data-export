package org.folio.service.transformationfields;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.processor.ReferenceData;
import org.folio.rest.jaxrs.model.TransformationField;
import org.folio.rest.jaxrs.model.TransformationField.RecordType;
import org.folio.rest.jaxrs.model.TransformationFieldCollection;
import org.folio.service.mapping.referencedata.ReferenceDataImpl;
import org.folio.service.mapping.referencedata.ReferenceDataProvider;
import org.folio.service.transformationfields.builder.DisplayNameKeyBuilderImpl;
import org.folio.service.transformationfields.builder.FieldIdBuilderImpl;
import org.folio.service.transformationfields.builder.JsonPathBuilder;
import org.folio.util.OkapiConnectionParams;
import org.folio.util.ReferenceDataResponseUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.folio.TestUtil.readFileContentFromResources;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.ALTERNATIVE_TITLE_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.CONTRIBUTOR_NAME_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.ELECTRONIC_ACCESS_RELATIONSHIPS;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.HOLDING_NOTE_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.IDENTIFIER_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.INSTANCE_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.ITEM_NOTE_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.LOAN_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.MATERIAL_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.MODES_OF_ISSUANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class TransformationFieldsServiceUnitTest {

  private static final String EXPECTED_TRANSFORMATION_FIELDS_RESPONSE_PATH = "mapping/expectedTransformationFields.json";
  private static final String TRANSFORMATION_FIELDS = "transformationFields";
  private static final String FIELD_ID = "fieldId";
  private static final String TENANT_ID = "diku";
  private final OkapiConnectionParams okapiConnectionParams;
  @Mock
  private JsonPathBuilder pathBuilder;
  @Mock
  private FieldIdBuilderImpl fieldIdBuilder;
  @Mock
  private DisplayNameKeyBuilderImpl displayNameKeyBuilder;
  @Mock
  private ReferenceDataProvider referenceDataProvider;
  @Spy
  @InjectMocks
  private TransformationFieldsServiceImpl fieldNamesService;
  private Map<String, TransformationField> expectedFields;

  TransformationFieldsServiceUnitTest() {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    okapiConnectionParams = new OkapiConnectionParams(headers);
    expectedFields = initializeExpectedTranslationFieldsResponse();
  }

  @BeforeEach
  void before() {
    ReferenceData referenceData = new ReferenceDataImpl();
    referenceData.put(IDENTIFIER_TYPES, ReferenceDataResponseUtil.getIdentifierTypes());
    referenceData.put(ALTERNATIVE_TITLE_TYPES, ReferenceDataResponseUtil.getAlternativeTitleTypes());
    referenceData.put(CONTRIBUTOR_NAME_TYPES, ReferenceDataResponseUtil.getContributorNameTypes());
    referenceData.put(INSTANCE_TYPES, ReferenceDataResponseUtil.getInstanceTypes());
    referenceData.put(MODES_OF_ISSUANCE, ReferenceDataResponseUtil.getModeOfIssuance());
    referenceData.put(MATERIAL_TYPES, ReferenceDataResponseUtil.getMaterialTypes());
    referenceData.put(LOAN_TYPES, ReferenceDataResponseUtil.getLoanTypes());
    referenceData.put(ELECTRONIC_ACCESS_RELATIONSHIPS, ReferenceDataResponseUtil.getElectronicAccessRelationships());
    referenceData.put(HOLDING_NOTE_TYPES, ReferenceDataResponseUtil.getHoldingNoteTypes());
    referenceData.put(ITEM_NOTE_TYPES, ReferenceDataResponseUtil.getItemNoteTypes());
    doCallRealMethod().when(pathBuilder).build(any(RecordType.class), any(TransformationFieldsConfig.class));
    doCallRealMethod().when(pathBuilder).build(any(RecordType.class), any(TransformationFieldsConfig.class), any());
    doCallRealMethod().when(displayNameKeyBuilder).build(any(RecordType.class), anyString());
    doCallRealMethod().when(fieldIdBuilder).build(any(RecordType.class), anyString());
    doCallRealMethod().when(fieldIdBuilder).build(any(RecordType.class), anyString(), anyString());
    when(referenceDataProvider.getReferenceDataForTransformationFields(any(OkapiConnectionParams.class))).thenReturn(referenceData);
  }

  @Test
  void getFieldNamesShouldReturnValidFields(VertxTestContext context) {
    // when
    Future<TransformationFieldCollection> transformationFieldsFuture = fieldNamesService.getTransformationFields(okapiConnectionParams);

    // then
    transformationFieldsFuture.onComplete(ar ->
      context.verify(() -> {
        assertTrue(ar.succeeded());
        TransformationFieldCollection transformationFieldCollection = ar.result();
        System.out.print(transformationFieldCollection);
        transformationFieldCollection.getTransformationFields()
          .forEach(transformationField -> checkIfActualFieldEqualToExpected(expectedFields.get(transformationField.getFieldId()), transformationField));
        assertFalse(transformationFieldCollection.getTransformationFields().isEmpty());
        assertNotEquals(0, (int) transformationFieldCollection.getTotalRecords());
        context.completeNow();
      }));
  }

  Map<String, TransformationField> initializeExpectedTranslationFieldsResponse() {
    expectedFields = new HashMap<>();
    JsonArray expectedTransformationFields =
      new JsonObject(readFileContentFromResources(EXPECTED_TRANSFORMATION_FIELDS_RESPONSE_PATH))
        .getJsonArray(TRANSFORMATION_FIELDS);
    for (Object object : expectedTransformationFields) {
      JsonObject jsonObject = JsonObject.mapFrom(object);
      expectedFields.put(jsonObject.getString(FIELD_ID), jsonObject.mapTo(TransformationField.class));
    }
    return expectedFields;
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
