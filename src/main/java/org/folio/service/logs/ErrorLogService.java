package org.folio.service.logs;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;

public interface ErrorLogService {

  /**
   * Returns {@link ErrorLogCollection} grouped by the jobExecutioId
   *
   * @param jobExecutionid id of job execution
   * @return future with {@link ErrorLogCollection}
   */
  Future<ErrorLogCollection> getByJobExecutionId(String jobExecutionid, int offset, int limit, String tenantId);

  /**
   * Saves {@link ErrorLog} to database
   *
   * @param errorLog {@link ErrorLog} to save
   * @param tenantId  id of specific tenant
   * @return future with saved {@link ErrorLog}
   */
  Future<ErrorLog> save(ErrorLog errorLog, String tenantId);

  /**
   * Updates {@link ErrorLog}
   *
   * @param errorLog {@link ErrorLog} to update
   * @param tenantId  id of specific tenant
   * @return future with {@link ErrorLog}
   */
  Future<ErrorLog> update(ErrorLog errorLog, String tenantId);

  /**
   * Delete {@link ErrorLog} by id
   *
   * @param id  errorLog id
   * @param tenantId tenant id
   * @return future with {@link ErrorLog}
   */
  Future<Boolean> deleteById(String id, String tenantId);

  /**
   * Creates and saves {@link ErrorLog} to database
   *
   * @param reason the reason of the error
   * @param jobExecutionId  id of specific job execution
   * @param tenantId  id of specific tenant
   * @return future with saved {@link ErrorLog}
   */
  Future<ErrorLog> saveGeneralError(String reason, String jobExecutionId, String tenantId);

  /**
   * Creates and saves {@link ErrorLog} to database

   * @param record {@link JsonObject} inventory record that is cause of the error
   * @param reason the reason of the error
   * @param jobExecutionId  id of specific job execution
   * @param tenantId  id of specific tenant
   * @return future with saved {@link ErrorLog}
   */
  Future<ErrorLog> saveWithAffectedRecord(JsonObject record, String reason, String jobExecutionId, String tenantId);

}
