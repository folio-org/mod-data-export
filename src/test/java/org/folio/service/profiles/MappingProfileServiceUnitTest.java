package org.folio.service.profiles;

import io.vertx.core.Future;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.dao.impl.MappingProfileDaoImpl;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.MappingProfileCollection;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.rest.jaxrs.model.UserInfo;
import org.folio.service.profiles.mapping.MappingProfileServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
public class MappingProfileServiceUnitTest {
  private static final String MAPPING_PROFILE_ID = UUID.randomUUID().toString();
  private static final String TENANT_ID = "diku";
  MappingProfile expectedMappingProfile;
  @Spy
  @InjectMocks
  private MappingProfileServiceImpl mappingProfileService;
  @Mock
  private MappingProfileDaoImpl mappingProfileDao;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
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
  void getById_shouldReturnFailedFuture_whenMappingProfileDoesNotExist(TestContext context) {
    // given
    Async async = context.async();
    String errorMessage = String.format("Mapping profile not found with id %s", MAPPING_PROFILE_ID);
    when(mappingProfileDao.getById(MAPPING_PROFILE_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.empty()));
    // when
    Future<MappingProfile> future = mappingProfileService.getById(MAPPING_PROFILE_ID, TENANT_ID);
    // then
    verify(mappingProfileDao, times(1)).getById(MAPPING_PROFILE_ID, TENANT_ID);
    future.setHandler(ar -> {
      context.assertTrue(ar.failed());
      Assert.assertEquals(ar.cause().getMessage(), errorMessage);
      async.complete();
    });
  }

  @Test
  void save_shouldCallDaoSave_addUuidToTheMappingProfile(TestContext context) {
    // given
    Async async = context.async();
    expectedMappingProfile.setId(null);
    when(mappingProfileDao.save(expectedMappingProfile, TENANT_ID)).thenReturn(Future.succeededFuture(expectedMappingProfile));
    // when
    Future<MappingProfile> future = mappingProfileService.save(expectedMappingProfile, TENANT_ID);
    // then
    verify(mappingProfileDao, times(1)).save(expectedMappingProfile, TENANT_ID);
    future.setHandler(ar -> {
      context.assertTrue(ar.succeeded());
      Assert.assertNotNull(ar.result().getId());
      async.complete();
    });
  }

  @Test
  void delete_shouldCallDaoDelete(TestContext context) {
    // given
    when(mappingProfileDao.delete(expectedMappingProfile.getId(), TENANT_ID)).thenReturn(Future.succeededFuture(true));
    // when
    mappingProfileService.delete(expectedMappingProfile.getId(), TENANT_ID);
    // then
    verify(mappingProfileDao, times(1)).delete(expectedMappingProfile.getId(), TENANT_ID);
  }

  @Test
  void update_shouldCallDaoUpdate(TestContext context) {
    // given
    when(mappingProfileDao.update(expectedMappingProfile, TENANT_ID)).thenReturn(Future.succeededFuture(expectedMappingProfile));
    // when
    mappingProfileService.update(expectedMappingProfile, TENANT_ID);
    // then
    verify(mappingProfileDao, times(1)).update(expectedMappingProfile, TENANT_ID);
  }

  @Test
  void get_shouldCallDaoGet(TestContext context) {
    // given
    String query = "?query=recordTypes=INSTANCE";
    when(mappingProfileDao.get(query, 0, 10, TENANT_ID))
      .thenReturn(Future.succeededFuture(new MappingProfileCollection()
        .withMappingProfiles(singletonList(expectedMappingProfile))));
    // when
    mappingProfileService.get(query, 0, 10, TENANT_ID);
    // then
    verify(mappingProfileDao, times(1)).get(query, 0, 10, TENANT_ID);
  }

}
