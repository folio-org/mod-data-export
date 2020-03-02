package org.folio.dao;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FileDefinition;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Data access object for {@link FileDefinition}
 */
public interface FileDefinitionDao {

  /**
   * Searches for {@link FileDefinition} by id
   *
   * @param id       UploadDefinition id
   * @param tenantId tenant id
   * @return future with optional {@link FileDefinition}
   */
  Future<Optional<FileDefinition>> getById(String id, String tenantId);

  /**
   * Searches for {@link FileDefinition} by status or with updatedDate greater then {@code lastUpdateDate}
   *
   * @param lastUpdateDate time of last fileDefinition changes
   * @param tenantId tenant id
   * @return future with list of {@link FileDefinition}
   */
  Future<List<FileDefinition>> getExpiredEntries(Date lastUpdateDate, String tenantId);

  /**
   * Saves {@link FileDefinition} to database
   *
   * @param fileDefinition {@link FileDefinition} to save
   * @param tenantId       tenant id
   * @return future with id of saved {@link FileDefinition}
   */
  Future<FileDefinition> save(FileDefinition fileDefinition, String tenantId);

  /**
   * Updates {@link FileDefinition}
   *
   * @param fileDefinition {@link FileDefinition} to update
   * @param tenantId       tenant id
   * @return future with {@link FileDefinition}
   */
  Future<FileDefinition> update(FileDefinition fileDefinition, String tenantId);

  /**
   * Deletes {@link FileDefinition} from database
   *
   * @param id id of {@link FileDefinition} to delete
   * @param tenantId tenant id
   * @return future with true is succeeded
   */
  Future<Boolean> deleteById(String id, String tenantId);
}
