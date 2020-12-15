package org.folio.service.profiles;

import io.vertx.core.Future;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.clients.UsersClient;
import org.folio.dao.impl.MappingProfileDaoImpl;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.MappingProfileCollection;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.TransformationField;
import org.folio.rest.jaxrs.model.TransformationFieldCollection;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.rest.jaxrs.model.UserInfo;
import org.folio.service.profiles.mappingprofile.MappingProfileServiceImpl;
import org.folio.service.transformationfields.TransformationFieldsService;
import org.folio.util.OkapiConnectionParams;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.NotFoundException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.jaxrs.model.TransformationField.RecordType.INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class MappingProfileServiceUnitTest {
  private static final String MAPPING_PROFILE_ID = UUID.randomUUID().toString();
  private static final String TENANT_ID = "diku";
  private static final String FIELD_ID = "fieldId";
  private static final String MISSING_FIELD_ID = "missingFieldId";
  private static MappingProfile expectedMappingProfile;
  private static final String DEFAULT_MAPPING_PROFILE_ID = "25d81cbe-9686-11ea-bb37-0242ac130002";

  @Spy
  @InjectMocks
  private MappingProfileServiceImpl mappingProfileService;
  @Mock
  private MappingProfileDaoImpl mappingProfileDao;
  @Mock
  private UsersClient usersClient;
  @Mock
  private TransformationFieldsService transformationFieldsService;

  private static OkapiConnectionParams okapiConnectionParams;

  @BeforeAll
  static void beforeEach() {
    expectedMappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withDescription("Description")
      .withRecordTypes(singletonList(RecordType.INSTANCE))
      .withOutputFormat(MappingProfile.OutputFormat.MARC)
      .withTransformations(singletonList(new Transformations()))
      .withUserInfo(new UserInfo())
      .withMetadata(new Metadata()
        .withCreatedByUserId(UUID.randomUUID().toString())
        .withUpdatedByUserId(UUID.randomUUID().toString()));
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    okapiConnectionParams = new OkapiConnectionParams(headers);
  }

  @Test
  void getById_shouldReturnFailedFuture_whenMappingProfileDoesNotExist(VertxTestContext context) {
    // given
    when(mappingProfileDao.getById(MAPPING_PROFILE_ID, TENANT_ID)).thenReturn(succeededFuture(Optional.empty()));
    // when
    Future<MappingProfile> future = mappingProfileService.getById(MAPPING_PROFILE_ID, okapiConnectionParams);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.failed());
      verify(mappingProfileDao).getById(eq(MAPPING_PROFILE_ID), eq(TENANT_ID));
      assertTrue(ar.cause() instanceof NotFoundException);
      context.completeNow();
    }));
  }

  @Test
  void save_shouldCallDaoSave_addUuidToTheMappingProfile(VertxTestContext context) {
    // given
    expectedMappingProfile.setId(null);
    when(mappingProfileDao.save(expectedMappingProfile, TENANT_ID)).thenReturn(succeededFuture(expectedMappingProfile));
    when(usersClient.getUserInfoAsync(expectedMappingProfile.getMetadata().getCreatedByUserId(), okapiConnectionParams))
      .thenReturn(succeededFuture(new UserInfo()));
    // when
    Future<MappingProfile> future = mappingProfileService.save(expectedMappingProfile, okapiConnectionParams);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(mappingProfileDao).save(eq(expectedMappingProfile), eq(TENANT_ID));
      Assert.assertNotNull(ar.result().getId());
      context.completeNow();
    }));
  }

  @Test
  void delete_shouldCallDaoDelete(VertxTestContext context) {
    // given
    when(mappingProfileDao.delete(expectedMappingProfile.getId(), TENANT_ID)).thenReturn(succeededFuture(true));
    // when
    Future<Boolean> future = mappingProfileService.deleteById(expectedMappingProfile.getId(), TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(mappingProfileDao).delete(eq(expectedMappingProfile.getId()), eq(TENANT_ID));
      context.completeNow();
    }));
  }

  @Test
  void delete_shouldThrowException_ifMappingProfileIsDefault(VertxTestContext context) {
    // assert that exception is thrown
    Assertions.assertThrows(ServiceException.class, () -> {
      mappingProfileService.deleteById(DEFAULT_MAPPING_PROFILE_ID, TENANT_ID);
    });
    context.completeNow();
  }

  @Test
  void update_shouldCallDaoUpdate(VertxTestContext context) {
    // given
    when(mappingProfileDao.update(expectedMappingProfile, TENANT_ID)).thenReturn(succeededFuture(expectedMappingProfile));
    when(usersClient.getUserInfoAsync(expectedMappingProfile.getMetadata().getUpdatedByUserId(), okapiConnectionParams))
      .thenReturn(succeededFuture(new UserInfo()));
    // when
    Future<MappingProfile> future = mappingProfileService.update(expectedMappingProfile, okapiConnectionParams);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(mappingProfileDao).update(eq(expectedMappingProfile), eq(TENANT_ID));
      context.completeNow();
    }));
  }

  @Test
  void update_shouldThrowException_ifMappingProfileIsDefault(VertxTestContext context) {
    // given
    MappingProfile defaultMappingProfile = new MappingProfile().withId(DEFAULT_MAPPING_PROFILE_ID);
    // assert that exception is thrown
    Assertions.assertThrows(ServiceException.class, () -> {
      mappingProfileService.update(defaultMappingProfile, okapiConnectionParams);
    });
    context.completeNow();
  }

  @Test
  void get_shouldCallDaoGet(VertxTestContext context) {
    // given
    String query = "?query=recordTypes=INSTANCE";
    when(mappingProfileDao.get(query, 0, 10, TENANT_ID))
      .thenReturn(succeededFuture(new MappingProfileCollection()
        .withMappingProfiles(singletonList(expectedMappingProfile))));
    // when
    Future<MappingProfileCollection> future = mappingProfileService.get(query, 0, 10, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(mappingProfileDao).get(eq(query), eq(0), eq(10), eq(TENANT_ID));
      context.completeNow();
    }));
  }

  @Test
  void validate_shouldThrowExceptionWhenTransformationFieldIdMissing(VertxTestContext context) {
    // given
    when(transformationFieldsService.validateTransformations(anyList())).thenReturn(Future.succeededFuture());
    Transformations transformations = new Transformations();
    expectedMappingProfile.setTransformations(singletonList(transformations));
    TransformationFieldCollection transformationFieldCollection = new TransformationFieldCollection()
      .withTransformationFields(singletonList(new TransformationField()));
    when(transformationFieldsService.getTransformationFields(okapiConnectionParams)).thenReturn(succeededFuture(transformationFieldCollection));
    //then
    Future<Void> future = mappingProfileService.validate(expectedMappingProfile, okapiConnectionParams);
    //when
    future.onComplete(ar -> {
      assertTrue(ar.failed());
      assertEquals(ServiceException.class, ar.cause().getClass());
      assertEquals("Field id is missing for mapping profile transformation", ar.cause().getMessage());
      context.completeNow();
    });
  }

  @Test
  void validate_shouldThrowExceptionWhenTransformationFieldDoesntExistByProvidedFieldId(VertxTestContext context) {
    // given
    when(transformationFieldsService.validateTransformations(anyList())).thenReturn(Future.succeededFuture());
    Transformations transformations = new Transformations()
      .withFieldId(MISSING_FIELD_ID);
    expectedMappingProfile.setTransformations(singletonList(transformations));
    TransformationFieldCollection transformationFieldCollection = new TransformationFieldCollection()
      .withTransformationFields(singletonList(new TransformationField()
        .withFieldId(FIELD_ID)));
    when(transformationFieldsService.getTransformationFields(okapiConnectionParams)).thenReturn(succeededFuture(transformationFieldCollection));
    //then
    Future<Void> future = mappingProfileService.validate(expectedMappingProfile, okapiConnectionParams);
    //when
    future.onComplete(ar -> {
      assertTrue(ar.failed());
      assertEquals(ServiceException.class, ar.cause().getClass());
      assertEquals("Transformation doesn't exist for provided fieldId: " + MISSING_FIELD_ID, ar.cause().getMessage());
      context.completeNow();
    });
  }

  @Test
  void validate_shouldThrowExceptionWhenTransformationRecordTypeIncorrect(VertxTestContext context) {
    // given
    when(transformationFieldsService.validateTransformations(anyList())).thenReturn(Future.succeededFuture());
    Transformations transformations = new Transformations()
      .withFieldId(FIELD_ID)
      .withRecordType(RecordType.HOLDINGS);
    expectedMappingProfile.setTransformations(singletonList(transformations));
    TransformationFieldCollection transformationFieldCollection = new TransformationFieldCollection()
      .withTransformationFields(singletonList(new TransformationField()
        .withFieldId(FIELD_ID)
        .withRecordType(INSTANCE)));
    when(transformationFieldsService.getTransformationFields(okapiConnectionParams)).thenReturn(succeededFuture(transformationFieldCollection));
    //then
    Future<Void> future = mappingProfileService.validate(expectedMappingProfile, okapiConnectionParams);
    //when
    future.onComplete(ar -> {
      assertTrue(ar.failed());
      assertEquals(ServiceException.class, ar.cause().getClass());
      assertEquals(format("Transformation record type is missing or incorrect according to provided fieldId: %s, " +
        "expected record type: %s", FIELD_ID, INSTANCE), ar.cause().getMessage());
      context.completeNow();
    });
  }

  @Test
  void validate_shouldReturnSucceededFuture(VertxTestContext context) {
    // given
    when(transformationFieldsService.validateTransformations(anyList())).thenReturn(Future.succeededFuture());
    Transformations transformations = new Transformations()
      .withFieldId(FIELD_ID)
      .withRecordType(RecordType.INSTANCE);
    expectedMappingProfile.setTransformations(singletonList(transformations));
    TransformationFieldCollection transformationFieldCollection = new TransformationFieldCollection()
      .withTransformationFields(singletonList(new TransformationField()
        .withFieldId(FIELD_ID)
        .withRecordType(INSTANCE)));
    when(transformationFieldsService.getTransformationFields(okapiConnectionParams)).thenReturn(succeededFuture(transformationFieldCollection));
    //then
    Future<Void> future = mappingProfileService.validate(expectedMappingProfile, okapiConnectionParams);
    //when
    future.onComplete(ar -> {
      assertTrue(ar.succeeded());
      context.completeNow();
    });
  }

}
