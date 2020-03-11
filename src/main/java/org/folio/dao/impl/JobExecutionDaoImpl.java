package org.folio.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.ws.rs.NotFoundException;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.UUID;

import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.dao.JobExecutionDao;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.rest.jaxrs.model.JobExecutionCollection;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.persist.interfaces.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static org.folio.dao.impl.FileDefinitionDaoImpl.constructCriteria;

@Repository
public class JobExecutionDaoImpl implements JobExecutionDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String TABLE = "job_executions";
  private static final String ID_FIELD = "'id'";

  @Autowired
  private PostgresClientFactory pgClientFactory;

  @Override
  public Future<JobExecutionCollection> get(String query, int offset, int limit, String tenantId) {
    Promise<Results<JobExecution>> promise = Promise.promise();
    try {
      String[] fieldList = {"*"};
      CQLWrapper cql = getCQLWrapper(TABLE, query, limit, offset);
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
    jobExecution.setId(UUID.randomUUID().toString());
    pgClientFactory.getInstance(tenantId).save(TABLE, jobExecution.getId(), jobExecution, promise);
    return promise.future().map(jobExecution);
  }

  @Override
  public Future<JobExecution> update(JobExecution jobExecution, String tenantId) {
    Promise<JobExecution> promise = Promise.promise();
    try {
      Criteria idCrit = constructCriteria(ID_FIELD, jobExecution.getId());
      pgClientFactory.getInstance(tenantId).update(TABLE, jobExecution, new Criterion(idCrit), true, updateResult -> {
        if (updateResult.failed()) {
          LOGGER.error("Could not update jobExecution with id {}", jobExecution.getId(), updateResult.cause());
          promise.fail(updateResult.cause());
        } else if (updateResult.result().getUpdated() != 1) {
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
    Promise<Results<JobExecution>> promise = Promise.promise();
    try {
      Criteria idCrit = constructCriteria(ID_FIELD, jobExecutionId);
      pgClientFactory.getInstance(tenantId).get(TABLE, JobExecution.class, new Criterion(idCrit), false, promise);
    } catch (Exception e) {
      LOGGER.error(e);
      promise.fail(e);
    }
    return promise.future()
      .map(Results::getResults)
      .map(fileDefinitions -> fileDefinitions.isEmpty() ? Optional.empty() : Optional.of(fileDefinitions.get(0)));
  }

  /**
   * Builds CQLWrapper by which db result is filtered
   *
   * @param tableName - json key name
   * @param query     - query string to filter jobExecutions based on matching criteria in fields
   * @param limit     - limit of records for pagination
   * @param offset    - starting index in a list of results
   * @return - CQLWrapper
   */
  private CQLWrapper getCQLWrapper(String tableName, String query, int limit, int offset) throws FieldException {
    CQL2PgJSON cql2pgJson = new CQL2PgJSON(tableName + ".jsonb");
    return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
  }

}
