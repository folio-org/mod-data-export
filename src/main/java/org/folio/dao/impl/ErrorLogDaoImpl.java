package org.folio.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.dao.ErrorLogDao;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.persist.interfaces.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.ws.rs.NotFoundException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import static org.folio.util.HelperUtils.getCQLWrapper;

@Repository
public class ErrorLogDaoImpl implements ErrorLogDao {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  private static final String TABLE = "error_logs";

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

  @Override
  public Future<List<ErrorLog>> getByQuery(Criterion criterion, String tenantId) {
    Promise<Results<ErrorLog>> promise = Promise.promise();
    try {
      pgClientFactory.getInstance(tenantId).get(TABLE, ErrorLog.class, criterion, false, promise);
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
      pgClientFactory.getInstance(tenantId).update(TABLE, errorLog, errorLog.getId(), updateResult -> {
        if (updateResult.failed()) {
          LOGGER.error("Could not update errorLog with id {}, cause: {}", errorLog.getId(), updateResult.cause().getMessage());
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
    return promise.future();
  }

  @Override
  public Future<Boolean> deleteById(String id, String tenantId) {
    Promise<RowSet<Row>> promise = Promise.promise();
    try {
      pgClientFactory.getInstance(tenantId).delete(TABLE, id, promise);
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      promise.fail(e);
    }
    return promise.future().map(updateResult -> updateResult.rowCount() == 1);
  }

  @Override
  public Future<Optional<ErrorLog>> getById(String id, String tenantId) {
    Promise<ErrorLog> promise = Promise.promise();
    try {
      pgClientFactory.getInstance(tenantId).getById(TABLE, id, ErrorLog.class, promise);
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      promise.fail(e);
    }
    return promise.future().map(Optional::ofNullable);
  }

}
