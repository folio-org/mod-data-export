package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.util.Constants.DEFAULT_AUTHORITY_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_INSTANCE_JOB_PROFILE_ID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.QuickExportRequest;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuickExportServiceTest {

  @Mock private FileDefinitionsService fileDefinitionsService;
  @Mock private DataExportService dataExportService;
  @Mock private ExportIdEntityRepository exportIdEntityRepository;
  @Mock private JobExecutionService jobExecutionService;

  @InjectMocks private QuickExportService quickExportService;

  @ParameterizedTest
  @TestMate(name = "TestMate-78ac8ae7db200522b18fe768e363c661")
  @CsvSource({
    "INSTANCE, " + DEFAULT_INSTANCE_JOB_PROFILE_ID + ", instance",
    "AUTHORITY, " + DEFAULT_AUTHORITY_JOB_PROFILE_ID + ", authority"
  })
  void postQuickExportShouldInitiateExportWithDefaultProfilePerRecordType(
      String recordType, String expectedProfileId, String expectedIdType) {
    // Given
    var instanceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var expectedHrId = 123;
    var quickExportRequest = new QuickExportRequest();
    quickExportRequest.setUuids(List.of(instanceId));
    quickExportRequest.setRecordType(QuickExportRequest.RecordTypeEnum.fromValue(recordType));
    quickExportRequest.setJobProfileId(null);
    var jobExecution = new JobExecution().id(jobExecutionId).hrId(expectedHrId);
    doAnswer(
            invocation -> {
              FileDefinition fileDef = invocation.getArgument(0);
              fileDef.setJobExecutionId(jobExecutionId);
              return null;
            })
        .when(fileDefinitionsService)
        .postFileDefinition(any(FileDefinition.class));
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    // When
    var response = quickExportService.postQuickExport(quickExportRequest);
    // Then
    assertThat(response.getJobExecutionId()).isEqualTo(jobExecutionId);
    assertThat(response.getJobExecutionHrId()).isEqualTo(expectedHrId);
    var fileDefCaptor = ArgumentCaptor.forClass(FileDefinition.class);
    verify(fileDefinitionsService).postFileDefinition(fileDefCaptor.capture());
    assertThat(fileDefCaptor.getValue().getFileName()).isEqualTo("quick-export.csv");
    var exportRequestCaptor = ArgumentCaptor.forClass(ExportRequest.class);
    verify(dataExportService).postDataExport(exportRequestCaptor.capture());
    var capturedRequest = exportRequestCaptor.getValue();
    assertThat(capturedRequest.getJobProfileId()).isEqualTo(UUID.fromString(expectedProfileId));
    assertThat(capturedRequest.getQuick()).isTrue();
    assertThat(capturedRequest.getIdType().getValue()).isEqualTo(expectedIdType);
    var batchCaptor = ArgumentCaptor.forClass(List.class);
    verify(exportIdEntityRepository).saveAll(batchCaptor.capture());
    List<ExportIdEntity> savedBatch = batchCaptor.getValue();
    assertThat(savedBatch).hasSize(1);
    assertThat(savedBatch.get(0).getJobExecutionId()).isEqualTo(jobExecutionId);
    assertThat(savedBatch.get(0).getInstanceId()).isEqualTo(instanceId);
  }

  @Test
  @TestMate(name = "TestMate-800b26a738d47cc81055908f4917831b")
  void postQuickExportShouldUseProvidedJobProfileId() {
    // Given
    var customJobProfileId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var instanceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var expectedHrId = 123;
    var quickExportRequest = new QuickExportRequest();
    quickExportRequest.setUuids(List.of(instanceId));
    quickExportRequest.setRecordType(QuickExportRequest.RecordTypeEnum.INSTANCE);
    quickExportRequest.setJobProfileId(customJobProfileId);
    var jobExecution = new JobExecution().id(jobExecutionId).hrId(expectedHrId);
    doAnswer(
            invocation -> {
              FileDefinition fileDef = invocation.getArgument(0);
              fileDef.setJobExecutionId(jobExecutionId);
              return null;
            })
        .when(fileDefinitionsService)
        .postFileDefinition(any(FileDefinition.class));
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    // When
    var response = quickExportService.postQuickExport(quickExportRequest);
    // Then
    assertThat(response.getJobExecutionId()).isEqualTo(jobExecutionId);
    assertThat(response.getJobExecutionHrId()).isEqualTo(expectedHrId);
    var exportRequestCaptor = ArgumentCaptor.forClass(ExportRequest.class);
    verify(dataExportService).postDataExport(exportRequestCaptor.capture());
    var capturedRequest = exportRequestCaptor.getValue();
    assertThat(capturedRequest.getJobProfileId()).isEqualTo(customJobProfileId);
    assertThat(capturedRequest.getQuick()).isTrue();
    assertThat(capturedRequest.getRecordType()).isEqualTo(ExportRequest.RecordTypeEnum.INSTANCE);
    assertThat(capturedRequest.getIdType()).isEqualTo(ExportRequest.IdTypeEnum.INSTANCE);
    var batchCaptor = ArgumentCaptor.forClass(List.class);
    verify(exportIdEntityRepository).saveAll(batchCaptor.capture());
    List<ExportIdEntity> savedBatch = batchCaptor.getValue();
    assertThat(savedBatch).hasSize(1);
    assertThat(savedBatch.get(0).getJobExecutionId()).isEqualTo(jobExecutionId);
    assertThat(savedBatch.get(0).getInstanceId()).isEqualTo(instanceId);
  }

  @Test
  @TestMate(name = "TestMate-61b8877b57a5907915f9c59d8568b81d")
  void postQuickExportShouldSkipSavingEntitiesWhenUuidsAreNull() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var customJobProfileId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    var expectedHrId = 123;
    var quickExportRequest = new QuickExportRequest();
    quickExportRequest.setUuids(null);
    quickExportRequest.setRecordType(QuickExportRequest.RecordTypeEnum.INSTANCE);
    quickExportRequest.setJobProfileId(customJobProfileId);
    var jobExecution = new JobExecution().id(jobExecutionId).hrId(expectedHrId);
    doAnswer(
            invocation -> {
              FileDefinition fileDef = invocation.getArgument(0);
              fileDef.setJobExecutionId(jobExecutionId);
              return null;
            })
        .when(fileDefinitionsService)
        .postFileDefinition(any(FileDefinition.class));
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    // When
    var response = quickExportService.postQuickExport(quickExportRequest);
    // Then
    assertThat(response.getJobExecutionId()).isEqualTo(jobExecutionId);
    assertThat(response.getJobExecutionHrId()).isEqualTo(expectedHrId);
    verify(exportIdEntityRepository, never()).saveAll(any(List.class));
    verify(fileDefinitionsService).postFileDefinition(any(FileDefinition.class));
    verify(dataExportService).postDataExport(any(ExportRequest.class));
  }

  @Test
  @TestMate(name = "TestMate-2468e25cfc111d4c6355338da68ef115")
  void postQuickExportShouldThrowExceptionForUnsupportedRecordType() {
    // Given
    var instanceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var quickExportRequest = new QuickExportRequest();
    quickExportRequest.setUuids(List.of(instanceId));
    quickExportRequest.setRecordType(QuickExportRequest.RecordTypeEnum.ITEM);
    quickExportRequest.setJobProfileId(null);
    // When
    var exception =
        assertThrows(
            DataExportRequestValidationException.class,
            () -> quickExportService.postQuickExport(quickExportRequest));
    // Then
    assertThat(exception.getMessage())
        .contains("No default job profile found by the following recordType: ITEM");
    var fileDefCaptor = ArgumentCaptor.forClass(FileDefinition.class);
    verify(fileDefinitionsService).postFileDefinition(fileDefCaptor.capture());
    assertThat(fileDefCaptor.getValue().getFileName()).isEqualTo("quick-export.csv");
    verifyNoInteractions(dataExportService);
    verifyNoInteractions(jobExecutionService);
  }
}
