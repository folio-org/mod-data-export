package org.folio.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.UpdateResult;
import org.apache.commons.lang3.time.TimeZones;
import org.folio.dao.FileDefinitionDao;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.interfaces.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static org.drools.core.util.StringUtils.EMPTY;

@Repository
public class FileDefinitionDaoImpl implements FileDefinitionDao {
  public static final String SOURCE_PATH_FIELD = "'sourcePath'";
  public static final String NOT_EQUAL_OPERATION = "<>";
  public static final String METADATA_FIELD = "'metadata'";
  public static final String UPDATED_DATE_FIELD = "'updatedDate'";
  public static final String LESS_OR_EQUAL_OPERATION = "<=";
  public static final String AND_OPERATION = "AND";
  private final Logger logger = LoggerFactory.getLogger(FileDefinitionDaoImpl.class);
  private static final String TABLE = "file_definitions";
  private static final String ID_FIELD = "'id'";
  private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";

  private PostgresClientFactory pgClientFactory;
  private SimpleDateFormat dateFormatter;

  public FileDefinitionDaoImpl(@Autowired PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
    this.dateFormatter = new SimpleDateFormat(DATE_FORMAT_PATTERN);
    this.dateFormatter.setTimeZone(TimeZone.getTimeZone(TimeZones.GMT_ID));
  }

  @Override
  public Future<Optional<FileDefinition>> getById(String id, String tenantId) {
    Promise<Results<FileDefinition>> promise = Promise.promise();
    try {
      Criteria idCrit = constructCriteria(ID_FIELD, id);
      pgClientFactory.getInstance(tenantId).get(TABLE, FileDefinition.class, new Criterion(idCrit), false, promise);
    } catch (Exception e) {
      logger.error(e);
      promise.fail(e);
    }
    return promise.future()
      .map(Results::getResults)
      .map(fileDefinitions -> fileDefinitions.isEmpty() ? Optional.empty() : Optional.of(fileDefinitions.get(0)));
  }

  @Override
  public Future<List<FileDefinition>> getExpiredEntries(Date lastUpdateDate, String tenantId) {
    Promise<Results<FileDefinition>> promise = Promise.promise();
    try {
      Criterion expiredEntriesCriterion = constructExpiredEntriesCriterion(lastUpdateDate);
      pgClientFactory.getInstance(tenantId).get(TABLE, FileDefinition.class, expiredEntriesCriterion, false, promise);
    } catch (Exception e) {
      logger.error("Error during getting fileDefinition entries by expired date", e);
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
    Promise<UpdateResult> promise = Promise.promise();
    pgClientFactory.getInstance(tenantId).update(TABLE, fileDefinition, fileDefinition.getId(), promise);
    return promise.future().map(fileDefinition);
  }

  @Override
  public Future<Boolean> deleteById(String id, String tenantId) {
    Promise<UpdateResult> promise = Promise.promise();
    pgClientFactory.getInstance(tenantId).delete(TABLE, id, promise);
    return promise.future().map(updateResult -> updateResult.getUpdated() == 1);
  }

  /**
   * Builds criteria by which db result is filtered
   *
   * @param jsonbField - json key name
   * @param value - value corresponding to the key
   * @return - Criteria object
   */
  public static Criteria constructCriteria(String jsonbField, String value) {
    Criteria criteria = new Criteria();
    criteria.addField(jsonbField);
    criteria.setOperation("=");
    criteria.setVal(value);
    return criteria;
  }

  private Criterion constructExpiredEntriesCriterion(Date lastUpdateDate) {
    Criterion criterion = new Criterion();
    Criteria notEmptySourcePathCriteria = new Criteria();
    notEmptySourcePathCriteria.addField(SOURCE_PATH_FIELD)
      .setOperation(NOT_EQUAL_OPERATION)
      .setVal(EMPTY);
    Criteria lastUpdateDateCriteria = new Criteria();
    lastUpdateDateCriteria.addField(METADATA_FIELD)
      .addField(UPDATED_DATE_FIELD)
      .setOperation(LESS_OR_EQUAL_OPERATION)
      .setVal(lastUpdateDate.toString());
    criterion.addCriterion(notEmptySourcePathCriteria, AND_OPERATION, lastUpdateDateCriteria);
    return criterion;
  }

}
