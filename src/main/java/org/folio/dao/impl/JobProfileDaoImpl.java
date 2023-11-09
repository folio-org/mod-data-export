package org.folio.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.dao.JobProfileDao;
import org.folio.rest.jaxrs.model.JobProfile;
import org.folio.rest.jaxrs.model.JobProfileCollection;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.persist.interfaces.Results;
import org.folio.util.HelperUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.ws.rs.NotFoundException;

import static java.lang.String.format;

@Repository
public class JobProfileDaoImpl implements JobProfileDao {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());


  private static final String TABLE = "job_profiles";
  private static final String JOB_EXECUTIONS_TABLE = "job_executions";

  private PostgresClientFactory pgClientFactory;

  public JobProfileDaoImpl(@Autowired PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<Optional<JobProfile>> getById(String id, String tenantId) {
    Promise<JobProfile> promise = Promise.promise();
    try {
      pgClientFactory.getInstance(tenantId).getById(TABLE, id, JobProfile.class, promise);
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      promise.fail(e);
    }
    return promise.future().map(Optional::ofNullable);
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
      pgClientFactory.getInstance(tenantId).update(TABLE, jobProfile, jobProfile.getId(), updateResult -> {
        if (updateResult.failed()) {
          LOGGER.error("Could not update jobProfile with id {}, cause: {}", jobProfile.getId(), updateResult.cause().getMessage());
          promise.fail(updateResult.cause());
        } else if (updateResult.result().rowCount() != 1) {
          String errorMessage = format("JobProfile with id '%s' was not found", jobProfile.getId());
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
    return promise.future();
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

  @Override
  public Future<JobProfileCollection> getUsed(int offset, int limit, String tenantId) {
    return pgClientFactory.getInstance(tenantId)
        .execute(format("SELECT DISTINCT jobProfileId, jsonb ->> 'jobProfileName' FROM %s_mod_data_export.%s OFFSET %s LIMIT %s;",
            tenantId, JOB_EXECUTIONS_TABLE, offset, limit))
        .map(results -> {
          List<JobProfile> list = new ArrayList<>();
          for (var row: results) {
            list.add(new JobProfile().withName(row.getString(1)).withId(row.getUUID(0).toString()));
          }
          return new JobProfileCollection().withJobProfiles(list);
        });
  }

}
