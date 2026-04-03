package org.folio.dataexp.service;

import static org.folio.dataexp.util.Constants.DEFAULT_AUTHORITY_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_HOLDINGS_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_INSTANCE_JOB_PROFILE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.folio.dataexp.domain.dto.ExportAllRequest;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.MarcAuthorityRecordAllRepository;
import static org.mockito.Mockito.lenient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.User;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.service.validators.DataExportRequestValidator;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.dataexp.service.DataExportService;
import lombok.SneakyThrows;
import org.folio.dataexp.service.CommonExportStatistic;
import org.folio.dataexp.service.FileDefinitionService;
import org.folio.dataexp.service.JobExecutionService;
import org.folio.dataexp.service.InputFileProcessor;
import org.folio.dataexp.service.SlicerProcessor;
import org.folio.dataexp.service.SingleFileProcessorAsync;
import jakarta.persistence.EntityNotFoundException;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class DefaultJobProfileIdTest {

  @Mock private FileDefinitionsService mockFileDefinitionService;
  @Mock private DataExportService mockDataExportService;
  @InjectMocks private DataExportAllService mockDataExportAllService;

    @Mock private InputFileProcessor inputFileProcessor;

    @Mock private JobProfileEntityRepository jobProfileEntityRepository;

    @Mock private UserClient userClient;

    @Mock private FileDefinitionEntityRepository fileDefinitionEntityRepository;

    @InjectMocks private DataExportService dataExportService;

    @Mock private SingleFileProcessorAsync singleFileProcessorAsync;

    @Mock private MappingProfileEntityRepository mappingProfileEntityRepository;

    @Mock private JobExecutionService jobExecutionService;

    @Mock private DataExportRequestValidator dataExportRequestValidator;

    @Mock private ExecutorService executor;

    @Mock private SlicerProcessor slicerProcessor;

    @Mock private FolioExecutionContext folioExecutionContext;

    @Mock private InstanceEntityRepository instanceEntityRepository;

    @Mock private HoldingsRecordEntityRepository holdingsRecordEntityRepository;

    @Mock private MarcAuthorityRecordAllRepository marcAuthorityRecordAllRepository;

    @Mock private ExportIdEntityRepository exportIdEntityRepository;

  @ParameterizedTest
  @EnumSource(value = ExportAllRequest.IdTypeEnum.class)
  void getDefaultJobProfileIdTest(ExportAllRequest.IdTypeEnum idType) {
    var exportAllRequest = new ExportAllRequest().idType(idType);
    mockDataExportAllService.postDataExportAll(exportAllRequest);
    var exportRequestArgumentCaptor = ArgumentCaptor.forClass(ExportRequest.class);
    verify(mockDataExportService).postDataExport(exportRequestArgumentCaptor.capture());
    String expected = null;
    if (idType == ExportAllRequest.IdTypeEnum.HOLDING) {
      expected = DEFAULT_HOLDINGS_JOB_PROFILE_ID;
    } else if (idType == ExportAllRequest.IdTypeEnum.AUTHORITY) {
      expected = DEFAULT_AUTHORITY_JOB_PROFILE_ID;
    } else if (idType == ExportAllRequest.IdTypeEnum.INSTANCE) {
      expected = DEFAULT_INSTANCE_JOB_PROFILE_ID;
    }
    assertEquals(
        UUID.fromString(expected), exportRequestArgumentCaptor.getValue().getJobProfileId());
  }

    @Test
  void postDataExportShouldInitializeJobExecutionAndStartStandardAsyncExport() throws Exception {
    // TestMate-1531217b3461537891fc7f5ad318b3cf
    // Given
    var fileDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobProfileId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var mappingProfileId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000004");
    var userId = UUID.fromString("00000000-0000-0000-0000-000000000005");
    var expectedHrId = 100;
    // Inject the mock executor because it's initialized in the class under test
    Field executorField = DataExportService.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(dataExportService, executor);
    var exportRequest =
        new ExportRequest()
            .fileDefinitionId(fileDefinitionId)
            .jobProfileId(jobProfileId)
            .all(false)
            .quick(false)
            .idType(ExportRequest.IdTypeEnum.INSTANCE);
    var fileDefinition =
        new FileDefinition()
            .id(fileDefinitionId)
            .jobExecutionId(jobExecutionId)
            .fileName("test_upload.csv");
    var fileDefinitionEntity =
        FileDefinitionEntity.builder().id(fileDefinitionId).fileDefinition(fileDefinition).build();
    var jobProfile =
        new JobProfile()
            .id(jobProfileId)
            .name("Test Job Profile")
            .mappingProfileId(mappingProfileId);
    var jobProfileEntity =
        JobProfileEntity.builder()
            .id(jobProfileId)
            .jobProfile(jobProfile)
            .mappingProfileId(mappingProfileId)
            .build();
    var mappingProfileEntity =
        MappingProfileEntity.builder().id(mappingProfileId).format("MARC").build();
    var jobExecution =
        new JobExecution()
            .id(jobExecutionId)
            .progress(new JobExecutionProgress().total(0).exported(0).failed(0));
    var user = new User();
    user.setId(userId.toString());
    var personal = new User.Personal();
    personal.setFirstName("John");
    personal.setLastName("Doe");
    user.setPersonal(personal);
    when(fileDefinitionEntityRepository.getReferenceById(fileDefinitionId))
        .thenReturn(fileDefinitionEntity);
    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    when(jobExecutionService.getNextHrid()).thenReturn(expectedHrId);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    doAnswer(
            invocation -> {
              Runnable runnable = invocation.getArgument(0);
              runnable.run();
              return null;
            })
        .when(executor)
        .execute(any(Runnable.class));
    try (var mockedStatic = mockStatic(FolioExecutionScopeExecutionContextManager.class)) {
      mockedStatic
          .when(
              () ->
                  FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext(
                      any(Runnable.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      // When
      dataExportService.postDataExport(exportRequest);
    }
    // Then
    assertThat(jobExecution.getJobProfileId()).isEqualTo(jobProfileId);
    assertThat(jobExecution.getJobProfileName()).isEqualTo("Test Job Profile");
    assertThat(jobExecution.getHrId()).isEqualTo(expectedHrId);
    assertThat(jobExecution.getRunBy().getFirstName()).isEqualTo("John");
    assertThat(jobExecution.getRunBy().getLastName()).isEqualTo("Doe");
    assertThat(jobExecution.getRunBy().getUserId()).isEqualTo(userId.toString());
    assertThat(jobExecution.getStatus()).isEqualTo(JobExecution.StatusEnum.IN_PROGRESS);
    assertThat(jobExecution.getStartedDate()).isNotNull();
    assertThat(jobExecution.getExportedFiles()).hasSize(1);
    verify(dataExportRequestValidator)
        .validate(eq(exportRequest), any(FileDefinition.class), eq(mappingProfileId.toString()));
    verify(inputFileProcessor)
        .readFile(
            any(FileDefinition.class),
            isA(CommonExportStatistic.class),
            eq(ExportRequest.IdTypeEnum.INSTANCE));
    verify(slicerProcessor)
        .sliceInstancesIds(any(FileDefinition.class), eq(exportRequest), eq("MARC"));
    verify(singleFileProcessorAsync)
        .exportBySingleFile(eq(jobExecutionId), eq(exportRequest), isA(CommonExportStatistic.class));
  }

    @Test
  @SneakyThrows
  void postDataExportWhenValidationFailsShouldSetStatusToFailAndStop() {
    // TestMate-2141af7a00ed6aac6c57557636d499e2
    // Given
    var fileDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobProfileId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var mappingProfileId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000004");
    var userId = UUID.fromString("00000000-0000-0000-0000-000000000005");
    var expectedHrId = 101;
    Field executorField = DataExportService.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(dataExportService, executor);
    var exportRequest = new ExportRequest()
        .fileDefinitionId(fileDefinitionId)
        .jobProfileId(jobProfileId)
        .all(false)
        .quick(false)
        .idType(ExportRequest.IdTypeEnum.INSTANCE);
    var fileDefinition = new FileDefinition()
        .id(fileDefinitionId)
        .jobExecutionId(jobExecutionId)
        .fileName("test_validation_fail.csv");
    var fileDefinitionEntity = FileDefinitionEntity.builder()
        .id(fileDefinitionId)
        .fileDefinition(fileDefinition)
        .build();
    var jobProfile = new JobProfile()
        .id(jobProfileId)
        .name("Validation Fail Profile")
        .mappingProfileId(mappingProfileId);
    var jobProfileEntity = JobProfileEntity.builder()
        .id(jobProfileId)
        .jobProfile(jobProfile)
        .mappingProfileId(mappingProfileId)
        .build();
    var mappingProfileEntity = MappingProfileEntity.builder()
        .id(mappingProfileId)
        .format("MARC")
        .build();
    var jobExecution = new JobExecution()
        .id(jobExecutionId)
        .progress(new JobExecutionProgress().total(0).exported(0).failed(0));
    var user = new User();
    user.setId(userId.toString());
    var personal = new User.Personal();
    personal.setFirstName("Jane");
    personal.setLastName("Doe");
    user.setPersonal(personal);
    when(fileDefinitionEntityRepository.getReferenceById(fileDefinitionId)).thenReturn(fileDefinitionEntity);
    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId)).thenReturn(mappingProfileEntity);
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    when(jobExecutionService.getNextHrid()).thenReturn(expectedHrId);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    doThrow(new DataExportRequestValidationException("Validation failed"))
        .when(dataExportRequestValidator).validate(eq(exportRequest), any(FileDefinition.class), eq(mappingProfileId.toString()));
    // When
    dataExportService.postDataExport(exportRequest);
    // Then
    assertThat(jobExecution.getStatus()).isEqualTo(JobExecution.StatusEnum.FAIL);
    assertThat(jobExecution.getJobProfileId()).isEqualTo(jobProfileId);
    assertThat(jobExecution.getJobProfileName()).isEqualTo("Validation Fail Profile");
    assertThat(jobExecution.getCompletedDate()).isNotNull();
    assertThat(jobExecution.getRunBy().getFirstName()).isEqualTo("Jane");
    assertThat(jobExecution.getRunBy().getLastName()).isEqualTo("Doe");
    assertThat(jobExecution.getExportedFiles()).hasSize(1);
    var jobExecutionCaptor = ArgumentCaptor.forClass(JobExecution.class);
    verify(jobExecutionService).save(jobExecutionCaptor.capture());
    assertThat(jobExecutionCaptor.getValue().getStatus()).isEqualTo(JobExecution.StatusEnum.FAIL);
    verify(executor, never()).execute(any(Runnable.class));
    verify(inputFileProcessor, never()).readFile(any(), any(), any());
    verify(slicerProcessor, never()).sliceInstancesIds(any(), any(), any());
  }

    @ParameterizedTest
  @EnumSource(value = ExportRequest.IdTypeEnum.class)
  @SneakyThrows
  void postDataExportWhenAllIsTrueShouldSkipInputFileReadAndCalculateTotal(ExportRequest.IdTypeEnum idType) {
    // TestMate-22d8a316cfde3ac9e25cfb74d19266cb
    // Given
    var fileDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobProfileId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var mappingProfileId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000004");
    var userId = UUID.fromString("00000000-0000-0000-0000-000000000005");
    var expectedHrId = 200;
    var expectedTotalCount = 500L;
    Field executorField = DataExportService.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(dataExportService, executor);
    var exportRequest = new ExportRequest()
        .fileDefinitionId(fileDefinitionId)
        .jobProfileId(jobProfileId)
        .all(true)
        .quick(false)
        .idType(idType);
    var fileDefinition = new FileDefinition()
        .id(fileDefinitionId)
        .jobExecutionId(jobExecutionId)
        .fileName("all_records_export.csv");
    var fileDefinitionEntity = FileDefinitionEntity.builder()
        .id(fileDefinitionId)
        .fileDefinition(fileDefinition)
        .build();
    var jobProfile = new JobProfile()
        .id(jobProfileId)
        .name("Export All Profile")
        .mappingProfileId(mappingProfileId);
    var jobProfileEntity = JobProfileEntity.builder()
        .id(jobProfileId)
        .jobProfile(jobProfile)
        .mappingProfileId(mappingProfileId)
        .build();
    var mappingProfileEntity = MappingProfileEntity.builder()
        .id(mappingProfileId)
        .format("MARC")
        .build();
    var jobExecution = new JobExecution()
        .id(jobExecutionId)
        .progress(new JobExecutionProgress().total(0).exported(0).failed(0));
    var user = new User();
    user.setId(userId.toString());
    var personal = new User.Personal();
    personal.setFirstName("Test");
    personal.setLastName("User");
    user.setPersonal(personal);
    when(fileDefinitionEntityRepository.getReferenceById(fileDefinitionId)).thenReturn(fileDefinitionEntity);
    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId)).thenReturn(mappingProfileEntity);
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    when(jobExecutionService.getNextHrid()).thenReturn(expectedHrId);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    if (idType == ExportRequest.IdTypeEnum.INSTANCE) {
      when(instanceEntityRepository.count()).thenReturn(expectedTotalCount);
    } else if (idType == ExportRequest.IdTypeEnum.HOLDING) {
      when(holdingsRecordEntityRepository.count()).thenReturn(expectedTotalCount);
    } else if (idType == ExportRequest.IdTypeEnum.AUTHORITY) {
      when(marcAuthorityRecordAllRepository.count()).thenReturn(expectedTotalCount);
    }
    doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(0);
      runnable.run();
      return null;
    }).when(executor).execute(any(Runnable.class));
    try (var mockedStatic = mockStatic(FolioExecutionScopeExecutionContextManager.class)) {
      mockedStatic.when(() -> FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext(any(Runnable.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      // When
      dataExportService.postDataExport(exportRequest);
    }
    // Then
    assertThat(jobExecution.getStatus()).isEqualTo(JobExecution.StatusEnum.IN_PROGRESS);
    assertThat(jobExecution.getProgress().getTotal()).isEqualTo((int) expectedTotalCount);
    assertThat(jobExecution.getRunBy().getFirstName()).isEqualTo("Test");
    assertThat(jobExecution.getRunBy().getLastName()).isEqualTo("User");
    assertThat(jobExecution.getExportedFiles()).hasSize(1);
    verify(dataExportRequestValidator).validate(eq(exportRequest), any(FileDefinition.class), eq(mappingProfileId.toString()));
    verify(inputFileProcessor, never()).readFile(any(), any(), any());
    verify(slicerProcessor).sliceInstancesIds(any(FileDefinition.class), eq(exportRequest), eq("MARC"));
    verify(singleFileProcessorAsync).exportBySingleFile(eq(jobExecutionId), eq(exportRequest), isA(CommonExportStatistic.class));
    var jobExecutionCaptor = ArgumentCaptor.forClass(JobExecution.class);
    // updateJobExecutionForPostDataExport is called twice: once before executor.execute and once inside it
    verify(jobExecutionService, org.mockito.Mockito.times(2)).save(jobExecutionCaptor.capture());
    assertThat(jobExecutionCaptor.getValue().getStatus()).isEqualTo(JobExecution.StatusEnum.IN_PROGRESS);
    assertThat(jobExecutionCaptor.getValue().getProgress().getTotal()).isEqualTo((int) expectedTotalCount);
  }

    @Test
  @SneakyThrows
  void postDataExportWhenQuickIsTrueShouldSkipInputFileReadAndUseExportIdCount() {
    // TestMate-59230d5876824e14a86f3b0a230b792b
    // Given
    var fileDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobProfileId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var mappingProfileId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000004");
    var userId = UUID.fromString("00000000-0000-0000-0000-000000000005");
    var expectedHrId = 123;
    var expectedTotalCount = 50L;
    Field executorField = DataExportService.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(dataExportService, executor);
    var exportRequest = new ExportRequest()
        .fileDefinitionId(fileDefinitionId)
        .jobProfileId(jobProfileId)
        .all(false)
        .quick(true)
        .idType(ExportRequest.IdTypeEnum.INSTANCE);
    var fileDefinition = new FileDefinition()
        .id(fileDefinitionId)
        .jobExecutionId(jobExecutionId)
        .fileName("quick_export_test.csv");
    var fileDefinitionEntity = FileDefinitionEntity.builder()
        .id(fileDefinitionId)
        .fileDefinition(fileDefinition)
        .build();
    var jobProfile = new JobProfile()
        .id(jobProfileId)
        .name("Quick Export Profile")
        .mappingProfileId(mappingProfileId);
    var jobProfileEntity = JobProfileEntity.builder()
        .id(jobProfileId)
        .jobProfile(jobProfile)
        .mappingProfileId(mappingProfileId)
        .build();
    var mappingProfileEntity = MappingProfileEntity.builder()
        .id(mappingProfileId)
        .format("MARC")
        .build();
    var jobExecution = new JobExecution()
        .id(jobExecutionId)
        .progress(new JobExecutionProgress().total(0).exported(0).failed(0));
    var user = new User();
    user.setId(userId.toString());
    var personal = new User.Personal();
    personal.setFirstName("First");
    personal.setLastName("Last");
    user.setPersonal(personal);
    when(fileDefinitionEntityRepository.getReferenceById(fileDefinitionId)).thenReturn(fileDefinitionEntity);
    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId)).thenReturn(mappingProfileEntity);
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    when(jobExecutionService.getNextHrid()).thenReturn(expectedHrId);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(exportIdEntityRepository.countByJobExecutionId(jobExecutionId)).thenReturn(expectedTotalCount);
    doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(0);
      runnable.run();
      return null;
    }).when(executor).execute(any(Runnable.class));
    try (var mockedStatic = mockStatic(FolioExecutionScopeExecutionContextManager.class)) {
      mockedStatic.when(() -> FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext(any(Runnable.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      // When
      dataExportService.postDataExport(exportRequest);
    }
    // Then
    assertThat(jobExecution.getStatus()).isEqualTo(JobExecution.StatusEnum.IN_PROGRESS);
    assertThat(jobExecution.getProgress().getTotal()).isEqualTo((int) expectedTotalCount);
    assertThat(jobExecution.getRunBy().getFirstName()).isEqualTo("First");
    assertThat(jobExecution.getRunBy().getLastName()).isEqualTo("Last");
    assertThat(jobExecution.getRunBy().getUserId()).isEqualTo(userId.toString());
    assertThat(jobExecution.getExportedFiles()).hasSize(1);
    verify(dataExportRequestValidator).validate(eq(exportRequest), any(FileDefinition.class), eq(mappingProfileId.toString()));
    verify(inputFileProcessor, never()).readFile(any(), any(), any());
    verify(slicerProcessor).sliceInstancesIds(any(FileDefinition.class), eq(exportRequest), eq("MARC"));
    verify(singleFileProcessorAsync).exportBySingleFile(eq(jobExecutionId), eq(exportRequest), isA(CommonExportStatistic.class));
    var jobExecutionCaptor = ArgumentCaptor.forClass(JobExecution.class);
    verify(jobExecutionService, org.mockito.Mockito.atLeastOnce()).save(jobExecutionCaptor.capture());
    assertThat(jobExecutionCaptor.getValue().getProgress().getTotal()).isEqualTo((int) expectedTotalCount);
  }

    @Test
  void postDataExportWhenFileDefinitionNotFoundShouldThrowEntityNotFoundException() {
    // TestMate-7b64f97feddd5b7abdf77201220c655a
    // Given
    var fileDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobProfileId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var exportRequest = new ExportRequest()
        .fileDefinitionId(fileDefinitionId)
        .jobProfileId(jobProfileId);
    var fileDefinitionEntity = org.mockito.Mockito.mock(FileDefinitionEntity.class);
    when(fileDefinitionEntityRepository.getReferenceById(fileDefinitionId)).thenReturn(fileDefinitionEntity);
    when(fileDefinitionEntity.getFileDefinition()).thenThrow(new EntityNotFoundException("Entity not found in DB"));
    // When
    var exception = assertThrows(EntityNotFoundException.class, () -> dataExportService.postDataExport(exportRequest));
    // Then
    assertEquals("Unable to find", exception.getMessage());
    verify(jobProfileEntityRepository, never()).getReferenceById(any(UUID.class));
    verify(jobExecutionService, never()).getById(any(UUID.class));
  }
}
