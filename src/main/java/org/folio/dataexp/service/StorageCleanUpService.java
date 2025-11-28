package org.folio.dataexp.service;

import static java.util.concurrent.TimeUnit.HOURS;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.util.S3FilePathUtils;
import org.folio.s3.client.FolioS3Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for cleaning up expired files and file definitions from storage. */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageCleanUpService {
  private static final int DEFAULT_EXPIRATION_PERIOD = 24;

  private final FileDefinitionEntityRepository fileDefinitionEntityRepository;
  private final FolioS3Client storageClient;
  private final ExportIdEntityRepository exportIdEntityRepository;

  @Value("${application.clean-up-files-delay}")
  private String cleanUpFilesDelay;

  /** Removes expired files and file definitions from storage. */
  public void cleanExpiredFilesAndFileDefinitions() {
    var expirationDate =
        new Date(new Date().getTime() - HOURS.toMillis(getExpirationPeriod(cleanUpFilesDelay)));
    var expiredFileDefinitions = fileDefinitionEntityRepository.getExpiredEntities(expirationDate);
    log.info(
        "Removing files and file definitions, number of file definitions to clean up: {}",
        expiredFileDefinitions.size());
    expiredFileDefinitions.forEach(
        fileDefinitionEntity -> {
          var fileDefinition = fileDefinitionEntity.getFileDefinition();
          storageClient.remove(
              S3FilePathUtils.getPathToUploadedFiles(
                  fileDefinition.getId(), fileDefinition.getFileName()));
          fileDefinitionEntityRepository.delete(fileDefinitionEntity);
        });
  }

  /**
   * Removes export ID entities for a given job execution.
   *
   * @param jobExecutionId the job execution UUID
   */
  @Transactional
  public void cleanExportIdEntities(UUID jobExecutionId) {
    exportIdEntityRepository.deleteWithJobExecutionId(jobExecutionId);
  }

  /**
   * Gets the expiration period in hours from the configuration value.
   *
   * @param value the configuration value
   * @return expiration period in hours
   */
  private int getExpirationPeriod(String value) {
    if (isNotEmpty(value)) {
      try {
        return Integer.parseUnsignedInt(value);
      } catch (NumberFormatException e) {
        log.info("Invalid value for file definition expiration: {}, using default 24 hours", value);
      }
    }
    return DEFAULT_EXPIRATION_PERIOD;
  }
}
