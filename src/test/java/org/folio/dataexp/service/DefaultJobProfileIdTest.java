package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.util.Constants.DEFAULT_AUTHORITY_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_HOLDINGS_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_INSTANCE_JOB_PROFILE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import lombok.SneakyThrows;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.ExportAllRequest;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.User;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.repository.MarcAuthorityRecordAllRepository;
import org.folio.dataexp.service.validators.DataExportRequestValidator;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultJobProfileIdTest {

  private static final UUID FILE_DEFINITION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID JOB_PROFILE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID MAPPING_PROFILE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000003");
  private static final UUID JOB_EXECUTION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000004");
  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

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

    var requestCaptor = ArgumentCaptor.forClass(ExportRequest.class);
    verify(mockDataExportService).postDataExport(requestCaptor.capture());

    assertEquals(
        UUID.fromString(defaultJobProfileIdFor(idType)),
        requestCaptor.getValue().getJobProfileId());
  }

  @Test
  @TestMate(name = "TestMate-1531217b3461537891fc7f5ad318b3cf")
  @SneakyThrows
  void postDataExportShouldInitializeJobExecutionAndStartStandardAsyncExport() {
    setExecutorOnService();
    var scenario = buildScenario("test_upload.csv", "Test Job Profile", "John", "Doe");
    var exportRequest = exportRequest(false, false, ExportRequest.IdTypeEnum.INSTANCE);
    var expectedHrId = 100;

    stubCommonLookups(scenario, expectedHrId);
    runExecutorInline();

    withMockedFolioContext(() -> dataExportService.postDataExport(exportRequest));

    assertThat(scenario.jobExecution.getJobProfileId()).isEqualTo(JOB_PROFILE_ID);
    assertThat(scenario.jobExecution.getJobProfileName()).isEqualTo("Test Job Profile");
    assertThat(scenario.jobExecution.getHrId()).isEqualTo(expectedHrId);
    assertThat(scenario.jobExecution.getRunBy().getFirstName()).isEqualTo("John");
    assertThat(scenario.jobExecution.getRunBy().getLastName()).isEqualTo("Doe");
    assertThat(scenario.jobExecution.getRunBy().getUserId()).isEqualTo(USER_ID.toString());
    assertThat(scenario.jobExecution.getStatus()).isEqualTo(JobExecution.StatusEnum.IN_PROGRESS);
    assertThat(scenario.jobExecution.getStartedDate()).isNotNull();
    assertThat(scenario.jobExecution.getExportedFiles()).hasSize(1);

    verify(dataExportRequestValidator)
        .validate(eq(exportRequest), any(FileDefinition.class), eq(MAPPING_PROFILE_ID.toString()));
    verify(inputFileProcessor)
        .readFile(
            any(FileDefinition.class),
            isA(CommonExportStatistic.class),
            eq(ExportRequest.IdTypeEnum.INSTANCE));
    verify(slicerProcessor)
        .sliceInstancesIds(any(FileDefinition.class), eq(exportRequest), eq("MARC"));
    verify(singleFileProcessorAsync)
        .exportBySingleFile(
            eq(JOB_EXECUTION_ID), eq(exportRequest), isA(CommonExportStatistic.class));
  }

  @Test
  @TestMate(name = "TestMate-2141af7a00ed6aac6c57557636d499e2")
  @SneakyThrows
  void postDataExportWhenValidationFailsShouldSetStatusToFailAndStop() {
    setExecutorOnService();
    var scenario =
        buildScenario("test_validation_fail.csv", "Validation Fail Profile", "Jane", "Doe");
    var exportRequest = exportRequest(false, false, ExportRequest.IdTypeEnum.INSTANCE);
    var expectedHrId = 101;

    stubCommonLookups(scenario, expectedHrId);
    doThrow(new DataExportRequestValidationException("Validation failed"))
        .when(dataExportRequestValidator)
        .validate(eq(exportRequest), any(FileDefinition.class), eq(MAPPING_PROFILE_ID.toString()));

    dataExportService.postDataExport(exportRequest);

    assertThat(scenario.jobExecution.getStatus()).isEqualTo(JobExecution.StatusEnum.FAIL);
    assertThat(scenario.jobExecution.getJobProfileId()).isEqualTo(JOB_PROFILE_ID);
    assertThat(scenario.jobExecution.getJobProfileName()).isEqualTo("Validation Fail Profile");
    assertThat(scenario.jobExecution.getCompletedDate()).isNotNull();
    assertThat(scenario.jobExecution.getRunBy().getFirstName()).isEqualTo("Jane");
    assertThat(scenario.jobExecution.getRunBy().getLastName()).isEqualTo("Doe");
    assertThat(scenario.jobExecution.getExportedFiles()).hasSize(1);

    var saveCaptor = ArgumentCaptor.forClass(JobExecution.class);
    verify(jobExecutionService).save(saveCaptor.capture());
    assertThat(saveCaptor.getValue().getStatus()).isEqualTo(JobExecution.StatusEnum.FAIL);

    verify(executor, never()).execute(any(Runnable.class));
    verify(inputFileProcessor, never()).readFile(any(), any(), any());
    verify(slicerProcessor, never()).sliceInstancesIds(any(), any(), any());
  }

  @ParameterizedTest
  @TestMate(name = "TestMate-22d8a316cfde3ac9e25cfb74d19266cb")
  @EnumSource(value = ExportRequest.IdTypeEnum.class)
  @SneakyThrows
  void postDataExportWhenAllIsTrueShouldSkipInputFileReadAndCalculateTotal(
      ExportRequest.IdTypeEnum idType) {
    setExecutorOnService();
    var scenario = buildScenario("all_records_export.csv", "Export All Profile", "Test", "User");
    var expectedHrId = 200;
    var expectedTotalCount = 500L;

    stubCommonLookups(scenario, expectedHrId);
    stubAllModeCount(idType, expectedTotalCount);
    runExecutorInline();

    var exportRequest = exportRequest(true, false, idType);
    withMockedFolioContext(() -> dataExportService.postDataExport(exportRequest));

    assertThat(scenario.jobExecution.getStatus()).isEqualTo(JobExecution.StatusEnum.IN_PROGRESS);
    assertThat(scenario.jobExecution.getProgress().getTotal()).isEqualTo((int) expectedTotalCount);
    assertThat(scenario.jobExecution.getRunBy().getFirstName()).isEqualTo("Test");
    assertThat(scenario.jobExecution.getRunBy().getLastName()).isEqualTo("User");
    assertThat(scenario.jobExecution.getExportedFiles()).hasSize(1);

    verify(dataExportRequestValidator)
        .validate(eq(exportRequest), any(FileDefinition.class), eq(MAPPING_PROFILE_ID.toString()));
    verify(inputFileProcessor, never()).readFile(any(), any(), any());
    verify(slicerProcessor)
        .sliceInstancesIds(any(FileDefinition.class), eq(exportRequest), eq("MARC"));
    verify(singleFileProcessorAsync)
        .exportBySingleFile(
            eq(JOB_EXECUTION_ID), eq(exportRequest), isA(CommonExportStatistic.class));

    var saveCaptor = ArgumentCaptor.forClass(JobExecution.class);
    verify(jobExecutionService, times(2)).save(saveCaptor.capture());
    assertThat(saveCaptor.getValue().getStatus()).isEqualTo(JobExecution.StatusEnum.IN_PROGRESS);
    assertThat(saveCaptor.getValue().getProgress().getTotal()).isEqualTo((int) expectedTotalCount);
  }

  @Test
  @TestMate(name = "TestMate-59230d5876824e14a86f3b0a230b792b")
  @SneakyThrows
  void postDataExportWhenQuickIsTrueShouldSkipInputFileReadAndUseExportIdCount() {
    setExecutorOnService();
    var scenario = buildScenario("quick_export_test.csv", "Quick Export Profile", "First", "Last");
    var expectedHrId = 123;
    var expectedTotalCount = 50L;

    stubCommonLookups(scenario, expectedHrId);
    when(exportIdEntityRepository.countByJobExecutionId(JOB_EXECUTION_ID))
        .thenReturn(expectedTotalCount);
    runExecutorInline();

    var exportRequest = exportRequest(false, true, ExportRequest.IdTypeEnum.INSTANCE);

    withMockedFolioContext(() -> dataExportService.postDataExport(exportRequest));

    assertThat(scenario.jobExecution.getStatus()).isEqualTo(JobExecution.StatusEnum.IN_PROGRESS);
    assertThat(scenario.jobExecution.getProgress().getTotal()).isEqualTo((int) expectedTotalCount);
    assertThat(scenario.jobExecution.getRunBy().getFirstName()).isEqualTo("First");
    assertThat(scenario.jobExecution.getRunBy().getLastName()).isEqualTo("Last");
    assertThat(scenario.jobExecution.getRunBy().getUserId()).isEqualTo(USER_ID.toString());
    assertThat(scenario.jobExecution.getExportedFiles()).hasSize(1);

    verify(dataExportRequestValidator)
        .validate(eq(exportRequest), any(FileDefinition.class), eq(MAPPING_PROFILE_ID.toString()));
    verify(inputFileProcessor, never()).readFile(any(), any(), any());
    verify(slicerProcessor)
        .sliceInstancesIds(any(FileDefinition.class), eq(exportRequest), eq("MARC"));
    verify(singleFileProcessorAsync)
        .exportBySingleFile(
            eq(JOB_EXECUTION_ID), eq(exportRequest), isA(CommonExportStatistic.class));

    var saveCaptor = ArgumentCaptor.forClass(JobExecution.class);
    verify(jobExecutionService, atLeastOnce()).save(saveCaptor.capture());
    assertThat(saveCaptor.getValue().getProgress().getTotal()).isEqualTo((int) expectedTotalCount);
  }

  @Test
  @TestMate(name = "TestMate-7b64f97feddd5b7abdf77201220c655a")
  void postDataExportWhenFileDefinitionNotFoundShouldThrowEntityNotFoundException() {
    var exportRequest =
        new ExportRequest().fileDefinitionId(FILE_DEFINITION_ID).jobProfileId(JOB_PROFILE_ID);
    var fileDefinitionEntity = org.mockito.Mockito.mock(FileDefinitionEntity.class);

    when(fileDefinitionEntityRepository.getReferenceById(FILE_DEFINITION_ID))
        .thenReturn(fileDefinitionEntity);
    when(fileDefinitionEntity.getFileDefinition())
        .thenThrow(new EntityNotFoundException("Entity not found in DB"));

    var exception =
        assertThrows(
            EntityNotFoundException.class, () -> dataExportService.postDataExport(exportRequest));

    assertEquals("Unable to find", exception.getMessage());
    verify(jobProfileEntityRepository, never()).getReferenceById(any(UUID.class));
    verify(jobExecutionService, never()).getById(any(UUID.class));
  }

  private String defaultJobProfileIdFor(ExportAllRequest.IdTypeEnum idType) {
    return switch (idType) {
      case HOLDING -> DEFAULT_HOLDINGS_JOB_PROFILE_ID;
      case AUTHORITY -> DEFAULT_AUTHORITY_JOB_PROFILE_ID;
      case INSTANCE -> DEFAULT_INSTANCE_JOB_PROFILE_ID;
    };
  }

  private ExportRequest exportRequest(boolean all, boolean quick, ExportRequest.IdTypeEnum idType) {
    return new ExportRequest()
        .fileDefinitionId(FILE_DEFINITION_ID)
        .jobProfileId(JOB_PROFILE_ID)
        .all(all)
        .quick(quick)
        .idType(idType);
  }

  private Scenario buildScenario(
      String fileName, String profileName, String firstName, String lastName) {

    var user = new User();
    user.setId(USER_ID.toString());
    var personal = new User.Personal();
    personal.setFirstName(firstName);
    personal.setLastName(lastName);
    user.setPersonal(personal);

    var jobExecution =
        new JobExecution()
            .id(JOB_EXECUTION_ID)
            .progress(new JobExecutionProgress().total(0).exported(0).failed(0));

    var mappingProfileEntity =
        MappingProfileEntity.builder().id(MAPPING_PROFILE_ID).format("MARC").build();

    var jobProfile =
        new JobProfile().id(JOB_PROFILE_ID).name(profileName).mappingProfileId(MAPPING_PROFILE_ID);

    var jobProfileEntity =
        JobProfileEntity.builder()
            .id(JOB_PROFILE_ID)
            .jobProfile(jobProfile)
            .mappingProfileId(MAPPING_PROFILE_ID)
            .build();

    var fileDefinition =
        new FileDefinition()
            .id(FILE_DEFINITION_ID)
            .jobExecutionId(JOB_EXECUTION_ID)
            .fileName(fileName);

    var fileDefinitionEntity =
        FileDefinitionEntity.builder()
            .id(FILE_DEFINITION_ID)
            .fileDefinition(fileDefinition)
            .build();

    return new Scenario(
        fileDefinitionEntity, jobProfileEntity, mappingProfileEntity, jobExecution, user);
  }

  private void stubCommonLookups(Scenario scenario, int expectedHrId) {
    when(fileDefinitionEntityRepository.getReferenceById(FILE_DEFINITION_ID))
        .thenReturn(scenario.fileDefinitionEntity);
    when(jobProfileEntityRepository.getReferenceById(JOB_PROFILE_ID))
        .thenReturn(scenario.jobProfileEntity);
    when(mappingProfileEntityRepository.getReferenceById(MAPPING_PROFILE_ID))
        .thenReturn(scenario.mappingProfileEntity);
    when(jobExecutionService.getById(JOB_EXECUTION_ID)).thenReturn(scenario.jobExecution);
    when(jobExecutionService.getNextHrid()).thenReturn(expectedHrId);
    when(folioExecutionContext.getUserId()).thenReturn(USER_ID);
    when(userClient.getUserById(USER_ID.toString())).thenReturn(scenario.user);
  }

  private void stubAllModeCount(ExportRequest.IdTypeEnum idType, long count) {
    switch (idType) {
      case INSTANCE -> when(instanceEntityRepository.count()).thenReturn(count);
      case HOLDING -> when(holdingsRecordEntityRepository.count()).thenReturn(count);
      case AUTHORITY -> when(marcAuthorityRecordAllRepository.count()).thenReturn(count);
      default -> fail("IdType not supported in test: " + idType);
    }
  }

  private void runExecutorInline() {
    doAnswer(
            invocation -> {
              Runnable runnable = invocation.getArgument(0);
              runnable.run();
              return null;
            })
        .when(executor)
        .execute(any(Runnable.class));
  }

  @SneakyThrows
  private void setExecutorOnService() {
    Field executorField = DataExportService.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(dataExportService, executor);
  }

  private void withMockedFolioContext(Runnable action) {
    try (var mockedStatic = mockStatic(FolioExecutionScopeExecutionContextManager.class)) {
      mockedStatic
          .when(
              () ->
                  FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext(
                      any(Runnable.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      action.run();
    }
  }

  private static class Scenario {
    final FileDefinitionEntity fileDefinitionEntity;
    final JobProfileEntity jobProfileEntity;
    final MappingProfileEntity mappingProfileEntity;
    final JobExecution jobExecution;
    final User user;

    Scenario(
        FileDefinitionEntity fileDefinitionEntity,
        JobProfileEntity jobProfileEntity,
        MappingProfileEntity mappingProfileEntity,
        JobExecution jobExecution,
        User user) {
      this.fileDefinitionEntity = fileDefinitionEntity;
      this.jobProfileEntity = jobProfileEntity;
      this.mappingProfileEntity = mappingProfileEntity;
      this.jobExecution = jobExecution;
      this.user = user;
    }
  }
}
