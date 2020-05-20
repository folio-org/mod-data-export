package org.folio.service.profiles;

import io.vertx.core.Future;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.folio.dao.impl.JobProfileDaoImpl;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.jaxrs.model.JobProfileCollection;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.UserInfo;
import org.folio.service.profiles.jobprofile.JobProfileServiceImpl;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.NotFoundException;
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
class JobProfileServiceUnitTest {
  private static final String JOB_PROFILE_ID = UUID.randomUUID().toString();
  private static final String TENANT_ID = "diku";
  private JobProfile expectedJobProfile;
  @Spy
  @InjectMocks
  private JobProfileServiceImpl jobProfileService;
  @Mock
  private JobProfileDaoImpl jobProfileDao;

  @BeforeEach
  public void setUp() {
    expectedJobProfile = new JobProfile()
      .withId(UUID.randomUUID().toString())
      .withDescription("Description")
      .withMappingProfileId(UUID.randomUUID().toString())
      .withUserInfo(new UserInfo())
      .withMetadata(new Metadata());
  }

  @Test
  void getById_shouldReturnFailedFuture_whenJobProfileDoesNotExist(VertxTestContext context) {
    // given
    String errorMessage = String.format("JobProfile not found with id %s", JOB_PROFILE_ID);
    when(jobProfileDao.getById(JOB_PROFILE_ID, TENANT_ID)).thenReturn(Future.succeededFuture(Optional.empty()));
    // when
    Future<JobProfile> future = jobProfileService.getById(JOB_PROFILE_ID, TENANT_ID);
    // then
    future.setHandler(ar -> context.verify(() -> {
      assertTrue(ar.failed());
      verify(jobProfileDao).getById(eq(JOB_PROFILE_ID), eq(TENANT_ID));
      assertTrue(ar.cause() instanceof NotFoundException);
      assertEquals(errorMessage, ar.cause().getMessage());
      context.completeNow();
    }));
  }

  @Test
  void save_shouldCallDaoSave_addUuidToTheMappingProfile(VertxTestContext context) {
    // given
    expectedJobProfile.setId(null);
    when(jobProfileDao.save(expectedJobProfile, TENANT_ID)).thenReturn(Future.succeededFuture(expectedJobProfile));
    // when
    Future<JobProfile> future = jobProfileService.save(expectedJobProfile, TENANT_ID);
    // then
    future.setHandler(ar -> context.verify(() -> {
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
    future.setHandler(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(jobProfileDao).deleteById(eq(expectedJobProfile.getId()), eq(TENANT_ID));
      context.completeNow();
    }));
  }

  @Test
  void update_shouldCallDaoUpdate(VertxTestContext context) {
    // given
    when(jobProfileDao.update(expectedJobProfile, TENANT_ID)).thenReturn(Future.succeededFuture(expectedJobProfile));
    // when
    Future<JobProfile> future = jobProfileService.update(expectedJobProfile, TENANT_ID);
    // then
    future.setHandler(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(jobProfileDao).update(eq(expectedJobProfile), eq(TENANT_ID));
      context.completeNow();
    }));
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
    future.setHandler(ar -> context.verify(() -> {
      assertTrue(ar.succeeded());
      verify(jobProfileDao).get(eq(query), eq(0), eq(10), eq(TENANT_ID));
      context.completeNow();
    }));
  }

}
