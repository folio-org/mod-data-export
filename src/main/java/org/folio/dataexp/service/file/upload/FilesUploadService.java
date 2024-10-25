package org.folio.dataexp.service.file.upload;


import org.folio.dataexp.domain.dto.FileDefinition;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.UUID;

/**
 * File upload service. Provides lifecycle methods for file uploading functionality.
 */
public interface FilesUploadService {

  /**
   * Saves given file data to the storage
   *
   * @param fileDefinitionId id of {@link FileDefinition}
   * @param resource         file to upload
   * @return {@link FileDefinition}
   */
  FileDefinition uploadFile(UUID fileDefinitionId, Resource resource) throws IOException;

  /**
   * Aborts uploading for the given {@link FileDefinition} by id
   *
   * @param fileDefinitionId id of the {@link FileDefinition}
   * @return {@link FileDefinition}
   */
  FileDefinition errorUploading(UUID fileDefinitionId);
}
