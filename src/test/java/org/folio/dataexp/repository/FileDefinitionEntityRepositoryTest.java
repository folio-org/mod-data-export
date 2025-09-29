package org.folio.dataexp.repository;

import static java.util.concurrent.TimeUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.UUID;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FileDefinitionEntityRepositoryTest extends BaseDataExportInitializer {
  @Autowired
  FileDefinitionEntityRepository fileDefinitionEntityRepository;

  @Test
  void shouldGetExpiredFileDefinitions() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var expectedId = UUID.randomUUID();
      var expirationDate = new Date(new Date().getTime() - HOURS.toMillis(1));
      fileDefinitionEntityRepository.save(FileDefinitionEntity.builder()
          .id(expectedId)
          .fileDefinition(new FileDefinition()
              .id(expectedId)
              .sourcePath("path")
              .metadata(new Metadata().updatedDate(new Date(new Date().getTime()
                  - HOURS.toMillis(2)))))
          .build());
      fileDefinitionEntityRepository.save(FileDefinitionEntity.builder()
          .id(UUID.randomUUID())
          .fileDefinition(new FileDefinition()
              .id(UUID.randomUUID())
              .metadata(new Metadata().updatedDate(new Date(new Date().getTime()
                  - HOURS.toMillis(2)))))
          .build());
      fileDefinitionEntityRepository.save(FileDefinitionEntity.builder()
          .id(UUID.randomUUID())
          .fileDefinition(new FileDefinition()
              .id(UUID.randomUUID())
              .sourcePath("path2")
              .metadata(new Metadata().updatedDate(new Date())))
          .build());

      var res = fileDefinitionEntityRepository.getExpiredEntities(expirationDate);

      assertThat(res).hasSize(1);
      var actualFileDefinition = res.get(0).getFileDefinition();
      assertThat(actualFileDefinition.getId()).isEqualTo(expectedId);
    }
  }
}
