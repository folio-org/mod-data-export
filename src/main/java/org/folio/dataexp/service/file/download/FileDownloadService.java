package org.folio.dataexp.service.file.download;

import java.util.UUID;
import org.folio.dataexp.domain.dto.FileDownload;
import org.folio.dataexp.domain.dto.JobExecution;

/**
 * Service interface for file downloading functionality.
 */
public interface FileDownloadService {

  /**
   * Retrieves a {@link FileDownload} entity with information for downloading a file.
   *
   * @param jobExecutionId the ID of the {@link JobExecution}
   * @param exportFileId the ID of the file to download
   * @return {@link FileDownload} containing download information
   */
  FileDownload getFileDownload(UUID jobExecutionId, UUID exportFileId);

}
