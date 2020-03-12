package org.folio.service.export.storage;


import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FileDefinition;
/**
 * File retrieval service. Provides methods for retrieving the exported files.
 */
public interface ExportStorageService {

  /**
   * Fetch the link to download a file for a given job by fileName
   * @param jobExecutionId The job execution ID to which the files are associated
   * @param exportFileName The name of the file to download
   * @param tenantId
   * @return A link using which the file can be downloaded
   */
  Future<String> getFileDownloadLink(String jobExecutionId, String exportFileName, String tenantId);

  /**
   * Store the file in S3
   *
   * @param fileDefinition file definition
   * @param tenantId       tenant id
   */
  void storeFile(FileDefinition fileDefinition, String tenantId);
}
