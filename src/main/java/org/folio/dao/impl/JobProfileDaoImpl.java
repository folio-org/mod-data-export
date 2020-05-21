package org.folio.dao.impl;

import static org.folio.util.HelperUtils.constructCriteria;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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


@Repository
public class JobProfileDaoImpl implements JobProfileDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


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
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClientFactory.getInstance(tenantId).update(TABLE, jobProfile, jobProfile.getId(), promise);
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
