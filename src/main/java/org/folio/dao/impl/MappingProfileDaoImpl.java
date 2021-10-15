package org.folio.dao.impl;

import static org.folio.util.HelperUtils.getCQLWrapper;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import javax.ws.rs.NotFoundException;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.dao.MappingProfileDao;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.MappingProfileCollection;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.persist.interfaces.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class MappingProfileDaoImpl implements MappingProfileDao {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  private static final String TABLE = "mapping_profiles";

  @Autowired
  private PostgresClientFactory pgClientFactory;

  @Override
  public Future<MappingProfileCollection> get(String query, int offset, int limit, String tenantId) {
    Promise<Results<MappingProfile>> promise = Promise.promise();
    try {
      String[] fieldList = {"*"};
      CQLWrapper cql = getCQLWrapper(TABLE, query, limit, offset);
      pgClientFactory.getInstance(tenantId).get(TABLE, MappingProfile.class, fieldList, cql, true, false, promise);
    } catch (FieldException exception) {
      LOGGER.error("Error while querying mappingProfiles", exception);
      promise.fail(exception);
    }
    return promise.future().map(results -> new MappingProfileCollection()
      .withMappingProfiles(results.getResults())
      .withTotalRecords(results.getResultInfo().getTotalRecords()));
  }

  @Override
  public Future<MappingProfile> save(MappingProfile mappingProfile, String tenantId) {
    Promise<String> promise = Promise.promise();
    pgClientFactory.getInstance(tenantId).save(TABLE, mappingProfile.getId(), mappingProfile, promise);
    return promise.future().map(mappingProfile);
  }

  @Override
  public Future<MappingProfile> update(MappingProfile mappingProfile, String tenantId) {
    Promise<MappingProfile> promise = Promise.promise();
    try {
      pgClientFactory.getInstance(tenantId).update(TABLE, mappingProfile, mappingProfile.getId(), updateResult -> {
        if (updateResult.failed()) {
          LOGGER.error("Could not update mappingProfile with id {}, cause: {}", mappingProfile.getId(), updateResult.cause().getMessage());
          promise.fail(updateResult.cause());
        } else if (updateResult.result().rowCount() != 1) {
          String errorMessage = String.format("MappingProfile with id '%s' was not found", mappingProfile.getId());
          LOGGER.error(errorMessage);
          promise.fail(new NotFoundException(errorMessage));
        } else {
          promise.complete(mappingProfile);
        }
      });
    } catch (Exception e) {
      LOGGER.error("Error updating jobExecution", e);
      promise.fail(e);
    }
    return promise.future();
  }

  @Override
  public Future<Boolean> delete(String mappingProfileId, String tenantId) {
    Promise<RowSet<Row>> promise = Promise.promise();
    try {
      pgClientFactory.getInstance(tenantId).delete(TABLE, mappingProfileId, promise);
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      promise.fail(e);
    }
    return promise.future().map(updateResult -> updateResult.rowCount() == 1);
  }

  @Override
  public Future<Optional<MappingProfile>> getById(String mappingProfileId, String tenantId) {
    Promise<MappingProfile> promise = Promise.promise();
    try {
      pgClientFactory.getInstance(tenantId).getById(TABLE, mappingProfileId, MappingProfile.class, promise);
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      promise.fail(e);
    }
    return promise.future().map(Optional::ofNullable);
  }
}
