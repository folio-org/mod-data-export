package org.folio.service.profiles.jobprofile;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.HttpStatus;
import org.folio.clients.UsersClient;
import org.folio.dao.JobProfileDao;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.jaxrs.model.JobProfileCollection;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.lang.invoke.MethodHandles;
import java.util.UUID;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Service
public class JobProfileServiceImpl implements JobProfileService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String DEFAULT_JOB_PROFILE_ID = "6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a";

  @Autowired
  private JobProfileDao jobProfileDao;
  @Autowired
  private UsersClient usersClient;

  @Override
  public Future<JobProfile> getById(String id, String tenantId) {
    return jobProfileDao.getById(id, tenantId)
      .compose(optionalJobProfile -> {
        if (optionalJobProfile.isPresent()) {
          return succeededFuture(optionalJobProfile.get());
        } else {
          String errorMessage = String.format("JobProfile not found with id %s", id);
          LOGGER.error(errorMessage);
          return failedFuture(new NotFoundException(errorMessage));
        }
      });
  }

  @Override
  public Future<JobProfile> save(JobProfile jobProfile, OkapiConnectionParams params) {
    if (jobProfile.getId() == null) {
      jobProfile.setId(UUID.randomUUID().toString());
    }
    Promise<JobProfile> jobProfilePromise = Promise.promise();
    if (jobProfile.getMetadata() != null && isNotEmpty(jobProfile.getMetadata().getCreatedByUserId())) {
      usersClient.getUserInfoAsync(jobProfile.getMetadata().getCreatedByUserId(), params)
        .onComplete(optionalUserInfoAr -> {
          if (optionalUserInfoAr.succeeded()) {
            jobProfile.withUserInfo(optionalUserInfoAr.result());
          }
          jobProfileDao.save(jobProfile, params.getTenantId())
            .onSuccess(jobProfilePromise::complete)
            .onFailure(jobProfilePromise::fail);
        });
    } else {
      return jobProfileDao.save(jobProfile, params.getTenantId());
    }
    return jobProfilePromise.future();
  }

  @Override
  public Future<JobProfile> update(JobProfile jobProfile, OkapiConnectionParams params) {
    Promise<JobProfile> jobProfilePromise = Promise.promise();
    if (DEFAULT_JOB_PROFILE_ID.equals(jobProfile.getId())) {
      throw new ServiceException(HttpStatus.HTTP_FORBIDDEN, "Editing of default job profile is forbidden");
    }
    if (jobProfile.getMetadata() != null && isNotEmpty(jobProfile.getMetadata().getUpdatedByUserId())) {
      usersClient.getUserInfoAsync(jobProfile.getMetadata().getUpdatedByUserId(), params)
        .onComplete(optionalUserInfoAr -> {
          if (optionalUserInfoAr.succeeded()) {
            jobProfile.withUserInfo(optionalUserInfoAr.result());
          }
          jobProfileDao.update(jobProfile, params.getTenantId())
            .onSuccess(jobProfilePromise::complete)
            .onFailure(jobProfilePromise::fail);
        });
    } else {
      return jobProfileDao.update(jobProfile, params.getTenantId());
    }
    return jobProfilePromise.future();
  }

  @Override
  public Future<JobProfileCollection> get(String query, int offset, int limit, String tenantId) {
    return jobProfileDao.get(query, offset, limit, tenantId);
  }

  @Override
  public Future<Boolean> deleteById(String id, String tenantId) {
    if (DEFAULT_JOB_PROFILE_ID.equals(id)) {
      throw new ServiceException(HttpStatus.HTTP_FORBIDDEN, "Deletion of default job profile is forbidden");
    }
    return jobProfileDao.deleteById(id, tenantId);
  }

}
