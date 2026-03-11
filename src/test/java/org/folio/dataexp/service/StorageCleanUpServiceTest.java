package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.util.S3FilePathUtils;
import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StorageCleanUpServiceTest {

  @Mock private FileDefinitionEntityRepository fileDefinitionEntityRepository;

  @Mock private FolioS3Client storageClient;

  @InjectMocks private StorageCleanUpService storageCleanUpService;

  @Captor private ArgumentCaptor<FileDefinitionEntity> fileDefinitionEntityCaptor;

  private static Stream<String> emptyDelayProvider() {
    return Stream.of(null, "");
  }

  @Test
  @TestMate(name = "TestMate-3d5279a2547feaa5ee0c3879052df208")
  void cleanExpiredFilesAndFileDefinitionsShouldDeleteExpiredFilesAndDefinitions() {
    try (MockedStatic<S3FilePathUtils> s3FilePathUtilsMockedStatic =
        mockStatic(S3FilePathUtils.class)) {
      // Given
      ReflectionTestUtils.setField(storageCleanUpService, "cleanUpFilesDelay", "24");
      var fileDefinition1 =
          new FileDefinition()
              .id(UUID.fromString("a72432c0-8a56-45a4-9264-3990666345a8"))
              .fileName("file1.csv");
      var fileDefinitionEntity1 =
          FileDefinitionEntity.builder().fileDefinition(fileDefinition1).build();
      var path1 = "path/to/file1.csv";
      var fileDefinition2 =
          new FileDefinition()
              .id(UUID.fromString("f36551b3-4983-4f32-a5d6-0a693c10a316"))
              .fileName("file2.csv");
      var fileDefinitionEntity2 =
          FileDefinitionEntity.builder().fileDefinition(fileDefinition2).build();
      var path2 = "path/to/file2.csv";
      var expiredEntities = List.of(fileDefinitionEntity1, fileDefinitionEntity2);
      when(fileDefinitionEntityRepository.getExpiredEntities(any(Date.class)))
          .thenReturn(expiredEntities);
      s3FilePathUtilsMockedStatic
          .when(
              () ->
                  S3FilePathUtils.getPathToUploadedFiles(
                      fileDefinition1.getId(), fileDefinition1.getFileName()))
          .thenReturn(path1);
      s3FilePathUtilsMockedStatic
          .when(
              () ->
                  S3FilePathUtils.getPathToUploadedFiles(
                      fileDefinition2.getId(), fileDefinition2.getFileName()))
          .thenReturn(path2);
      // When
      storageCleanUpService.cleanExpiredFilesAndFileDefinitions();
      // Then
      verify(storageClient, times(2)).remove(any(String.class));
      verify(storageClient).remove(path1);
      verify(storageClient).remove(path2);
      verify(fileDefinitionEntityRepository, times(2)).delete(fileDefinitionEntityCaptor.capture());
      assertThat(fileDefinitionEntityCaptor.getAllValues())
          .containsExactlyInAnyOrder(fileDefinitionEntity1, fileDefinitionEntity2);
    }
  }

  @Test
  @TestMate(name = "TestMate-d683600f14b8bc58f1fe10bde54c70fb")
  void cleanExpiredFilesAndFileDefinitionsShouldDoNothingWhenNoFilesExpired() {
    // Given
    ReflectionTestUtils.setField(storageCleanUpService, "cleanUpFilesDelay", "24");
    when(fileDefinitionEntityRepository.getExpiredEntities(any(Date.class)))
        .thenReturn(Collections.emptyList());
    // When
    storageCleanUpService.cleanExpiredFilesAndFileDefinitions();
    // Then
    verify(storageClient, never()).remove(any(String.class));
    verify(fileDefinitionEntityRepository, never()).delete(any(FileDefinitionEntity.class));
  }

  @ParameterizedTest
  @TestMate(name = "TestMate-d55056655d850cc9e9572e201b440a92")
  @ValueSource(strings = {"invalid-value", "-10"})
  void cleanExpiredFilesAndFileDefinitionsShouldUseDefaultExpirationWhenDelayIsInvalid(
      String invalidDelay) {
    try (MockedStatic<S3FilePathUtils> s3FilePathUtilsMockedStatic =
        mockStatic(S3FilePathUtils.class)) {
      // Given
      ReflectionTestUtils.setField(storageCleanUpService, "cleanUpFilesDelay", invalidDelay);
      var fileDefinition =
          new FileDefinition()
              .id(UUID.fromString("a72432c0-8a56-45a4-9264-3990666345a8"))
              .fileName("expired_file.csv");
      var fileDefinitionEntity =
          FileDefinitionEntity.builder().fileDefinition(fileDefinition).build();
      var expiredEntities = List.of(fileDefinitionEntity);
      var expectedPath = "path/to/expired_file.csv";
      when(fileDefinitionEntityRepository.getExpiredEntities(any(Date.class)))
          .thenReturn(expiredEntities);
      s3FilePathUtilsMockedStatic
          .when(() -> S3FilePathUtils.getPathToUploadedFiles(any(UUID.class), anyString()))
          .thenReturn(expectedPath);
      // When
      storageCleanUpService.cleanExpiredFilesAndFileDefinitions();
      // Then
      verify(storageClient).remove(expectedPath);
      verify(fileDefinitionEntityRepository).delete(fileDefinitionEntityCaptor.capture());
      assertThat(fileDefinitionEntityCaptor.getValue()).isEqualTo(fileDefinitionEntity);
    }
  }

  @ParameterizedTest
  @TestMate(name = "TestMate-02eafb490bd2a9cdd3ef22c8628deabf")
  @MethodSource("emptyDelayProvider")
  void cleanExpiredFilesAndFileDefinitionsShouldUseDefaultExpirationWhenDelayIsEmpty(
      String emptyDelay) {
    try (MockedStatic<S3FilePathUtils> s3FilePathUtilsMockedStatic =
        mockStatic(S3FilePathUtils.class)) {
      // Given
      ReflectionTestUtils.setField(storageCleanUpService, "cleanUpFilesDelay", emptyDelay);
      var fileDefinition =
          new FileDefinition()
              .id(UUID.fromString("a72432c0-8a56-45a4-9264-3990666345a8"))
              .fileName("expired-file.csv");
      var fileDefinitionEntity =
          FileDefinitionEntity.builder().fileDefinition(fileDefinition).build();
      var expiredEntities = List.of(fileDefinitionEntity);
      var expectedPath = "path/to/expired-file.csv";
      when(fileDefinitionEntityRepository.getExpiredEntities(any(Date.class)))
          .thenReturn(expiredEntities);
      s3FilePathUtilsMockedStatic
          .when(() -> S3FilePathUtils.getPathToUploadedFiles(any(UUID.class), anyString()))
          .thenReturn(expectedPath);
      // When
      storageCleanUpService.cleanExpiredFilesAndFileDefinitions();
      // Then
      verify(storageClient).remove(expectedPath);
      verify(fileDefinitionEntityRepository).delete(fileDefinitionEntityCaptor.capture());
      assertThat(fileDefinitionEntityCaptor.getValue()).isEqualTo(fileDefinitionEntity);
    }
  }
}
