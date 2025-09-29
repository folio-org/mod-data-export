package org.folio.dataexp.service.file.download;

import java.util.ArrayList;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.FileDownload;
import org.folio.dataexp.service.JobExecutionService;
import org.folio.dataexp.util.S3FilePathUtils;
import org.folio.s3.client.FolioS3Client;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link FileDownloadService} for downloading files from S3 storage.
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class FileDownloadServiceImpl implements FileDownloadService {

  private final FolioS3Client s3Client;
  private final JobExecutionService jobExecutionService;

  /**
   * {@inheritDoc}
   */
  @Override
  public FileDownload getFileDownload(UUID jobExecutionId, UUID exportFileId) {
    var jobExecution = jobExecutionService.getById(jobExecutionId);
    var name = new ArrayList<>(jobExecution.getExportedFiles())
        .get(0)
        .getFileName();
    var link = s3Client.getPresignedUrl(
        S3FilePathUtils.getPathToStoredFiles(jobExecutionId.toString(), name)
    );
    return new FileDownload()
        .fileId(exportFileId)
        .link(link);
  }
}
