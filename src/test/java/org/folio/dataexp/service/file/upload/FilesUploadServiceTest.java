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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import org.folio.dataexp.exception.file.definition.UploadFileException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.io.Resource;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class FilesUploadServiceTest {

  private static final String UPLOADED_FILE_PATH = "src/test/resources/upload.csv";

  @Mock private FileDefinitionEntityRepository fileDefinitionEntityRepository;
  @Mock private FolioS3Client s3Client;

  @InjectMocks private FilesUploadServiceImpl fileUploadService;

    @Captor
private ArgumentCaptor<InputStream> inputStreamArgumentCaptor;

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

    @ParameterizedTest
  @EnumSource(
      value = FileDefinition.StatusEnum.class,
      names = {"IN_PROGRESS", "COMPLETED", "ERROR"})
  void uploadFile_shouldThrowUploadFileException_whenDefinitionStatusIsNotNew(
      FileDefinition.StatusEnum status) {
    // TestMate-6c2d4068c3b484b9d051e4c5aabeeb20
    // Given
    var fileDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var resource = new PathResource(UPLOADED_FILE_PATH);
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(fileDefinitionId);
    fileDefinition.setFileName("test.csv");
    fileDefinition.setStatus(status);
    var fileDefinitionEntity =
        FileDefinitionEntity.builder().fileDefinition(fileDefinition).build();
    when(fileDefinitionEntityRepository.getReferenceById(fileDefinitionId))
        .thenReturn(fileDefinitionEntity);
    // When & Then
    var exception =
        assertThrows(
            UploadFileException.class,
            () -> fileUploadService.uploadFile(fileDefinitionId, resource));
    assertEquals(
        "File already uploaded for file definition with id : " + fileDefinitionId,
        exception.getMessage());
    verify(fileDefinitionEntityRepository).getReferenceById(fileDefinitionId);
    verify(s3Client, never()).write(any(), any());
    verify(fileDefinitionEntityRepository, never()).save(any());
  }

    @Test
  @SneakyThrows
  void uploadFile_shouldUseNullInputStream_whenResourceIsNull() {
    // TestMate-71a51c7182c153684de43d4b733f628b
    // Given
    var fileDefinitionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(fileDefinitionId);
    fileDefinition.setFileName("empty-file.csv");
    fileDefinition.setStatus(FileDefinition.StatusEnum.NEW);
    fileDefinition.setMetadata(new Metadata());
    var fileDefinitionEntity =
        FileDefinitionEntity.builder().fileDefinition(fileDefinition).build();
    when(fileDefinitionEntityRepository.getReferenceById(fileDefinitionId))
        .thenReturn(fileDefinitionEntity);
    doAnswer(
            invocation -> {
              InputStream stream = invocation.getArgument(1);
              assertEquals(-1, stream.read());
              return null;
            })
        .when(s3Client)
        .write(any(String.class), any(InputStream.class));
    // When
    var resultFileDefinition = fileUploadService.uploadFile(fileDefinitionId, null);
    // Then
    assertEquals(FileDefinition.StatusEnum.COMPLETED, resultFileDefinition.getStatus());
    verify(fileDefinitionEntityRepository).getReferenceById(fileDefinitionId);
    verify(fileDefinitionEntityRepository, times(2)).save(isA(FileDefinitionEntity.class));
    verify(s3Client).write(any(String.class), any(InputStream.class));
  }

    @Test
  @SneakyThrows
  void uploadFile_shouldThrowIOException_whenS3WriteFails() {
    // TestMate-a0e68b531504650ff5429c76d3d84bc9
    // Given
    var fileDefinitionId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(fileDefinitionId);
    fileDefinition.setFileName("failed-upload.csv");
    fileDefinition.setStatus(FileDefinition.StatusEnum.NEW);
    fileDefinition.setMetadata(new Metadata());
    var fileDefinitionEntity =
        FileDefinitionEntity.builder().fileDefinition(fileDefinition).build();
    var resource = new PathResource(UPLOADED_FILE_PATH);
    when(fileDefinitionEntityRepository.getReferenceById(fileDefinitionId))
        .thenReturn(fileDefinitionEntity);
    doThrow(new RuntimeException("S3 write error"))
        .when(s3Client)
        .write(any(String.class), any(InputStream.class));
    // When & Then
    assertThrows(
        RuntimeException.class, () -> fileUploadService.uploadFile(fileDefinitionId, resource));
    assertEquals(FileDefinition.StatusEnum.IN_PROGRESS, fileDefinition.getStatus());
    verify(fileDefinitionEntityRepository).getReferenceById(fileDefinitionId);
    verify(fileDefinitionEntityRepository).save(isA(FileDefinitionEntity.class));
    verify(s3Client).write(any(String.class), any(InputStream.class));
  }
}
