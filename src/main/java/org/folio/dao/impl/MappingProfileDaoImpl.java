package org.folio.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.UpdateResult;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.dao.MappingProfileDao;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.MappingProfileCollection;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.persist.interfaces.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.ws.rs.NotFoundException;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import static org.folio.util.HelperUtils.constructCriteria;
import static org.folio.util.HelperUtils.getCQLWrapper;

@Repository
public class MappingProfileDaoImpl implements MappingProfileDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String TABLE = "mapping_profiles";
  private static final String ID_FIELD = "'id'";

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
      Criteria idCrit = constructCriteria(ID_FIELD, mappingProfile.getId());
      pgClientFactory.getInstance(tenantId).update(TABLE, mappingProfile, new Criterion(idCrit), true, updateResult -> {
        if (updateResult.failed()) {
          LOGGER.error("Could not update mappingProfile with id {}", mappingProfile.getId(), updateResult.cause().getMessage());
          promise.fail(updateResult.cause());
        } else if (updateResult.result().getUpdated() != 1) {
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
    return promise.future().map(mappingProfile);
  }

  @Override
  public Future<Boolean> delete(String mappingProfileId, String tenantId) {
    Promise<UpdateResult> promise = Promise.promise();
    try {
      Criteria idCrit = constructCriteria(ID_FIELD, mappingProfileId);
      pgClientFactory.getInstance(tenantId).delete(TABLE, new Criterion(idCrit), promise);
    } catch (Exception e) {
      LOGGER.error(e);
      promise.fail(e);
    }
    return promise.future().map(updateResult -> updateResult.getUpdated() == 1);
  }

  @Override
  public Future<Optional<MappingProfile>> getById(String mappingProfileId, String tenantId) {
    Promise<Results<MappingProfile>> promise = Promise.promise();
    try {
      Criteria idCrit = constructCriteria(ID_FIELD, mappingProfileId);
      pgClientFactory.getInstance(tenantId).get(TABLE, MappingProfile.class, new Criterion(idCrit), false, promise);
    } catch (Exception e) {
      LOGGER.error(e);
      promise.fail(e);
    }
    return promise.future()
      .map(Results::getResults)
      .map(mappingProfiles -> mappingProfiles.isEmpty() ? Optional.empty() : Optional.of(mappingProfiles.get(0)));
  }
}
