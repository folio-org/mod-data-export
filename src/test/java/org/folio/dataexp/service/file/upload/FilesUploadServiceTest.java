package org.folio.dataexp.service.file.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.PathResource;

@ExtendWith(MockitoExtension.class)
class FilesUploadServiceTest {

  private static final String UPLOADED_FILE_PATH = "src/test/resources/upload.csv";

  @Mock private FileDefinitionEntityRepository fileDefinitionEntityRepository;
  @Mock private FolioS3Client s3Client;

  @InjectMocks private FilesUploadServiceImpl fileUploadService;

  @Test
  @SneakyThrows
  void uploadFileTest() {
    var fileDefinitionId = UUID.randomUUID();

    var fileDefinition = new FileDefinition();
    fileDefinition.setId(fileDefinitionId);
    fileDefinition.fileName("upload.csv");
    fileDefinition.setStatus(FileDefinition.StatusEnum.NEW);
    fileDefinition.setMetadata(new Metadata());

    var fileDefinitionEntity =
        FileDefinitionEntity.builder().fileDefinition(fileDefinition).build();
    var resource = new PathResource(UPLOADED_FILE_PATH);

    when(fileDefinitionEntityRepository.getReferenceById(fileDefinitionId))
        .thenReturn(fileDefinitionEntity);

    fileUploadService.uploadFile(fileDefinitionId, resource);

    assertEquals(FileDefinition.StatusEnum.COMPLETED, fileDefinition.getStatus());
    verify(fileDefinitionEntityRepository).getReferenceById(fileDefinitionId);
    verify(fileDefinitionEntityRepository, times(2)).save(isA(FileDefinitionEntity.class));
    verify(s3Client).write(isA(String.class), isA(InputStream.class));
  }

  @Test
  @SneakyThrows
  void errorUploading() {
    var fileDefinitionId = UUID.randomUUID();

    var fileDefinition = new FileDefinition();
    fileDefinition.setId(fileDefinitionId);
    fileDefinition.fileName("upload.csv");
    fileDefinition.setStatus(FileDefinition.StatusEnum.NEW);
    fileDefinition.setMetadata(new Metadata());

    var fileDefinitionEntity =
        FileDefinitionEntity.builder().fileDefinition(fileDefinition).build();
    when(fileDefinitionEntityRepository.getReferenceById(fileDefinitionId))
        .thenReturn(fileDefinitionEntity);

    fileUploadService.errorUploading(fileDefinitionId);

    assertEquals(FileDefinition.StatusEnum.ERROR, fileDefinition.getStatus());
    verify(fileDefinitionEntityRepository).getReferenceById(fileDefinitionId);
    verify(fileDefinitionEntityRepository).save(isA(FileDefinitionEntity.class));
  }
}
