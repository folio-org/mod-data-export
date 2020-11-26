package org.folio.service.file.upload;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.QuickExportRequest;
import org.folio.service.file.storage.FileStorage;
import org.folio.util.OkapiConnectionParams;

/**
 * File upload service. Provides lifecycle methods for file uploading functionality.
 */
public interface FileUploadService {
  /**
   * Starts uploading for the given {@link FileDefinition} by id
   *
   * @param fileDefinitionId id of the {@link FileDefinition}
   * @param tenantId         tenant id
   * @return {@link FileDefinition}
   */
  Future<FileDefinition> startUploading(String fileDefinitionId, String tenantId);

  /**
   * Saves given file data to the {@link FileStorage}
   *
   * @param fileDefinition {@link FileDefinition}
   * @param data           array of file bytes
   * @return {@link FileDefinition}
   */
  Future<FileDefinition> saveFileChunk(FileDefinition fileDefinition, byte[] data, String tenantId);

  /**
   * Completes uploading for the given {@link FileDefinition}
   *
   * @param fileDefinition {@link FileDefinition}
   * @param tenantId       tenant id
   * @return {@link FileDefinition}
   */
  Future<FileDefinition> completeUploading(FileDefinition fileDefinition, String tenantId);

  /**
   * Saves given file data to the {@link FileStorage}
   *
   * @param fileDefinition {@link FileDefinition}
   * @param query request query to retrieve uuids
   * @param params okapi connection param {@link OkapiConnectionParams}
   * @return {@link FileDefinition}
   */
  Future<FileDefinition> saveUUIDsByCQL(FileDefinition fileDefinition, String query, OkapiConnectionParams params);

  /**
   * Aborts uploading for the given {@link FileDefinition} by id
   *
   * @param fileDefinitionId id of the {@link FileDefinition}
   * @param tenantId         tenant id
   * @return {@link FileDefinition}
   */
  Future<FileDefinition> errorUploading(String fileDefinitionId, String tenantId);

  /**
   * Upload file for quick export with {@link FileDefinition}
   *
   * @param request        {@link QuickExportRequest}
   * @param fileDefinition {@link FileDefinition}
   * @param params         {@link OkapiConnectionParams}
   * @return {@link FileDefinition}
   */
  Future<FileDefinition> uploadFileDependsOnTypeForQuickExport(QuickExportRequest request, FileDefinition fileDefinition, OkapiConnectionParams params);
}
