package org.folio.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.dao.JobExecutionDao;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobExecutionCollection;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.persist.interfaces.Results;
import org.folio.util.HelperUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.ws.rs.NotFoundException;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JobExecutionDaoImpl implements JobExecutionDao {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  private static final String TABLE = "job_executions";
  private static final String HR_ID_QUERY = "SELECT nextval('job_execution_hrId')";
  private static final String LAST_UPDATED_DATE_FIELD = "'lastUpdatedDate'";
  private static final String STATUS_FIELD = "'status'";
  private static final SimpleDateFormat DATE_TIME_FORMAT_FOR_POSTGRES = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  @Autowired
  private PostgresClientFactory pgClientFactory;

  @Override
  public Future<JobExecutionCollection> get(String query, int offset, int limit, String tenantId) {
    Promise<Results<JobExecution>> promise = Promise.promise();
    try {
      String[] fieldList = {"*"};
      CQLWrapper cql = HelperUtils.getCQLWrapper(TABLE, query, limit, offset);
      pgClientFactory.getInstance(tenantId).get(TABLE, JobExecution.class, fieldList, cql, true, false, promise);
    } catch (FieldException exception) {
      LOGGER.error("Error while querying jobExecutions", exception);
      promise.fail(exception);
    }
    return promise.future().map(results -> new JobExecutionCollection()
      .withJobExecutions(results.getResults())
      .withTotalRecords(results.getResultInfo().getTotalRecords()));
  }

  @Override
  public Future<JobExecution> save(JobExecution jobExecution, String tenantId) {
    Promise<String> promise = Promise.promise();
    pgClientFactory.getInstance(tenantId).selectSingle(HR_ID_QUERY, getHrIdResult -> {
      if (getHrIdResult.succeeded()) {
        jobExecution.withId(UUID.randomUUID().toString())
          .setHrId(getHrIdResult.result().getInteger(0));
        pgClientFactory.getInstance(tenantId)
          .save(TABLE, jobExecution.getId(), jobExecution, promise);
      } else {
        LOGGER.error("Error while fetching next HRID in sequence: {}", getHrIdResult.cause().getMessage());
        promise.fail(getHrIdResult.cause());
      }
    });
    return promise.future()
      .map(jobExecution);
  }

  @Override
  public Future<JobExecution> update(JobExecution jobExecution, String tenantId) {
    Promise<JobExecution> promise = Promise.promise();
    try {
      pgClientFactory.getInstance(tenantId).update(TABLE, jobExecution, jobExecution.getId(), updateResult -> {
        if (updateResult.failed()) {
          LOGGER.error("Could not update jobExecution with id {}", jobExecution.getId(), updateResult.cause().getMessage());
          promise.fail(updateResult.cause());
        } else if (updateResult.result().rowCount() != 1) {
          String errorMessage = String.format("JobExecution with id '%s' was not found", jobExecution.getId());
          LOGGER.error(errorMessage);
          promise.fail(new NotFoundException(errorMessage));
        } else {
          promise.complete(jobExecution);
        }
      });
    } catch (Exception e) {
      LOGGER.error("Error updating jobExecution", e);
      promise.fail(e);
    }
    return promise.future();
  }

  @Override
  public Future<Optional<JobExecution>> getById(String jobExecutionId, String tenantId) {
    Promise<JobExecution> promise = Promise.promise();
    try {
      pgClientFactory.getInstance(tenantId).getById(TABLE, jobExecutionId, JobExecution.class, promise);
    } catch (Exception e) {
      LOGGER.error(e);
      promise.fail(e);
    }
    return promise.future().map(Optional::ofNullable);
  }


  @Override
  public Future<Boolean> deleteById(String id, String tenantId) {
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClientFactory.getInstance(tenantId).delete(TABLE, id, promise);
    return promise.future().map(updateResult -> updateResult.rowCount() == 1);
  }

  @Override
  public Future<List<JobExecution>> getExpiredEntries(Date expirationDate, String tenantId) {
    LOGGER.debug("Expiration date {}", expirationDate);
    Promise<Results<JobExecution>> promise = Promise.promise();
    try {
      Criterion expiredEntriesCriterion = constructExpiredEntriesCriterion(expirationDate);
      pgClientFactory.getInstance(tenantId).get(TABLE, JobExecution.class, expiredEntriesCriterion, false, promise);
    } catch (Exception e) {
      LOGGER.error("Error during getting fileDefinition entries by expired date", e);
      promise.fail(e);
    }
    return promise.future().map(Results::getResults);

  }

  private Criterion constructExpiredEntriesCriterion(Date expirationDate) {
    Criterion criterion = new Criterion();
    Criteria lastUpdateDateCriteria = new Criteria();
    lastUpdateDateCriteria.addField(LAST_UPDATED_DATE_FIELD)
      .setOperation("<=")
      .setVal(DATE_TIME_FORMAT_FOR_POSTGRES.format(expirationDate));
    Criteria statusIsProgressCriteria = new Criteria();
    statusIsProgressCriteria.addField(STATUS_FIELD)
      .setOperation("=")
      .setVal(String.valueOf(JobExecution.Status.IN_PROGRESS));
    criterion.addCriterion(lastUpdateDateCriteria);
    criterion.addCriterion(statusIsProgressCriteria);
    return criterion;
  }

}
