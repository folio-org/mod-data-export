package org.folio.service.profiles.jobprofile;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.UUID;
import javax.ws.rs.NotFoundException;
import org.folio.dao.JobProfileDao;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.jaxrs.model.JobProfileCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JobProfileServiceImpl implements JobProfileService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private JobProfileDao jobProfileDao;

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
  public Future<JobProfile> save(JobProfile jobProfile, String tenantId) {
    if (jobProfile.getId() == null) {
      jobProfile.setId(UUID.randomUUID().toString());
    }
    return jobProfileDao.save(jobProfile, tenantId);
  }

  @Override
  public Future<JobProfile> update(JobProfile jobProfile, String tenantId) {
    return jobProfileDao.update(jobProfile, tenantId);
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
