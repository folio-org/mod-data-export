package org.folio.service.profiles.mappingprofile;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.clients.UsersClient;
import org.folio.dao.MappingProfileDao;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.MappingProfileCollection;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.lang.invoke.MethodHandles;
import java.util.UUID;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

/**
 * Implementation of the MappingProfileService, calls MappingProfileDao to access MappingProfile metadata.
 */
@Service
public class MappingProfileServiceImpl implements MappingProfileService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private MappingProfileDao mappingProfileDao;
  @Autowired
  private UsersClient usersClient;

  @Override
  public Future<MappingProfileCollection> get(String query, int offset, int limit, String tenantId) {
    return mappingProfileDao.get(query, offset, limit, tenantId);
  }

  @Override
  public Future<MappingProfile> save(MappingProfile mappingProfile, OkapiConnectionParams params) {
    if (mappingProfile.getId() == null) {
      mappingProfile.setId(UUID.randomUUID().toString());
    }
    if (mappingProfile.getMetadata() != null && mappingProfile.getMetadata().getCreatedByUserId() != null) {
      return usersClient.getUserInfoAsync(mappingProfile.getMetadata().getCreatedByUserId(), params)
        .compose(userInfo -> mappingProfileDao.save(mappingProfile.withUserInfo(userInfo), params.getTenantId()));
    } else {
      return mappingProfileDao.save(mappingProfile, params.getTenantId());
    }
  }

  @Override
  public Future<MappingProfile> update(MappingProfile mappingProfile, OkapiConnectionParams params) {
    if (mappingProfile.getMetadata() != null && mappingProfile.getMetadata().getUpdatedByUserId() != null) {
      return usersClient.getUserInfoAsync(mappingProfile.getMetadata().getUpdatedByUserId(), params)
        .compose(userInfo -> mappingProfileDao.update(mappingProfile.withUserInfo(userInfo), params.getTenantId()));
    } else {
      return mappingProfileDao.update(mappingProfile, params.getTenantId());
    }

  }

  @Override
  public Future<MappingProfile> getById(String mappingProfileId, String tenantId) {
    return mappingProfileDao.getById(mappingProfileId, tenantId)
      .compose(optionalMappingProfile -> {
        if (optionalMappingProfile.isPresent()) {
          return succeededFuture(optionalMappingProfile.get());
        } else {
          String errorMessage = String.format("Mapping profile not found with id %s", mappingProfileId);
          LOGGER.error(errorMessage);
          return failedFuture(new NotFoundException(errorMessage));
        }
      });
  }

  @Override
  public Future<Boolean> delete(String mappingProfileId, String tenantId) {
    return mappingProfileDao.delete(mappingProfileId, tenantId);
  }
}
