package org.folio.service.profiles.jobprofile;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.clients.UsersClient;
import org.folio.dao.JobProfileDao;
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
    if (jobProfile.getMetadata() != null && isNotEmpty(jobProfile.getMetadata().getCreatedByUserId())) {
      return usersClient.getUserInfoAsync(jobProfile.getMetadata().getCreatedByUserId(), params)
        .compose(userInfo -> jobProfileDao.save(jobProfile.withUserInfo(userInfo), params.getTenantId()));
    } else {
      return jobProfileDao.save(jobProfile, params.getTenantId());
    }
  }

  @Override
  public Future<JobProfile> update(JobProfile jobProfile, OkapiConnectionParams params) {
    if (jobProfile.getMetadata() != null && isNotEmpty(jobProfile.getMetadata().getUpdatedByUserId())) {
      return usersClient.getUserInfoAsync(jobProfile.getMetadata().getUpdatedByUserId(), params)
        .compose(userInfo -> jobProfileDao.update(jobProfile.withUserInfo(userInfo), params.getTenantId()));
    } else {
      return jobProfileDao.update(jobProfile, params.getTenantId());
    }
  }

  @Override
  public Future<JobProfileCollection> get(String query, int offset, int limit, String tenantId) {
    return jobProfileDao.get(query, offset, limit, tenantId);
  }

  @Override
  public Future<Boolean> deleteById(String id, String tenantId) {
    return jobProfileDao.deleteById(id, tenantId);
  }

}
