package org.folio.dataexp.service;

import static java.util.concurrent.TimeUnit.HOURS;
import static org.folio.dataexp.service.file.upload.FileUploadServiceImpl.PATTERN_TO_SAVE_FILE;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.service.export.storage.FolioS3ClientFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageCleanUpService {
  private final FileDefinitionEntityRepository fileDefinitionEntityRepository;
  private final FolioS3ClientFactory folioS3ClientFactory;

  public void cleanExpiredFilesAndFileDefinitions() {
    var expirationDate = new Date(new Date().getTime() - HOURS.toMillis(1));
    var expiredFileDefinitions = fileDefinitionEntityRepository.getExpiredEntities(expirationDate);
    var storageClient = folioS3ClientFactory.getFolioS3Client();
    log.info("Removing files and file definitions, number of file definitions to clean up: {}", expiredFileDefinitions.size());
    expiredFileDefinitions.forEach(fileDefinitionEntity -> {
      var fileDefinition = fileDefinitionEntity.getFileDefinition();
      storageClient.remove(String.format(PATTERN_TO_SAVE_FILE, fileDefinition.getId(), fileDefinition.getFileName()));
      fileDefinitionEntityRepository.delete(fileDefinitionEntity);
    });
  }
}
