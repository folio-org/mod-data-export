package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.service.file.upload.FileUploadServiceImpl.PATTERN_TO_SAVE_FILE;

import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

class StorageCleanUpServiceTest extends BaseDataExportInitializer {
  @Autowired
  private StorageCleanUpService storageCleanUpService;
  @Autowired
  private FolioS3Client s3Client;
  @Autowired
  private FileDefinitionEntityRepository fileDefinitionEntityRepository;

  @Test
  void shouldRemoveExpiredFileAndRelatedFileDefinition() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var fileDefinitionId = UUID.randomUUID();
      var fileName = "file.csv";
      var path = String.format(PATTERN_TO_SAVE_FILE, fileDefinitionId, fileName);

      fileDefinitionEntityRepository.save(FileDefinitionEntity.builder()
        .id(fileDefinitionId)
        .fileDefinition(new FileDefinition()
          .id(fileDefinitionId)
          .sourcePath("path")
          .fileName(fileName)
          .metadata(new Metadata()
            .updatedDate(new Date(new Date().getTime() - TimeUnit.HOURS.toMillis(2)))))
        .build());

      s3Client.write(path, new ByteArrayInputStream("content".getBytes()));
      assertThat(s3Client.read(path)).hasBinaryContent("content".getBytes());

      storageCleanUpService.cleanExpiredFilesAndFileDefinitions();

      assertThat(fileDefinitionEntityRepository.findById(fileDefinitionId)).isEmpty();
      assertThat(s3Client.list(path)).isEmpty();
    }
  }
}
