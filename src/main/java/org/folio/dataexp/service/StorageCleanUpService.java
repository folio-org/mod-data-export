package org.folio.dataexp.service;

import static java.util.concurrent.TimeUnit.HOURS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.util.S3FilePathUtils;
import org.folio.s3.client.FolioS3Client;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageCleanUpService {
  private final FileDefinitionEntityRepository fileDefinitionEntityRepository;
  private final FolioS3Client storageClient;
  private final ExportIdEntityRepository exportIdEntityRepository;

  public void cleanExpiredFilesAndFileDefinitions() {
    var expirationDate = new Date(new Date().getTime() - HOURS.toMillis(1));
    var expiredFileDefinitions = fileDefinitionEntityRepository.getExpiredEntities(expirationDate);
    log.info("Removing files and file definitions, number of file definitions to clean up: {}", expiredFileDefinitions.size());
    expiredFileDefinitions.forEach(fileDefinitionEntity -> {
      var fileDefinition = fileDefinitionEntity.getFileDefinition();
      storageClient.remove(S3FilePathUtils.getPathToUploadedFiles(fileDefinition.getId(), fileDefinition.getFileName()));
      fileDefinitionEntityRepository.delete(fileDefinitionEntity);
    });
  }

  @Transactional
  public void cleanExportIdEntities(UUID jobExecutionId) {
    exportIdEntityRepository.deleteByJobExecutionId(jobExecutionId);
  }
}
