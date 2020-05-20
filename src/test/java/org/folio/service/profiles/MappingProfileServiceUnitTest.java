package org.folio.service.profiles;

import io.vertx.core.Future;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.folio.dao.impl.MappingProfileDaoImpl;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.MappingProfileCollection;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.rest.jaxrs.model.UserInfo;
import org.folio.service.profiles.mappingprofile.MappingProfileServiceImpl;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class MappingProfileServiceUnitTest {
  private static final String MAPPING_PROFILE_ID = UUID.randomUUID().toString();
  private static final String TENANT_ID = "diku";
  MappingProfile expectedMappingProfile;
  @Spy
  @InjectMocks
  private MappingProfileServiceImpl mappingProfileService;
  @Mock
  private MappingProfileDaoImpl mappingProfileDao;

  @BeforeEach
  public void setUp() {
    expectedMappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withDescription("Description")
      .withRecordTypes(singletonList(RecordType.INSTANCE))
      .withOutputFormat(MappingProfile.OutputFormat.MARC)
      .withTransformations(singletonList(new Transformations()))
      .withUserInfo(new UserInfo())
      .withMetadata(new Metadata());
  }


  @Test
  void getById_shouldReturnFailedFuture_whenMappingProfileDoesNotExist(VertxTestContext context) {
    // given
    String errorMessage = String.format("Mapping profile not found with id %s", MAPPING_PROFILE_ID);
    when(mappingProfileDao.getById(MAPPING_PROFILE_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.empty()));
    // when
    Future<MappingProfile> future = mappingProfileService.getById(MAPPING_PROFILE_ID, TENANT_ID);
    // then
    future.setHandler(ar -> {
      context.verify(() -> {
        assertTrue(ar.failed());
        verify(mappingProfileDao).getById(eq(MAPPING_PROFILE_ID), eq(TENANT_ID));
        assertEquals(errorMessage, ar.cause().getMessage());
        context.completeNow();
      });
    });
  }

  @Test
  void save_shouldCallDaoSave_addUuidToTheMappingProfile(VertxTestContext context) {
    // given
    expectedMappingProfile.setId(null);
    when(mappingProfileDao.save(expectedMappingProfile, TENANT_ID)).thenReturn(Future.succeededFuture(expectedMappingProfile));
    // when
    Future<MappingProfile> future = mappingProfileService.save(expectedMappingProfile, TENANT_ID);
    // then
    future.setHandler(ar -> {
      context.verify(() -> {
        assertTrue(ar.succeeded());
        verify(mappingProfileDao).save(eq(expectedMappingProfile), eq(TENANT_ID));
        Assert.assertNotNull(ar.result().getId());
        context.completeNow();
      });
    });
  }

  @Test
  void delete_shouldCallDaoDelete(VertxTestContext context) {
    // given
    when(mappingProfileDao.delete(expectedMappingProfile.getId(), TENANT_ID)).thenReturn(Future.succeededFuture(true));
    // when
    Future<Boolean> future = mappingProfileService.delete(expectedMappingProfile.getId(), TENANT_ID);
    // then
    future.setHandler(ar -> {
      context.verify(() -> {
        assertTrue(ar.succeeded());
        verify(mappingProfileDao).delete(eq(expectedMappingProfile.getId()), eq(TENANT_ID));
        context.completeNow();
      });
    });
  }

  @Test
  void update_shouldCallDaoUpdate(VertxTestContext context) {
    // given
    when(mappingProfileDao.update(expectedMappingProfile, TENANT_ID)).thenReturn(Future.succeededFuture(expectedMappingProfile));
    // when
    Future<MappingProfile> future = mappingProfileService.update(expectedMappingProfile, TENANT_ID);
    // then
    future.setHandler(ar -> {
      context.verify(() -> {
        assertTrue(ar.succeeded());
        verify(mappingProfileDao).update(eq(expectedMappingProfile), eq(TENANT_ID));
        context.completeNow();
      });
    });
  }

  @Test
  void get_shouldCallDaoGet(VertxTestContext context) {
    // given
    String query = "?query=recordTypes=INSTANCE";
    when(mappingProfileDao.get(query, 0, 10, TENANT_ID))
      .thenReturn(Future.succeededFuture(new MappingProfileCollection()
        .withMappingProfiles(singletonList(expectedMappingProfile))));
    // when
    Future<MappingProfileCollection> future = mappingProfileService.get(query, 0, 10, TENANT_ID);
    // then
    future.setHandler(ar -> {
      context.verify(() -> {
        assertTrue(ar.succeeded());
        verify(mappingProfileDao).get(eq(query), eq(0), eq(10), eq(TENANT_ID));
        context.completeNow();
      });
    });
  }

}
