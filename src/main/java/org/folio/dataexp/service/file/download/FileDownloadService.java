package org.folio.dataexp.service.file.download;


import java.util.UUID;

import org.folio.dataexp.domain.dto.FileDownload;
import org.folio.dataexp.domain.dto.JobExecution;

/**
 * File download service. Provides lifecycle methods for file downloading functionality.
 */
public interface FileDownloadService {

  /**
   * Returns {@link FileDownload} entity with information for download file
   *
   * @param jobExecutionId id of {@link JobExecution}
   * @param exportFileId   id of file
   * @return {@link FileDownload}
   */
  FileDownload getFileDownload(UUID jobExecutionId, UUID exportFileId);

}
