package org.folio.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.ErrorLogDao;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.persist.interfaces.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.ws.rs.NotFoundException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import static org.folio.util.HelperUtils.constructCriteria;
import static org.folio.util.HelperUtils.getCQLWrapper;

@Repository
public class ErrorLogDaoImpl implements ErrorLogDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String TABLE = "error_logs";
  private static final String ID_FIELD = "'id'";
  private static final String JOB_EXECUTION_ID_FIELD = "'jobExecutionId'";
  private static final String REASON_FIELD = "'reason'";

  @Autowired
  private PostgresClientFactory pgClientFactory;

  @Override
  public Future<ErrorLogCollection> get(String query, int offset, int limit, String tenantId) {
    Promise<Results<ErrorLog>> promise = Promise.promise();
    try {
      String[] fieldList = {"*"};
      CQLWrapper cql = getCQLWrapper(TABLE, query, limit, offset);
      pgClientFactory.getInstance(tenantId).get(TABLE, ErrorLog.class, fieldList, cql, true, false, promise);
    } catch (Exception e) {
      LOGGER.error(e);
      promise.fail(e);
    }
    return promise.future().map(results -> new ErrorLogCollection()
      .withErrorLogs(results.getResults())
      .withTotalRecords(results.getResultInfo().getTotalRecords()));
  }

  // This method should be changed in scope of MDEXP-318, after adding error code to the error log. we will get error
  // log here by jobExecutionId and errorCode fields
  @Override
  public Future<List<ErrorLog>> getByJobExecutionIdAndReason(String jobExecutionId, String reason, String tenantId) {
    Promise<Results<ErrorLog>> promise = Promise.promise();
    try {
      Criterion expiredEntriesCriterion = constructCriterionByJobExecutionIdAndReason(jobExecutionId, reason);
      pgClientFactory.getInstance(tenantId).get(TABLE, ErrorLog.class, expiredEntriesCriterion, false, promise);
    } catch (Exception e) {
      LOGGER.error("Error during getting errorLog entries by jobExecutionId and reason", e);
      promise.fail(e);
    }
    return promise.future().map(Results::getResults);
  }

  @Override
  public Future<ErrorLog> save(ErrorLog errorLog, String tenantId) {
    Promise<String> promise = Promise.promise();
    pgClientFactory.getInstance(tenantId).save(TABLE, errorLog.getId(), errorLog, promise);
    return promise.future().map(errorLog);
  }

  @Override
  public Future<ErrorLog> update(ErrorLog errorLog, String tenantId) {
    Promise<ErrorLog> promise = Promise.promise();
    try {
      Criteria idCrit = constructCriteria(ID_FIELD, errorLog.getId());
      pgClientFactory.getInstance(tenantId).update(TABLE, errorLog, new Criterion(idCrit), true, updateResult -> {
        if (updateResult.failed()) {
          LOGGER.error("Could not update errorLog with id {}", errorLog.getId(), updateResult.cause().getMessage());
          promise.fail(updateResult.cause());
        } else if (updateResult.result().rowCount() != 1) {
          String errorMessage = String.format("ErrorLog with id '%s' was not found", errorLog.getId());
          LOGGER.error(errorMessage);
          promise.fail(new NotFoundException(errorMessage));
        } else {
          promise.complete(errorLog);
        }
      });
    } catch (Exception e) {
      LOGGER.error("Error updating errorLog", e);
      promise.fail(e);
    }
    return promise.future().map(errorLog);
  }

  @Override
  public Future<Boolean> deleteById(String id, String tenantId) {
    Promise<RowSet<Row>> promise = Promise.promise();
    try {
      Criteria idCrit = constructCriteria(ID_FIELD, id);
      pgClientFactory.getInstance(tenantId).delete(TABLE, new Criterion(idCrit), promise);
    } catch (Exception e) {
      LOGGER.error(e);
      promise.fail(e);
    }
    return promise.future().map(updateResult -> updateResult.rowCount() == 1);
  }

  @Override
  public Future<Optional<ErrorLog>> getById(String id, String tenantId) {
    Promise<Results<ErrorLog>> promise = Promise.promise();
    try {
      Criteria idCrit = constructCriteria(ID_FIELD, id);
      pgClientFactory.getInstance(tenantId).get(TABLE, ErrorLog.class, new Criterion(idCrit), false, promise);
    } catch (Exception e) {
      LOGGER.error(e);
      promise.fail(e);
    }
    return promise.future()
      .map(Results::getResults)
      .map(mappingProfiles -> mappingProfiles.isEmpty() ? Optional.empty() : Optional.of(mappingProfiles.get(0)));
  }

  private Criterion constructCriterionByJobExecutionIdAndReason(String jobExecutionId, String reason) {
    Criterion criterion = new Criterion();
    Criteria jobExecutionIdCriteria = new Criteria();
    jobExecutionIdCriteria.addField(JOB_EXECUTION_ID_FIELD)
      .setOperation("=")
      .setVal(jobExecutionId);
    Criteria reasonCriteria = new Criteria();
    reasonCriteria.addField(REASON_FIELD)
      .setOperation("LIKE")
      .setVal("%" + reason + "%");
    criterion.addCriterion(jobExecutionIdCriteria);
    criterion.addCriterion(reasonCriteria);
    return criterion;
  }

}
