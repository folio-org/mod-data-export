package org.folio.dao.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.folio.dao.FileDefinitionDao;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.interfaces.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

import static java.lang.String.format;
import static org.folio.rest.persist.PostgresClient.convertToPsqlStandard;

@Repository
public class FileDefinitionDaoImpl implements FileDefinitionDao {
  private final Logger logger = LoggerFactory.getLogger(FileDefinitionDaoImpl.class);
  private static final String TABLE = "file_definitions";
  private static final String ID_FIELD = "'id'";

  private PostgresClientFactory pgClientFactory;

  public FileDefinitionDaoImpl(@Autowired PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
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
}
