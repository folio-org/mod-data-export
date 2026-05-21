package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.exception.export.DataExportException;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.export.ExportExecutor;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SingleFileProcessorAsyncTest {

  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private ExportExecutor exportExecutor;
  @InjectMocks private SingleFileProcessorAsync singleFileProcessorAsync;

  @Mock private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;

  @Mock private JobExecutionEntityRepository jobExecutionEntityRepository;

  @Mock private JobExecutionService jobExecutionService;

  @Mock private ErrorLogService errorLogService;

  @InjectMocks private SingleFileProcessor singleFileProcessor;

  @Test
  void executeExportTest() {
    var fileLocation = "mod-data-export/download/download.mrc";
    var exportEntity =
        JobExecutionExportFilesEntity.builder()
            .id(UUID.randomUUID())
            .fileLocation(fileLocation)
            .build();
    var commonFails = new CommonExportStatistic();

    singleFileProcessorAsync.executeExport(exportEntity, new ExportRequest(), commonFails);

    verify(exportExecutor)
        .exportAsynch(eq(exportEntity), isA(ExportRequest.class), eq(commonFails));
  }

  @Test
  @TestMate(name = "TestMate-3a2167a1178ab99324b0f224638f02d2")
  void testExportBySingleFileWhenNoExportsFoundShouldFailJobAndLogCommonErrors() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobExecution = new JobExecution();
    jobExecution.setId(jobExecutionId);
    jobExecution.setProgress(new JobExecutionProgress());
    var commonExportStatistic = new CommonExportStatistic();
    commonExportStatistic.setFailedToReadInputFile(false);
    commonExportStatistic.addToInvalidUuidFormat("invalid-1");
    commonExportStatistic.addToInvalidUuidFormat("invalid-2");
    var exportRequest = new ExportRequest();
    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId))
        .thenReturn(Collections.emptyList());
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    // When
    singleFileProcessor.exportBySingleFile(jobExecutionId, exportRequest, commonExportStatistic);
    // Then
    assertEquals(JobExecution.StatusEnum.FAIL, jobExecution.getStatus());
    assertNotNull(jobExecution.getLastUpdatedDate());
    assertNotNull(jobExecution.getCompletedDate());
    assertEquals(2, jobExecution.getProgress().getFailed());
    assertEquals(0, jobExecution.getProgress().getExported());
    verify(jobExecutionService).save(jobExecution);
    verify(errorLogService).saveCommonExportFailsErrors(commonExportStatistic, 2, jobExecutionId);
    verify(errorLogService, never()).saveFailedToReadInputFileError(any());
  }

  @Test
  @TestMate(name = "TestMate-8e13d4b11e3311305bbabe12feef91bc")
  void testExportBySingleFileWhenNoExportsAndInputFileReadFailedShouldLogSpecificError() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobExecution = new JobExecution();
    jobExecution.setId(jobExecutionId);
    jobExecution.setProgress(new JobExecutionProgress());
    var commonExportStatistic = new CommonExportStatistic();
    commonExportStatistic.setFailedToReadInputFile(true);
    commonExportStatistic.addToInvalidUuidFormat("invalid-uuid-1");
    var exportRequest = new ExportRequest();
    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId))
        .thenReturn(Collections.emptyList());
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    // When
    singleFileProcessor.exportBySingleFile(jobExecutionId, exportRequest, commonExportStatistic);
    // Then
    assertEquals(JobExecution.StatusEnum.FAIL, jobExecution.getStatus());
    assertNotNull(jobExecution.getLastUpdatedDate());
    assertNotNull(jobExecution.getCompletedDate());
    assertEquals(1, jobExecution.getProgress().getFailed());
    assertEquals(0, jobExecution.getProgress().getExported());
    verify(jobExecutionService).save(jobExecution);
    verify(errorLogService).saveFailedToReadInputFileError(jobExecutionId);
    verify(errorLogService, never()).saveCommonExportFailsErrors(any(), anyInt(), any());
  }

  @Test
  @TestMate(name = "TestMate-511a15dc4cb29f7e7cd69edfe0cd6ec4")
  void testExportBySingleFileShouldThrowDataExportExceptionWhenDirectoryCreationFails() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var exportRequest = new ExportRequest();
    var commonExportStatistic = new CommonExportStatistic();
    var exportEntity =
        JobExecutionExportFilesEntity.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
            .jobExecutionId(jobExecutionId)
            .build();
    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId))
        .thenReturn(List.of(exportEntity));
    try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
      mockedFiles
          .when(() -> Files.createDirectories(any(Path.class)))
          .thenThrow(new IOException("FileSystem error"));
      // When
      var exception =
          assertThrows(
              DataExportException.class,
              () ->
                  singleFileProcessor.exportBySingleFile(
                      jobExecutionId, exportRequest, commonExportStatistic));
      // Then
      assertEquals(
          "Can not create temp directory for job execution 00000000-0000-0000-0000-000000000001",
          exception.getMessage());
    }
  }

  @Test
  @TestMate(name = "TestMate-28a237b89cddc11f1b446745a4a44414")
  void testExportBySingleFileShouldProcessAllFilesAndSetLastExportFlag() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var exportRequest = new ExportRequest().all(false);
    var commonExportStatistic = new CommonExportStatistic();
    var entity1 =
        JobExecutionExportFilesEntity.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000011"))
            .jobExecutionId(jobExecutionId)
            .build();
    var entity2 =
        JobExecutionExportFilesEntity.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000012"))
            .jobExecutionId(jobExecutionId)
            .build();
    var entity3 =
        JobExecutionExportFilesEntity.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000013"))
            .jobExecutionId(jobExecutionId)
            .build();
    var entities = List.of(entity1, entity2, entity3);

    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId))
        .thenReturn(entities);

    // Use a list to capture the state of the 'lastExport' flag at the exact moment of invocation
    // because the implementation mutates a shared ExportRequest object.
    java.util.List<Boolean> capturedLastExportFlags = new java.util.ArrayList<>();
    org.mockito.Mockito.doAnswer(
            invocation -> {
              ExportRequest req = invocation.getArgument(1);
              capturedLastExportFlags.add(req.getLastExport());
              return null;
            })
        .when(exportExecutor)
        .export(any(), any(), eq(commonExportStatistic));
    try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
      mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);

      // When
      singleFileProcessor.exportBySingleFile(jobExecutionId, exportRequest, commonExportStatistic);

      // Then
      var entityCaptor = ArgumentCaptor.forClass(JobExecutionExportFilesEntity.class);
      verify(exportExecutor, times(3))
          .export(entityCaptor.capture(), any(ExportRequest.class), eq(commonExportStatistic));

      var capturedEntities = entityCaptor.getAllValues();
      assertThat(capturedEntities.get(0)).isEqualTo(entity1);
      assertThat(capturedEntities.get(1)).isEqualTo(entity2);
      assertThat(capturedEntities.get(2)).isEqualTo(entity3);

      // Verify the flag state recorded at call-time
      assertThat(capturedLastExportFlags.get(0)).isFalse();
      assertThat(capturedLastExportFlags.get(1)).isFalse();
      assertThat(capturedLastExportFlags.get(2)).isTrue();

      assertThat(commonExportStatistic.getExportedRecordsListener()).isNotNull();
    }
  }

  @ParameterizedTest
  @TestMate(name = "TestMate-5daf6dc68ce034d24d627e80bdc25000")
  @CsvSource({"100, false, 500", "100, true, 1000", "600, false, 600", "600, true, 6000"})
  void testExportBySingleFileShouldInitializeListenerWithCorrectProgressStep(
      int exportIdsBatch, boolean isAll, int expectedStep) {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var exportRequest = new ExportRequest().all(isAll);
    var commonExportStatistic = new CommonExportStatistic();
    var exportEntity =
        JobExecutionExportFilesEntity.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
            .jobExecutionId(jobExecutionId)
            .build();
    singleFileProcessor.setExportIdsBatch(exportIdsBatch);
    singleFileProcessor.setExportTmpStorage("tmp");
    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId))
        .thenReturn(List.of(exportEntity));
    try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
      mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
      // When
      singleFileProcessor.exportBySingleFile(jobExecutionId, exportRequest, commonExportStatistic);
      // Then
      var listener = commonExportStatistic.getExportedRecordsListener();
      assertThat(listener).isNotNull();
      var actualStep = (int) ReflectionTestUtils.getField(listener, "progressExportedUpdateStep");
      assertThat(actualStep).isEqualTo(expectedStep);
      verify(exportExecutor).export(eq(exportEntity), eq(exportRequest), eq(commonExportStatistic));
    }
  }
}
