package org.folio.dao.impl;

import static org.folio.util.HelperUtils.constructCriteria;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.dao.JobProfileDao;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.jaxrs.model.JobProfileCollection;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.persist.interfaces.Results;
import org.folio.util.HelperUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.ws.rs.NotFoundException;



@Repository
public class JobProfileDaoImpl implements JobProfileDao {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());


  private static final String TABLE = "job_profiles";
  private static final String ID_FIELD = "'id'";

  private PostgresClientFactory pgClientFactory;

  public JobProfileDaoImpl(@Autowired PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<Optional<JobProfile>> getById(String id, String tenantId) {
    Promise<Results<JobProfile>> promise = Promise.promise();
    try {
      Criteria idCrit = constructCriteria(ID_FIELD, id);
      pgClientFactory.getInstance(tenantId).get(TABLE, JobProfile.class, new Criterion(idCrit), false, promise);
    } catch (Exception e) {
      LOGGER.error(e);
      promise.fail(e);
    }
    return promise.future()
      .map(Results::getResults)
      .map(jobProfiles -> jobProfiles.isEmpty() ? Optional.empty() : Optional.of(jobProfiles.get(0)));
  }


  @Override
  public Future<JobProfile> save(JobProfile jobProfile, String tenantId) {
    Promise<String> promise = Promise.promise();
    pgClientFactory.getInstance(tenantId).save(TABLE, jobProfile.getId(), jobProfile, promise);
    return promise.future().map(jobProfile);
  }

  @Override
  public Future<JobProfile> update(JobProfile jobProfile, String tenantId) {
    Promise<JobProfile> promise = Promise.promise();
    try {
      Criteria idCrit = constructCriteria(ID_FIELD, jobProfile.getId());
      pgClientFactory.getInstance(tenantId).update(TABLE, jobProfile, new Criterion(idCrit), true, updateResult -> {
        if (updateResult.failed()) {
          LOGGER.error("Could not update jobProfile with id {}", jobProfile.getId(), updateResult.cause().getMessage());
          promise.fail(updateResult.cause());
        } else if (updateResult.result().rowCount() != 1) {
          String errorMessage = String.format("JobProfile with id '%s' was not found", jobProfile.getId());
          LOGGER.error(errorMessage);
          promise.fail(new NotFoundException(errorMessage));
        } else {
          promise.complete(jobProfile);
        }
      });
    } catch (Exception e) {
      LOGGER.error("Error updating jobExecution", e);
      promise.fail(e);
    }
    return promise.future().map(jobProfile);
  }

  @Override
  public Future<Boolean> deleteById(String id, String tenantId) {
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClientFactory.getInstance(tenantId).delete(TABLE, id, promise);
    return promise.future().map(updateResult -> updateResult.rowCount() == 1);
  }



  @Override
  public Future<JobProfileCollection> get(String query, int offset, int limit, String tenantId) {
    Promise<Results<JobProfile>> promise = Promise.promise();
    try {
      String[] fieldList = {"*"};
      CQLWrapper cql = HelperUtils.getCQLWrapper(TABLE, query, limit, offset);
      pgClientFactory.getInstance(tenantId).get(TABLE, JobProfile.class, fieldList, cql, true, false, promise);
    } catch (FieldException exception) {
      LOGGER.error("Error while querying jobProfiles", exception);
      promise.fail(exception);
    }
    return promise.future().map(results -> new JobProfileCollection()
      .withJobProfiles(results.getResults())
      .withTotalRecords(results.getResultInfo().getTotalRecords()));

  }

}
