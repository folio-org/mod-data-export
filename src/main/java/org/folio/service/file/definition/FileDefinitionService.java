package org.folio.service.file.definition;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.QuickExportRequest;

public interface FileDefinitionService {

  /**
   * Searches for {@link FileDefinition} by id
   *
   * @param id       UploadDefinition id
   * @param tenantId tenant id
   * @return future with optional {@link FileDefinition}
   */
  Future<FileDefinition> getById(String id, String tenantId);

  /**
   * Saves {@link FileDefinition} to database
   *
   * @param fileDefinition {@link FileDefinition} to save
   * @param tenantId       tenant id
   * @return future with saved {@link FileDefinition}
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
   * Create {@link JobData} with related jobExecution and jobData
   *
   * @param request  {@link QuickExportRequest}
   * @param tenantId tenant id
   * @return future with {@link JobData}
   */
  Future<JobData> prepareJobDataForQuickExport(QuickExportRequest request, String jobProfileId, String tenantId);

}
