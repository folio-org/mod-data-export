package org.folio.service.logs;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.folio.util.OkapiConnectionParams;

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
   * @param params   okapi headers and connection parameters
   * @return future with saved {@link ErrorLog}
   */
  Future<ErrorLog> save(ErrorLog errorLog, OkapiConnectionParams params);

  /**
   * Updates {@link ErrorLog}
   *
   * @param errorLog {@link ErrorLog} to update
   * @param params   okapi headers and connection parameters
   * @return future with {@link ErrorLog}
   */
  Future<ErrorLog> update(ErrorLog errorLog, OkapiConnectionParams params);

  /**
   * Delete {@link ErrorLog} by id
   *
   * @param id       errorLof id
   * @param tenantId tenant id
   * @return future with {@link ErrorLog}
   */
  Future<Boolean> deleteById(String id, String tenantId);
}
