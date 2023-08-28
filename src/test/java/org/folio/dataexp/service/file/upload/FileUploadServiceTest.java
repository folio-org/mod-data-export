package org.folio.dataexp.service.file.upload;

import lombok.SneakyThrows;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.service.export.storage.FolioS3ClientFactory;
import org.folio.s3.client.MinioS3Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.PathResource;

import java.io.InputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileUploadServiceTest {

  private static final String UPLOADED_FILE_PATH = "src/test/resources/upload.csv";

  @Mock
  private FileDefinitionEntityRepository fileDefinitionEntityRepository;
  @Mock
  private FolioS3ClientFactory folioS3ClientFactory;

  @InjectMocks
  private FileUploadServiceImpl fileUploadService;

  @Test
  @SneakyThrows
  void uploadFileTest() {
    var fileDefinitionId = UUID.randomUUID();

    var fileDefinition = new FileDefinition();
    fileDefinition.setId(fileDefinitionId);
    fileDefinition.fileName("upload.csv");
    fileDefinition.setStatus(FileDefinition.StatusEnum.NEW);

    var fileDefinitionEntity = FileDefinitionEntity.builder().fileDefinition(fileDefinition).build();
    var resource = new PathResource(UPLOADED_FILE_PATH);

    var mockS3 = Mockito.mock(MinioS3Client.class);

    when(fileDefinitionEntityRepository.getReferenceById(fileDefinitionId)).thenReturn(fileDefinitionEntity);
    when(folioS3ClientFactory.getFolioS3Client()).thenReturn(mockS3);

    fileUploadService.uploadFile(fileDefinitionId, resource);

    assertEquals(FileDefinition.StatusEnum.COMPLETED, fileDefinition.getStatus());
    verify(fileDefinitionEntityRepository).getReferenceById(fileDefinitionId);
    verify(fileDefinitionEntityRepository, times(2)).save(isA(FileDefinitionEntity.class));
    verify(mockS3).write(isA(String.class), isA(InputStream.class));
  }

  @Test
  @SneakyThrows
  void errorUploading() {
    var fileDefinitionId = UUID.randomUUID();

    var fileDefinition = new FileDefinition();
    fileDefinition.setId(fileDefinitionId);
    fileDefinition.fileName("upload.csv");
    fileDefinition.setStatus(FileDefinition.StatusEnum.NEW);

    var fileDefinitionEntity = FileDefinitionEntity.builder().fileDefinition(fileDefinition).build();
    when(fileDefinitionEntityRepository.getReferenceById(fileDefinitionId)).thenReturn(fileDefinitionEntity);

    fileUploadService.errorUploading(fileDefinitionId);

    assertEquals(FileDefinition.StatusEnum.ERROR, fileDefinition.getStatus());
    verify(fileDefinitionEntityRepository).getReferenceById(fileDefinitionId);
    verify(fileDefinitionEntityRepository).save(isA(FileDefinitionEntity.class));

  }
}
