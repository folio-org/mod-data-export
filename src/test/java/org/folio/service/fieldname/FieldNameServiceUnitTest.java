package org.folio.service.fieldname;

import io.vertx.core.Future;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.rest.RestVerticleTestBase;
import org.folio.rest.jaxrs.model.FieldName.RecordType;
import org.folio.rest.jaxrs.model.FieldNameCollection;
import org.folio.service.fieldname.builder.DisplayNameKeyBuilderImpl;
import org.folio.service.fieldname.builder.FieldIdBuilderImpl;
import org.folio.service.fieldname.builder.JsonPathBuilder;
import org.folio.util.OkapiConnectionParams;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class FieldNameServiceUnitTest extends RestVerticleTestBase {

  private static final String TENANT_ID = "diku";
  @Mock
  private JsonPathBuilder pathBuilder;
  @Mock
  private FieldIdBuilderImpl fieldIdBuilder;
  @Mock
  private DisplayNameKeyBuilderImpl displayNameKeyBuilder;
  @Spy
  @InjectMocks
  private FiledNamesServiceImpl filedNamesService;
  private OkapiConnectionParams okapiConnectionParams;

  FieldNameServiceUnitTest() {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    headers.put(OKAPI_HEADER_URL, MOCK_OKAPI_URL);
    okapiConnectionParams = new OkapiConnectionParams(headers);
  }

  @BeforeEach
  void before() {
    doCallRealMethod().when(pathBuilder).build(any(RecordType.class), any(FieldNameConfig.class));
    doCallRealMethod().when(pathBuilder).build(any(RecordType.class), any(FieldNameConfig.class), anyString());
    doCallRealMethod().when(displayNameKeyBuilder).build(any(RecordType.class), anyString());
    doCallRealMethod().when(fieldIdBuilder).build(any(RecordType.class), anyString());
    doCallRealMethod().when(fieldIdBuilder).build(any(RecordType.class), anyString(), anyString());
  }

  @Test
  void getFieldNamesShouldReturnFieldsWithIdentifiers(VertxTestContext context) {
    Future<FieldNameCollection> fieldNameCollectionFuture = filedNamesService.getFieldNames(okapiConnectionParams);

    fieldNameCollectionFuture.onComplete(ar ->
      context.verify(() -> {
        assertTrue(ar.succeeded());
        FieldNameCollection fieldNameCollection = ar.result();
        assertFalse(fieldNameCollection.getFieldNames().isEmpty());
        context.completeNow();
      }));
  }

}
