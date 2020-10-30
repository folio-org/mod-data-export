package org.folio.dao;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ErrorLogCollection;
import org.folio.rest.persist.Criteria.Criterion;

import java.util.List;
import java.util.Optional;

public interface ErrorLogDao {

  /**
   * Gets {@link ErrorLog}
   *
   * @param jobExecutionId id of job execution
   * @param tenantId       tenant id
   * @return future with {@link ErrorLogCollection}
   */
  Future<ErrorLogCollection> get(String jobExecutionId, int offset, int limit, String tenantId);

  /**
   * Gets {@link ErrorLog}
   *
   * @param criterion {@link Criterion}
   * @param tenantId  tenant id
   * @return future with list of {@link ErrorLog}
   */
  Future<List<ErrorLog>> getByQuery(Criterion criterion, String tenantId);

  /**
   * Saves {@link ErrorLog}
   *
   * @param errorLog errorLog to save
   * @param tenantId tenant id
   * @return future with id of saved {@link ErrorLog}
   */
  Future<ErrorLog> save(ErrorLog errorLog, String tenantId);

  /**
   * Updates {@link ErrorLog}
   *
   * @param errorLog job to update
   * @param tenantId tenant id
   * @return future
   */
  Future<ErrorLog> update(ErrorLog errorLog, String tenantId);

  /**
   * Deletes {@link ErrorLog} from database
   *
   * @param id       id of {@link ErrorLog} to delete
   * @param tenantId tenant id
   * @return future with true is succeeded
   */
  Future<Boolean> deleteById(String id, String tenantId);

  /**
   * Gets {@link ErrorLog} from database
   *
   * @param id       id of {@link ErrorLog} to return
   * @param tenantId tenant id
   * @return future with {@link ErrorLog}
   */
  Future<Optional<ErrorLog>> getById(String id, String tenantId);

}
