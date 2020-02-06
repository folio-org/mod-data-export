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
import org.folio.dao.util.PostgresClientFactory;
import org.folio.rest.jaxrs.model.FileDefinition;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(FileDefinitionDaoImpl.class);
  private static final String TABLE = "file_definitions";
  private static final String GET_BY_ID_QUERY = "SELECT id, file_name, job_execution_id, source_path, created_date, status, size FROM %s.%s WHERE id = ?";
  private static final String INSERT_QUERY = "INSERT INTO %s.%s (id, file_name, job_execution_id, source_path, created_date, status, size) VALUES (?, ?, ?, ?, ?, ?, ?);";
  private static final String UPDATE_BY_ID_QUERY = "UPDATE %s.%s SET file_name = ?, job_execution_id = ?, source_path = ?, created_date = ?, status = ?, size = ? WHERE id = ?";

  @Autowired
  private PostgresClientFactory pgClientFactory;

  @Override
  public Future<Optional<FileDefinition>> getById(String id, String tenantId) {
    Promise<ResultSet> promise = Promise.promise();
    try {
      String query = format(GET_BY_ID_QUERY, convertToPsqlStandard(tenantId), TABLE);
      JsonArray params = new JsonArray().add(id);
      pgClientFactory.getInstance(tenantId).select(query, params, promise);
    } catch (Exception e) {
      LOGGER.error("Error while searching for FileDefinition by id {}", e, id);
      promise.fail(e);
    }
    return promise.future().map(resultSet -> resultSet.getResults().isEmpty()
      ? Optional.empty() : Optional.of(mapEntry(resultSet.getRows().get(0))));
  }

  @Override
  public Future<FileDefinition> save(FileDefinition fileDefinition, String tenantId) {
    Promise<UpdateResult> promise = Promise.promise();
    try {
      String query = format(INSERT_QUERY, convertToPsqlStandard(tenantId), TABLE);
      JsonArray params = new JsonArray()
        .add(fileDefinition.getId())
        .add(fileDefinition.getFileName())
        .add(fileDefinition.getJobExecutionId())
        .add(fileDefinition.getSourcePath())
        .add(Timestamp.from(fileDefinition.getCreatedDate().toInstant()).toString())
        .add(fileDefinition.getStatus())
        .add(fileDefinition.getSize());
      pgClientFactory.getInstance(tenantId).execute(query, params, promise);
    } catch (Exception e) {
      LOGGER.error("Error while saving FileDefinition, id {}", e, fileDefinition.getId());
      promise.fail(e);
    }
    return promise.future().map(updateResult -> fileDefinition);
  }

  @Override
  public Future<FileDefinition> update(FileDefinition fileDefinition, String tenantId) {
    Promise<UpdateResult> promise = Promise.promise();
    try {
      String query = format(UPDATE_BY_ID_QUERY, convertToPsqlStandard(tenantId), TABLE);
      JsonArray params = new JsonArray()
        .add(fileDefinition.getFileName())
        .add(fileDefinition.getJobExecutionId())
        .add(fileDefinition.getSourcePath())
        .add(Timestamp.from(fileDefinition.getCreatedDate().toInstant()).toString())
        .add(fileDefinition.getStatus())
        .add(fileDefinition.getSize())
        .add(fileDefinition.getId());
      pgClientFactory.getInstance(tenantId).execute(query, params, promise);
    } catch (Exception e) {
      LOGGER.error("Error while updating FileDefinition by id '{}'", e, fileDefinition.getId());
      promise.fail(e);
    }
    return promise.future().compose(updateResult -> updateResult.getUpdated() == 1
      ? Future.succeededFuture(fileDefinition)
      : Future.failedFuture(new NotFoundException(format("FileDefinition with id '%s' was not updated", fileDefinition.getId()))));
  }

  private FileDefinition mapEntry(JsonObject entry) {
    return new FileDefinition()
      .withId(entry.getString("id"))
      .withFileName(entry.getString("file_name"))
      .withJobExecutionId(entry.getString("job_execution_id"))
      .withSourcePath(entry.getString("source_path"))
      .withCreatedDate(Date.from(LocalDateTime.parse(entry.getString("created_date")).toInstant(ZoneOffset.UTC)))
      .withStatus(FileDefinition.Status.valueOf(entry.getString("status")))
      .withSize(entry.getInteger("size"));
  }
}
