package org.folio.service.fieldname;

import io.vertx.core.Future;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.processor.ReferenceData;
import org.folio.rest.jaxrs.model.TransformationField.RecordType;
import org.folio.rest.jaxrs.model.TransformationFieldCollection;
import org.folio.service.fieldname.builder.DisplayNameKeyBuilderImpl;
import org.folio.service.fieldname.builder.FieldIdBuilderImpl;
import org.folio.service.fieldname.builder.JsonPathBuilder;
import org.folio.service.mapping.referencedata.ReferenceDataImpl;
import org.folio.service.mapping.referencedata.ReferenceDataProvider;
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

import java.util.Map;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.IDENTIFIER_TYPES;
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
  private TransformationFieldsServiceImpl filedNamesService;
  private ReferenceData referenceData;

  private OkapiConnectionParams okapiConnectionParams;

  TransformationFieldsServiceUnitTest() {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, "diku");
    okapiConnectionParams = new OkapiConnectionParams(headers);
  }

  @BeforeEach
  void before() {
    referenceData = new ReferenceDataImpl();
    referenceData.put(IDENTIFIER_TYPES, ReferenceDataResponseUtil.getIdentifierTypes());
    doCallRealMethod().when(pathBuilder).build(any(RecordType.class), any(TransformationFieldsConfig.class));
    doCallRealMethod().when(pathBuilder).build(any(RecordType.class), any(TransformationFieldsConfig.class), anyString());
    doCallRealMethod().when(displayNameKeyBuilder).build(any(RecordType.class), anyString());
    doCallRealMethod().when(fieldIdBuilder).build(any(RecordType.class), anyString());
    doCallRealMethod().when(fieldIdBuilder).build(any(RecordType.class), anyString(), anyString());
    when(referenceDataProvider.getReferenceDataForTransformationFields(any(OkapiConnectionParams.class))).thenReturn(referenceData);
  }

  @Test
  void getFieldNamesShouldReturnFieldsWithIdentifiers(VertxTestContext context) {
    Future<TransformationFieldCollection> transformationFieldsFuture = filedNamesService.getTransformationFields(okapiConnectionParams);

    transformationFieldsFuture.onComplete(ar ->
      context.verify(() -> {
        assertTrue(ar.succeeded());
        TransformationFieldCollection transformationFieldCollection = ar.result();
        assertFalse(transformationFieldCollection.getTransformationFields().isEmpty());
        assertNotEquals(0, (int) transformationFieldCollection.getTotalRecords());
        context.completeNow();
      }));
  }

}
