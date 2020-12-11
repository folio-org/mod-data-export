package org.folio.service.transformationfields;

import static org.folio.TestUtil.readFileContentFromResources;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.jaxrs.model.RecordType.HOLDINGS;
import static org.folio.rest.jaxrs.model.RecordType.INSTANCE;
import static org.folio.rest.jaxrs.model.RecordType.ITEM;
import static org.folio.util.ExternalPathResolver.ALTERNATIVE_TITLE_TYPES;
import static org.folio.util.ExternalPathResolver.CONTRIBUTOR_NAME_TYPES;
import static org.folio.util.ExternalPathResolver.ELECTRONIC_ACCESS_RELATIONSHIPS;
import static org.folio.util.ExternalPathResolver.HOLDING_NOTE_TYPES;
import static org.folio.util.ExternalPathResolver.IDENTIFIER_TYPES;
import static org.folio.util.ExternalPathResolver.INSTANCE_TYPES;
import static org.folio.util.ExternalPathResolver.ISSUANCE_MODES;
import static org.folio.util.ExternalPathResolver.ITEM_NOTE_TYPES;
import static org.folio.util.ExternalPathResolver.LOAN_TYPES;
import static org.folio.util.ExternalPathResolver.MATERIAL_TYPES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;
import org.folio.processor.referencedata.ReferenceData;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.TransformationField;
import org.folio.rest.jaxrs.model.TransformationField.RecordType;
import org.folio.rest.jaxrs.model.TransformationFieldCollection;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.mapping.referencedata.ReferenceDataImpl;
import org.folio.service.mapping.referencedata.ReferenceDataProvider;
import org.folio.service.transformationfields.builder.DisplayNameKeyBuilderImpl;
import org.folio.service.transformationfields.builder.FieldIdBuilderImpl;
import org.folio.service.transformationfields.builder.JsonPathBuilder;
import org.folio.util.OkapiConnectionParams;
import org.folio.util.ReferenceDataResponseUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class TransformationFieldsServiceUnitTest {

  private static final String EXPECTED_TRANSFORMATION_FIELDS_RESPONSE_PATH = "mapping/expectedTransformationFields.json";
  private static final String TRANSFORMATION_FIELDS = "transformationFields";
  private static final String FIELD_ID = "fieldId";
  private static final String TENANT_ID = "diku";

  private static final String EMPTY_TRANSFORMATION = "";

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

  @Test
  void getFieldNamesShouldReturnValidFields(VertxTestContext context) {
    //given
    mocReferenceData();
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

  private void mocReferenceData() {
    ReferenceData referenceData = new ReferenceDataImpl();
    referenceData.put(IDENTIFIER_TYPES, ReferenceDataResponseUtil.getIdentifierTypes());
    referenceData.put(ALTERNATIVE_TITLE_TYPES, ReferenceDataResponseUtil.getAlternativeTitleTypes());
    referenceData.put(CONTRIBUTOR_NAME_TYPES, ReferenceDataResponseUtil.getContributorNameTypes());
    referenceData.put(INSTANCE_TYPES, ReferenceDataResponseUtil.getInstanceTypes());
    referenceData.put(ISSUANCE_MODES, ReferenceDataResponseUtil.getModeOfIssuance());
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
  void shouldReturnFailedFuture_whenValidateTransformationsWithEmptyTransformationItemRecordType(VertxTestContext testContext) {
    testContext.verify(() -> {
      List<Transformations> list = getTransformationsList(ITEM, EMPTY_TRANSFORMATION);
      fieldNamesService.validateTransformations(list).onComplete(res -> {
          assertTrue(res.failed());
          assertTrue(res.cause() instanceof ServiceException);
          assertNotNull(res.cause().getMessage());
          testContext.completeNow();
        }
      );
    });
  }

  @Test
  void shouldReturnSucceededFuture_whenValidateTransformationsWithEmptyTransformationHoldingsRecordType(VertxTestContext testContext) {
    testContext.verify(() -> {
      List<Transformations> list = getTransformationsList(HOLDINGS, EMPTY_TRANSFORMATION);
      fieldNamesService.validateTransformations(list).onComplete(res -> {
          assertTrue(res.succeeded());
          testContext.completeNow();
        }
      );
    });
  }

  @Test
  void shouldReturnSucceededFuture_whenValidateTransformationsWithEmptyTransformationInstanceRecordType(VertxTestContext testContext) {
    testContext.verify(() -> {
      List<Transformations> list = getTransformationsList(INSTANCE, EMPTY_TRANSFORMATION);
      fieldNamesService.validateTransformations(list).onComplete(res -> {
          assertTrue(res.succeeded());
          testContext.completeNow();
        }
      );
    });
  }

  private List<Transformations> getTransformationsList(org.folio.rest.jaxrs.model.RecordType recordType, String transformation) {
    return Collections.singletonList(new Transformations().withRecordType(recordType).withTransformation(transformation));
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
