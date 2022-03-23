package org.folio.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.folio.dao.FileDefinitionDao;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.interfaces.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Repository
public class FileDefinitionDaoImpl implements FileDefinitionDao {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  private static final String SOURCE_PATH_FIELD = "'sourcePath'";
  private static final String NOT_EQUAL_OPERATION = "<>";
  private static final String METADATA_FIELD = "'metadata'";
  private static final String UPDATED_DATE_FIELD = "'updatedDate'";
  private static final String LESS_OR_EQUAL_OPERATION = "<=";
  private static final String AND_OPERATION = "AND";
  private static final String TABLE = "file_definitions";

  private PostgresClientFactory pgClientFactory;

  public FileDefinitionDaoImpl(@Autowired PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<Optional<FileDefinition>> getById(String id, String tenantId) {
    Promise<FileDefinition> promise = Promise.promise();
    try {
      pgClientFactory.getInstance(tenantId).getById(TABLE, id, FileDefinition.class, promise);
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      promise.fail(e);
    }
    return promise.future().map(Optional::ofNullable);
  }

  @Override
  public Future<List<FileDefinition>> getExpiredEntries(Date expirationDate, String tenantId) {
    Promise<Results<FileDefinition>> promise = Promise.promise();
    try {
      Criterion expiredEntriesCriterion = constructExpiredEntriesCriterion(expirationDate);
      pgClientFactory.getInstance(tenantId).get(TABLE, FileDefinition.class, expiredEntriesCriterion, false, promise);
    } catch (Exception e) {
      LOGGER.error("Error during getting fileDefinition entries by expired date", e);
      promise.fail(e);
    }
    return promise.future().map(Results::getResults);
  }

  @Override
  public Future<FileDefinition> save(FileDefinition fileDefinition, String tenantId) {
    Promise<String> promise = Promise.promise();
    pgClientFactory.getInstance(tenantId).save(TABLE, fileDefinition.getId(), fileDefinition, promise);
    return promise.future().map(fileDefinition);
  }

  @Override
  public Future<FileDefinition> update(FileDefinition fileDefinition, String tenantId) {
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClientFactory.getInstance(tenantId).update(TABLE, fileDefinition, fileDefinition.getId(), promise);
    return promise.future().map(fileDefinition);
  }

  @Override
  public Future<Boolean> deleteById(String id, String tenantId) {
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClientFactory.getInstance(tenantId).delete(TABLE, id, promise);
    return promise.future().map(updateResult -> updateResult.rowCount() == 1);
  }


  private Criterion constructExpiredEntriesCriterion(Date expirationDate) {
    Criterion criterion = new Criterion();
    Criteria notEmptySourcePathCriteria = new Criteria();
    notEmptySourcePathCriteria.addField(SOURCE_PATH_FIELD)
      .setOperation(NOT_EQUAL_OPERATION)
      .setVal(EMPTY);
    Criteria lastUpdateDateCriteria = new Criteria();
    lastUpdateDateCriteria.addField(METADATA_FIELD)
      .addField(UPDATED_DATE_FIELD)
      .setOperation(LESS_OR_EQUAL_OPERATION)
      .setVal(expirationDate.toString());
    criterion.addCriterion(notEmptySourcePathCriteria, AND_OPERATION, lastUpdateDateCriteria);
    return criterion;
  }

}
