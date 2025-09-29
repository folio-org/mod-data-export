package org.folio.dataexp.service.file.upload;

import java.io.IOException;
import java.util.UUID;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.springframework.core.io.Resource;

/**
 * Service interface for file uploading functionality.
 * Provides lifecycle methods for file uploading.
 */
public interface FilesUploadService {

  /**
   * Saves given file data to the storage.
   *
   * @param fileDefinitionId id of {@link FileDefinition}
   * @param resource         file to upload
   * @return {@link FileDefinition}
   * @throws IOException if file upload fails
   */
  FileDefinition uploadFile(UUID fileDefinitionId, Resource resource) throws IOException;

  /**
   * Aborts uploading for the given {@link FileDefinition} by id.
   *
   * @param fileDefinitionId id of the {@link FileDefinition}
   * @return {@link FileDefinition}
   */
  FileDefinition errorUploading(UUID fileDefinitionId);
}
