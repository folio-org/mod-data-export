package org.folio.service.logs;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.folio.processor.error.TranslationException;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.util.OkapiConnectionParams;

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
   * @param errorMessageCode the code of the error message
   * @param jobExecutionId id of specific job execution
   * @param tenantId       id of specific tenant
   * @return future with saved {@link ErrorLog}
   */
  Future<ErrorLog> saveGeneralError(String errorMessageCode, String jobExecutionId, String tenantId);

  /**
   * Creates and saves {@link ErrorLog} with values for error message to database
   *
   * @param errorMessageCode the code of the error message
   * @param errorMessageValues the values to replace placeholders in error message
   * @param jobExecutionId id of specific job execution
   * @param tenantId       id of specific tenant
   * @return future with saved {@link ErrorLog}
   */
  Future<ErrorLog> saveGeneralErrorWithMessageValues(String errorMessageCode, List<String> errorMessageValues, String jobExecutionId, String tenantId);

  /**
   * Creates and saves {@link ErrorLog} to database
   *
   * @param record         {@link JsonObject} inventory record that is cause of the error
   * @param errorMessageCode the code of the error message
   * @param errorMessageValues the values to replace placeholders in error message
   * @param jobExecutionId id of specific job execution
   * @param params         okapi connection parameters
   * @return future with saved {@link ErrorLog}
   */
  Future<ErrorLog> saveWithAffectedRecord(JsonObject record, String errorMessageCode, List<String> errorMessageValues, String jobExecutionId, TranslationException recordInfo, OkapiConnectionParams params);

  /**
   * Gets {@link ErrorLog}
   *
   * @param jobExecutionId id of job execution
   * @param notFoundUUIDs  collection with UUIDs that not found
   * @param tenantId       tenant id
   */
  void populateUUIDsNotFoundErrorLog(String jobExecutionId, Collection<String> notFoundUUIDs, String tenantId);

  /**
   * Gets {@link ErrorLog}
   *
   * @param jobExecutionId        id of job execution
   * @param numberOfNotFoundUUIDs number of not found UUIDs
   * @param tenantId              tenant id
   */
  void populateUUIDsNotFoundNumberErrorLog(String jobExecutionId, int numberOfNotFoundUUIDs, String tenantId);

  /**
   * If error log with description from error code is present - then true, otherwise - false
   *
   * @param errorCodes            error codes using for querying the logs
   * @param jobExecutionId        id of job execution
   * @param tenantId              tenant id
   */
  Future<Boolean> isErrorsByErrorCodePresent(List<String> errorCodes, String jobExecutionId, String tenantId);

}
