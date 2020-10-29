package org.folio.service.logs;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.folio.rest.persist.Criteria.Criterion;

import java.util.Collection;
import java.util.List;

public interface ErrorLogService {

  /**
   * Returns {@link ErrorLogCollection} grouped by the jobExecutioId
   *
   * @param jobExecutionId id of job execution
   * @return future with {@link ErrorLogCollection}
   */
  Future<ErrorLogCollection> get(String jobExecutionId, int offset, int limit, String tenantId);

  /**
   * Gets list of {@link ErrorLog}
   *
   * @param criterion {@link Criterion}
   * @param tenantId  tenant id
   * @return future with list of {@link ErrorLog}
   */
  Future<List<ErrorLog>> getByQuery(Criterion criterion, String tenantId);

  /**
   * Saves {@link ErrorLog} to database
   *
   * @param errorLog {@link ErrorLog} to save
   * @param tenantId id of specific tenant
   * @return future with saved {@link ErrorLog}
   */
  Future<ErrorLog> save(ErrorLog errorLog, String tenantId);

  /**
   * Updates {@link ErrorLog}
   *
   * @param errorLog {@link ErrorLog} to update
   * @param tenantId id of specific tenant
   * @return future with {@link ErrorLog}
   */
  Future<ErrorLog> update(ErrorLog errorLog, String tenantId);

  /**
   * Delete {@link ErrorLog} by id
   *
   * @param id       errorLog id
   * @param tenantId tenant id
   * @return future with {@link ErrorLog}
   */
  Future<Boolean> deleteById(String id, String tenantId);

  /**
   * Creates and saves {@link ErrorLog} to database
   *
   * @param reason         the reason of the error
   * @param jobExecutionId id of specific job execution
   * @param tenantId       id of specific tenant
   * @return future with saved {@link ErrorLog}
   */
  Future<ErrorLog> saveGeneralError(String reason, String jobExecutionId, String tenantId);

  /**
   * Creates and saves {@link ErrorLog} to database
   *
   * @param record         {@link JsonObject} inventory record that is cause of the error
   * @param reason         the reason of the error
   * @param jobExecutionId id of specific job execution
   * @param tenantId       id of specific tenant
   * @return future with saved {@link ErrorLog}
   */
  Future<ErrorLog> saveWithAffectedRecord(JsonObject record, String reason, String jobExecutionId, String tenantId);

  /**
   * Gets {@link ErrorLog}
   *
   * @param jobExecutionId id of job execution
   * @param notFoundUUIDs  collection with UUIDs that not found
   * @param tenantId       tenant id
   */
  void  populateNotFoundUUIDsErrorLog(String jobExecutionId, Collection<String> notFoundUUIDs, String tenantId);

  /**
   * Gets {@link ErrorLog}
   *
   * @param jobExecutionId        id of job execution
   * @param numberOfNotFoundUUIDs number of not found UUIDs
   * @param tenantId              tenant id
   */
  void  populateNotFoundUUIDsNumberErrorLog(String jobExecutionId, int numberOfNotFoundUUIDs, String tenantId);

}
