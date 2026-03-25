package org.folio.dataexp.service.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.exception.export.S3ExportsUploadException;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.service.CommonExportStatistic;
import org.folio.dataexp.service.JobExecutionService;
import org.folio.dataexp.service.StorageCleanUpService;
import org.folio.dataexp.service.export.strategies.ExportStrategyStatistic;
import org.folio.dataexp.service.export.strategies.ExportedRecordsListener;
import org.folio.dataexp.service.export.strategies.InstancesExportStrategy;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportExecutorTest {
  @Mock private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  @Mock private JobExecutionService jobExecutionService;
  @Mock private ExportStrategyFactory exportStrategyFactory;
  @Mock private ErrorLogService errorLogService;
  @Mock private ErrorLogEntityCqlRepository errorLogEntityCqlRepository;
  @Mock private InstancesExportStrategy instancesExportStrategy;
  @Mock private FileDefinitionEntityRepository fileDefinitionEntityRepository;
  @Mock private S3ExportsUploader s3ExportsUploader;
  @Mock private StorageCleanUpService storageCleanUpService;

  @InjectMocks private ExportExecutor exportExecutor;

  @Test
  @SneakyThrows
  void exportTest() {
    var jobExecutionId = UUID.randomUUID();
    var jobExecution = new JobExecution();
    jobExecution.setProgress(new JobExecutionProgress());
    jobExecution.setId(jobExecutionId);
    var fileDefinition = new FileDefinition();
    fileDefinition.setJobExecutionId(jobExecutionId);
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.setFileName("file_name.csv");

    var fileLocation = String.format("mod-data-export/download/%s/download.mrc", jobExecutionId);
    var exportEntity =
        JobExecutionExportFilesEntity.builder()
            .id(UUID.randomUUID())
            .jobExecutionId(jobExecutionId)
            .fileLocation(fileLocation)
            .build();

    var commonExportStatistic = new CommonExportStatistic();
    commonExportStatistic.setExportedRecordsListener(new ExportedRecordsListener(null, 1000, null));

    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    when(jobExecutionExportFilesEntityRepository.getReferenceById(exportEntity.getId()))
        .thenReturn(exportEntity);
    var completedExportEntity =
        JobExecutionExportFilesEntity.builder()
            .id(exportEntity.getId())
            .jobExecutionId(jobExecutionId)
            .status(JobExecutionExportFilesStatus.COMPLETED)
            .fileLocation(fileLocation)
            .build();
    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId))
        .thenReturn(List.of(completedExportEntity));

    var fileDefinitionEntity =
        FileDefinitionEntity.builder()
            .fileDefinition(fileDefinition)
            .id(fileDefinition.getId())
            .build();
    when(fileDefinitionEntityRepository.getFileDefinitionByJobExecutionId(
            jobExecutionId.toString()))
        .thenReturn(List.of(fileDefinitionEntity));
    when(exportStrategyFactory.getExportStrategy(
            new ExportRequest().idType(ExportRequest.IdTypeEnum.INSTANCE)))
        .thenReturn(instancesExportStrategy);
    when(instancesExportStrategy.saveOutputToLocalStorage(
            isA(JobExecutionExportFilesEntity.class),
            isA(ExportRequest.class),
            isA(ExportedRecordsListener.class)))
        .thenReturn(new ExportStrategyStatistic(new ExportedRecordsListener(null, 1000, null)));

    exportExecutor.export(exportEntity, new ExportRequest(), commonExportStatistic);

    assertEquals(JobExecutionExportFilesStatus.ACTIVE, exportEntity.getStatus());
    assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
    verify(s3ExportsUploader).upload(jobExecution, List.of(completedExportEntity), "file_name");
    verify(storageCleanUpService).cleanExportIdEntities(jobExecution.getId());
  }

  @Test
  void exportIfCommonFailsExistTest() {
    var jobExecutionId = UUID.randomUUID();
    var jobExecution = new JobExecution();
    jobExecution.setProgress(new JobExecutionProgress());
    jobExecution.setId(jobExecutionId);
    var fileDefinition = new FileDefinition();
    fileDefinition.setJobExecutionId(jobExecutionId);
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.setFileName("file_name.csv");

    var commonExportStatistic = new CommonExportStatistic();
    commonExportStatistic.incrementDuplicatedUuid();
    commonExportStatistic.addToInvalidUuidFormat("abs");
    commonExportStatistic.setExportedRecordsListener(new ExportedRecordsListener(null, 1000, null));

    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);

    var fileLocation = String.format("mod-data-export/download/%s/download.mrc", jobExecutionId);
    var exportEntity =
        JobExecutionExportFilesEntity.builder()
            .id(UUID.randomUUID())
            .jobExecutionId(jobExecutionId)
            .fileLocation(fileLocation)
            .build();
    var completedExportEntity =
        JobExecutionExportFilesEntity.builder()
            .id(exportEntity.getId())
            .jobExecutionId(jobExecutionId)
            .status(JobExecutionExportFilesStatus.COMPLETED_WITH_ERRORS)
            .fileLocation(fileLocation)
            .build();
    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId))
        .thenReturn(List.of(completedExportEntity));
    when(jobExecutionExportFilesEntityRepository.getReferenceById(exportEntity.getId()))
        .thenReturn(exportEntity);
    when(exportStrategyFactory.getExportStrategy(
            new ExportRequest().idType(ExportRequest.IdTypeEnum.INSTANCE)))
        .thenReturn(instancesExportStrategy);
    when(instancesExportStrategy.saveOutputToLocalStorage(
            isA(JobExecutionExportFilesEntity.class),
            isA(ExportRequest.class),
            isA(ExportedRecordsListener.class)))
        .thenReturn(new ExportStrategyStatistic(new ExportedRecordsListener(null, 1000, null)));
    when(errorLogEntityCqlRepository.countByJobExecutionId(isA(UUID.class))).thenReturn(2L);
    var fileDefinitionEntity =
        FileDefinitionEntity.builder()
            .fileDefinition(fileDefinition)
            .id(fileDefinition.getId())
            .build();
    when(fileDefinitionEntityRepository.getFileDefinitionByJobExecutionId(
            jobExecutionId.toString()))
        .thenReturn(List.of(fileDefinitionEntity));

    exportExecutor.export(exportEntity, new ExportRequest(), commonExportStatistic);

    assertEquals(JobExecutionExportFilesStatus.ACTIVE, exportEntity.getStatus());
    assertEquals(JobExecution.StatusEnum.COMPLETED_WITH_ERRORS, jobExecution.getStatus());
    verify(errorLogService).saveCommonExportFailsErrors(commonExportStatistic, 2, jobExecutionId);
    verify(s3ExportsUploader).upload(jobExecution, List.of(completedExportEntity), "file_name");
    verify(storageCleanUpService).cleanExportIdEntities(jobExecution.getId());
  }

  @Test
  @TestMate(name = "TestMate-ce4fdb3c602156189f0233edadf1eb30")
  void testExportWhenS3UploadFailsShouldSetJobStatusToFail() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var fileDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var jobExecution = new JobExecution();
    jobExecution.setId(jobExecutionId);
    jobExecution.setProgress(new JobExecutionProgress());
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(fileDefinitionId);
    fileDefinition.setJobExecutionId(jobExecutionId);
    fileDefinition.setFileName("test_export.csv");
    var exportEntityId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    var exportEntity =
        JobExecutionExportFilesEntity.builder()
            .id(exportEntityId)
            .jobExecutionId(jobExecutionId)
            .fileLocation("mod-data-export/download/file.mrc")
            .status(JobExecutionExportFilesStatus.SCHEDULED)
            .build();
    var commonExportStatistic = new CommonExportStatistic();
    commonExportStatistic.setExportedRecordsListener(new ExportedRecordsListener(null, 1000, null));
    var exportRequest = new ExportRequest();
    var completedExportEntity = exportEntity.withStatus(JobExecutionExportFilesStatus.COMPLETED);
    var fileDefinitionEntity =
        FileDefinitionEntity.builder().id(fileDefinitionId).fileDefinition(fileDefinition).build();
    when(jobExecutionExportFilesEntityRepository.getReferenceById(exportEntityId))
        .thenReturn(exportEntity);
    when(exportStrategyFactory.getExportStrategy(exportRequest))
        .thenReturn(instancesExportStrategy);
    when(instancesExportStrategy.saveOutputToLocalStorage(
            eq(exportEntity), eq(exportRequest), any()))
        .thenReturn(
            new ExportStrategyStatistic(commonExportStatistic.getExportedRecordsListener()));
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId))
        .thenReturn(List.of(completedExportEntity));
    when(fileDefinitionEntityRepository.getFileDefinitionByJobExecutionId(
            jobExecutionId.toString()))
        .thenReturn(List.of(fileDefinitionEntity));
    when(s3ExportsUploader.upload(jobExecution, List.of(completedExportEntity), "test_export"))
        .thenThrow(new S3ExportsUploadException("S3 Upload Failed"));
    // When
    exportExecutor.exportAsynch(exportEntity, exportRequest, commonExportStatistic);
    // Then
    assertEquals(JobExecution.StatusEnum.FAIL, jobExecution.getStatus());
    assertNotNull(jobExecution.getCompletedDate());
    verify(errorLogService)
        .saveGeneralErrorWithMessageValues(
            ErrorCode.INVALID_EXPORT_FILE_DEFINITION_ID.getCode(),
            List.of(fileDefinitionId.toString()),
            jobExecutionId);
    verify(errorLogService)
        .saveGeneralErrorWithMessageValues(
            ErrorCode.NO_FILE_GENERATED.getCode(),
            List.of(ErrorCode.NO_FILE_GENERATED.getDescription()),
            jobExecutionId);
    verify(jobExecutionService).save(jobExecution);
    verify(storageCleanUpService).cleanExportIdEntities(jobExecutionId);
  }

  @Test
  @TestMate(name = "TestMate-91f4310c38a39e7d762d71ffcb06dfeb")
  void testExportWhenAllFilesFailShouldSetJobStatusToFail() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var fileDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var jobExecution = new JobExecution();
    jobExecution.setId(jobExecutionId);
    jobExecution.setProgress(new JobExecutionProgress());
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(fileDefinitionId);
    fileDefinition.setJobExecutionId(jobExecutionId);
    fileDefinition.setFileName("test_export.csv");
    var exportEntityId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    var exportEntity =
        JobExecutionExportFilesEntity.builder()
            .id(exportEntityId)
            .jobExecutionId(jobExecutionId)
            .fileLocation("mod-data-export/download/file.mrc")
            .status(JobExecutionExportFilesStatus.SCHEDULED)
            .build();
    var commonExportStatistic = new CommonExportStatistic();
    commonExportStatistic.setExportedRecordsListener(new ExportedRecordsListener(null, 1000, null));
    var exportRequest = new ExportRequest();
    var failedExportEntity1 = exportEntity.withStatus(JobExecutionExportFilesStatus.FAILED);
    var failedExportEntity2 =
        JobExecutionExportFilesEntity.builder()
            .id(UUID.randomUUID())
            .jobExecutionId(jobExecutionId)
            .status(JobExecutionExportFilesStatus.FAILED)
            .build();
    var fileDefinitionEntity =
        FileDefinitionEntity.builder().id(fileDefinitionId).fileDefinition(fileDefinition).build();
    when(jobExecutionExportFilesEntityRepository.getReferenceById(exportEntityId))
        .thenReturn(exportEntity);
    when(exportStrategyFactory.getExportStrategy(exportRequest))
        .thenReturn(instancesExportStrategy);
    when(instancesExportStrategy.saveOutputToLocalStorage(
            eq(exportEntity), eq(exportRequest), any()))
        .thenReturn(
            new ExportStrategyStatistic(commonExportStatistic.getExportedRecordsListener()));
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId))
        .thenReturn(List.of(failedExportEntity1, failedExportEntity2));
    when(fileDefinitionEntityRepository.getFileDefinitionByJobExecutionId(
            jobExecutionId.toString()))
        .thenReturn(List.of(fileDefinitionEntity));
    // When
    exportExecutor.exportAsynch(exportEntity, exportRequest, commonExportStatistic);
    // Then
    assertEquals(JobExecution.StatusEnum.FAIL, jobExecution.getStatus());
    assertNotNull(jobExecution.getCompletedDate());
    verify(jobExecutionService).save(jobExecution);
    verify(storageCleanUpService).cleanExportIdEntities(jobExecutionId);
  }

  @Test
  @TestMate(name = "TestMate-055e5c486a85795b7238833724c024a6")
  void testExportWhenNotAllFilesFinishedShouldNotUpdateJobStatus() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var exportEntityIdA = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var exportEntityA =
        JobExecutionExportFilesEntity.builder()
            .id(exportEntityIdA)
            .jobExecutionId(jobExecutionId)
            .fileLocation("mod-data-export/download/fileA.mrc")
            .status(JobExecutionExportFilesStatus.SCHEDULED)
            .build();
    var commonExportStatistic = new CommonExportStatistic();
    var listener = new ExportedRecordsListener(null, 100, jobExecutionId);
    commonExportStatistic.setExportedRecordsListener(listener);
    var exportRequest = new ExportRequest();
    var exportStatistic = new ExportStrategyStatistic(listener);
    var jobExecution =
        new JobExecution()
            .id(jobExecutionId)
            .status(JobExecution.StatusEnum.IN_PROGRESS)
            .progress(new JobExecutionProgress().failed(0).duplicatedSrs(0).exported(0));
    var exportEntityIdB = UUID.fromString("00000000-0000-0000-0000-000000000003");
    // Mocking behavior
    when(jobExecutionExportFilesEntityRepository.getReferenceById(exportEntityIdA))
        .thenReturn(exportEntityA);
    when(exportStrategyFactory.getExportStrategy(exportRequest))
        .thenReturn(instancesExportStrategy);
    when(instancesExportStrategy.saveOutputToLocalStorage(
            eq(exportEntityA), eq(exportRequest), any()))
        .thenReturn(exportStatistic);
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    // Simulate that Entity A is now COMPLETED but Entity B is still ACTIVE
    var updatedEntityA = exportEntityA.withStatus(JobExecutionExportFilesStatus.COMPLETED);
    var exportEntityB =
        JobExecutionExportFilesEntity.builder()
            .id(exportEntityIdB)
            .jobExecutionId(jobExecutionId)
            .status(JobExecutionExportFilesStatus.ACTIVE)
            .build();
    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId))
        .thenReturn(List.of(updatedEntityA, exportEntityB));
    // When
    exportExecutor.export(exportEntityA, exportRequest, commonExportStatistic);
    // Then
    // The status of Entity A is set to ACTIVE at the beginning of the export method
    assertEquals(JobExecutionExportFilesStatus.ACTIVE, exportEntityA.getStatus());
    // Job status should remain IN_PROGRESS because Entity B is still ACTIVE
    assertEquals(JobExecution.StatusEnum.IN_PROGRESS, jobExecution.getStatus());
    assertNull(jobExecution.getCompletedDate());
    // Verify that finalization steps were NOT called
    verify(s3ExportsUploader, never()).upload(any(), any(), any());
    verify(storageCleanUpService, never()).cleanExportIdEntities(any());
    // Progress update and lastUpdatedDate are still saved at the end of the method
    verify(jobExecutionService).save(jobExecution);
  }

  @Test
  @TestMate(name = "TestMate-cab0e932cc36e3a0c3b8756e75a49c6b")
  void testExportWhenExportAllIsTrueShouldCalculateTotalProgress() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobExecution = new JobExecution();
    jobExecution.setId(jobExecutionId);
    var progress = new JobExecutionProgress();
    progress.setFailed(0);
    progress.setDuplicatedSrs(0);
    jobExecution.setProgress(progress);
    var fileDefinition = new FileDefinition();
    fileDefinition.setJobExecutionId(jobExecutionId);
    fileDefinition.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    fileDefinition.setFileName("test_all.csv");
    var exportRequest = new ExportRequest();
    exportRequest.setAll(true);
    var commonExportStatistic = new CommonExportStatistic();
    var listener = new ExportedRecordsListener(null, 1000, jobExecutionId);
    listener.getExportedCount().set(100);
    commonExportStatistic.setExportedRecordsListener(listener);
    var exportStatistic = new ExportStrategyStatistic(listener);
    exportStatistic.setFailed(10);
    exportStatistic.setDuplicatedSrs(5);
    var exportEntityId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    var exportEntity =
        JobExecutionExportFilesEntity.builder()
            .id(exportEntityId)
            .jobExecutionId(jobExecutionId)
            .fileLocation("mod-data-export/download/file.mrc")
            .status(JobExecutionExportFilesStatus.SCHEDULED)
            .build();
    var completedExportEntity = exportEntity.withStatus(JobExecutionExportFilesStatus.COMPLETED);
    var fileDefinitionEntity =
        FileDefinitionEntity.builder()
            .id(fileDefinition.getId())
            .fileDefinition(fileDefinition)
            .build();
    when(jobExecutionExportFilesEntityRepository.getReferenceById(exportEntityId))
        .thenReturn(exportEntity);
    when(exportStrategyFactory.getExportStrategy(exportRequest))
        .thenReturn(instancesExportStrategy);
    when(instancesExportStrategy.saveOutputToLocalStorage(
            eq(exportEntity), eq(exportRequest), any()))
        .thenReturn(exportStatistic);
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    when(jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecutionId))
        .thenReturn(List.of(completedExportEntity));
    when(fileDefinitionEntityRepository.getFileDefinitionByJobExecutionId(
            jobExecutionId.toString()))
        .thenReturn(List.of(fileDefinitionEntity));
    when(errorLogEntityCqlRepository.countByJobExecutionId(jobExecutionId)).thenReturn(0L);
    when(s3ExportsUploader.upload(jobExecution, List.of(completedExportEntity), "test_all"))
        .thenReturn("s3/path/test_all.mrc");
    // When
    exportExecutor.export(exportEntity, exportRequest, commonExportStatistic);
    // Then
    assertThat(jobExecution.getProgress().getTotal()).isEqualTo(105);
    assertThat(jobExecution.getProgress().getExported()).isEqualTo(100);
    assertThat(jobExecution.getProgress().getFailed()).isEqualTo(10);
    assertThat(jobExecution.getStatus()).isEqualTo(JobExecution.StatusEnum.COMPLETED);
    assertThat(jobExecution.getCompletedDate()).isNotNull();

    verify(jobExecutionService).save(jobExecution);
    verify(storageCleanUpService).cleanExportIdEntities(jobExecutionId);
    verify(s3ExportsUploader).upload(jobExecution, List.of(completedExportEntity), "test_all");
  }
}
