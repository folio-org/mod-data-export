package org.folio.service.profiles;

import io.vertx.core.Future;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.clients.UsersClient;
import org.folio.dao.impl.JobProfileDaoImpl;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.jaxrs.model.JobProfileCollection;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.UserInfo;
import org.folio.service.profiles.jobprofile.JobProfileServiceImpl;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Collections.singletonList;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class JobProfileServiceUnitTest {
  private static final String TENANT_ID = "diku";
  private static final String JOB_PROFILE_ID = UUID.randomUUID().toString();
  private static final String DEFAULT_INSTANCE_JOB_PROFILE_ID = "6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a";
  private static final String DEFAULT_HOLDINGS_JOB_PROFILE_ID = "5e9835fc-0e51-44c8-8a47-f7b8fce35da7";
  private static final String DEFAULT_AUTHORITY_JOB_PROFILE_ID = "56944b1c-f3f9-475b-bed0-7387c33620ce";

  private static JobProfile expectedJobProfile;

  @Spy
  @InjectMocks
  private JobProfileServiceImpl jobProfileService;
  @Mock
  private JobProfileDaoImpl jobProfileDao;
  @Mock
  private UsersClient usersClient;
  private static OkapiConnectionParams okapiConnectionParams;

  @BeforeAll
  static void beforeEach() {
    expectedJobProfile = new JobProfile()
      .withId(UUID.randomUUID().toString())
      .withDescription("Description")
      .withMappingProfileId(UUID.randomUUID().toString())
      .withUserInfo(new UserInfo())
      .withMetadata(new Metadata()
        .withCreatedByUserId(UUID.randomUUID().toString())
        .withUpdatedByUserId(UUID.randomUUID().toString()));
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    okapiConnectionParams = new OkapiConnectionParams(headers);
  }

  @Test
  void getById_shouldReturnFailedFuture_whenJobProfileDoesNotExist(VertxTestContext context) {
    // given
    when(jobProfileDao.getById(JOB_PROFILE_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.empty()));
    // when
    Future<JobProfile> future = jobProfileService.getById(JOB_PROFILE_ID, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.failed());
      verify(jobProfileDao).getById(eq(JOB_PROFILE_ID), eq(TENANT_ID));
      assertTrue(ar.cause() instanceof NotFoundException);
      context.completeNow();
    }));
  }

  @Test
  void save_shouldCallDaoSave_addUuidToTheJobProfile(VertxTestContext context) {
    // given
    expectedJobProfile.setId(null);
    when(usersClient.getUserInfoAsync(anyString(), any(OkapiConnectionParams.class)))
      .thenReturn(succeededFuture(new UserInfo()));
    when(jobProfileDao.save(expectedJobProfile, TENANT_ID)).thenReturn(Future.succeededFuture(expectedJobProfile));
    // when
    Future<JobProfile> future = jobProfileService.save(expectedJobProfile, okapiConnectionParams);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(jobProfileDao).save(eq(expectedJobProfile), eq(TENANT_ID));
      Assert.assertNotNull(ar.result().getId());
      context.completeNow();
    }));
  }

  @Test
  void delete_shouldCallDaoDelete(VertxTestContext context) {
    // given
    when(jobProfileDao.deleteById(expectedJobProfile.getId(), TENANT_ID)).thenReturn(Future.succeededFuture(true));
    // when
    Future<Boolean> future = jobProfileService.deleteById(expectedJobProfile.getId(), TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(jobProfileDao).deleteById(eq(expectedJobProfile.getId()), eq(TENANT_ID));
      context.completeNow();
    }));
  }

  @Test
  void delete_shouldThrowException_ifJobProfileIsDefault(VertxTestContext context) {
    // assert that exception is thrown
    Assertions.assertThrows(ServiceException.class, () -> {
      jobProfileService.deleteById(DEFAULT_INSTANCE_JOB_PROFILE_ID, TENANT_ID);
    });
    context.completeNow();
  }

  @Test
  void delete_shouldThrowException_ifHoldingsJobProfileIsDefault(VertxTestContext context) {
    // assert that exception is thrown
    Assertions.assertThrows(ServiceException.class, () -> {
      jobProfileService.deleteById(DEFAULT_HOLDINGS_JOB_PROFILE_ID, TENANT_ID);
    });
    context.completeNow();
  }

  @Test
  void delete_shouldThrowException_ifAuthorityJobProfileIsDefault(VertxTestContext context) {
    // assert that exception is thrown
    Assertions.assertThrows(ServiceException.class, () -> {
      jobProfileService.deleteById(DEFAULT_AUTHORITY_JOB_PROFILE_ID, TENANT_ID);
    });
    context.completeNow();
  }

  @Test
  void update_shouldCallDaoUpdate(VertxTestContext context) {
    // given
    when(usersClient.getUserInfoAsync(anyString(), any(OkapiConnectionParams.class)))
      .thenReturn(succeededFuture(new UserInfo()));
    when(jobProfileDao.update(expectedJobProfile, TENANT_ID)).thenReturn(Future.succeededFuture(expectedJobProfile));
    // when
    Future<JobProfile> future = jobProfileService.update(expectedJobProfile, okapiConnectionParams);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(jobProfileDao).update(eq(expectedJobProfile), eq(TENANT_ID));
      context.completeNow();
    }));
  }

  @Test
  void update_shouldThrowException_ifJobProfileIsDefault(VertxTestContext context) {
    // given
    JobProfile defaultJobProfile = new JobProfile().withId(DEFAULT_INSTANCE_JOB_PROFILE_ID);
    // assert that exception is thrown
    Assertions.assertThrows(ServiceException.class, () -> {
      jobProfileService.update(defaultJobProfile, okapiConnectionParams);
    });
    context.completeNow();
  }

  @Test
  void get_shouldCallDaoGet(VertxTestContext context) {
    // given
    String query = "?query=name=Default";
    when(jobProfileDao.get(query, 0, 10, TENANT_ID))
      .thenReturn(Future.succeededFuture(new JobProfileCollection()
        .withJobProfiles(singletonList(expectedJobProfile))));
    // when
    Future<JobProfileCollection> future = jobProfileService.get(query, 0, 10, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(jobProfileDao).get(eq(query), eq(0), eq(10), eq(TENANT_ID));
      context.completeNow();
    }));
  }

  @Test
  void getUsed_shouldReturnUsedJobProfiles(VertxTestContext context) {
    // given
    when(jobProfileDao.getUsed(0, 10, TENANT_ID)).thenReturn(Future.succeededFuture(new JobProfileCollection()
      .withJobProfiles(List.of(new JobProfile().withName("test").withId(UUID.randomUUID().toString())))
      .withTotalRecords(1)));
    // when
    Future<JobProfileCollection> future = jobProfileService.getUsed(0, 10, TENANT_ID);
    // then
    future.onComplete(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      assertEquals(1, ar.result().getJobProfiles().size());
      assertEquals(1, ar.result().getTotalRecords());
      assertEquals("test", ar.result().getJobProfiles().get(0).getName());
      context.completeNow();
    }));
  }

}
