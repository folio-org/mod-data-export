package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.util.Constants.DEFAULT_AUTHORITY_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_HOLDINGS_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_INSTANCE_JOB_PROFILE_ID;
import static org.folio.dataexp.util.ErrorCode.ERROR_DUPLICATED_IDS;
import static org.folio.dataexp.util.ErrorCode.ERROR_INVALID_CQL_SYNTAX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.client.SearchClient;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.ExportAllRequest;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.IdsJob;
import org.folio.dataexp.domain.dto.IdsJobPayload;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.ResourceIds;
import org.folio.dataexp.domain.dto.User;
import org.folio.dataexp.domain.entity.ExportIdEntity;
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
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.service.validators.DataExportRequestValidator;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

  @Captor private ArgumentCaptor<List<ExportIdEntity>> batchCaptor;

  @Mock private SearchClient searchClient;

  @Mock private InsertExportIdService insertExportIdService;

  @Mock private ErrorLogService errorLogService;

  @Mock private FolioS3Client s3Client;

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

  @Test
  @TestMate(name = "TestMate-e78a0fbbddefda7f93b2e1922fb68c46")
  void testReadFileWhenUploadFormatIsCsvShouldProcessIdsAndSaveInBatches() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var jobExecution =
        new JobExecution()
            .id(jobExecutionId)
            .progress(new JobExecutionProgress().total(0).readIds(0).exported(0).failed(0));
    var uuids =
        IntStream.range(0, 1005)
            .mapToObj(i -> UUID.nameUUIDFromBytes(String.valueOf(i).getBytes()).toString())
            .collect(Collectors.toList());
    // Keep track of clean UUIDs for verification
    var expectedUuid0 = uuids.get(0);
    var expectedUuid1 = uuids.get(1);
    // Add dirty data to test cleaning logic
    uuids.set(0, "\"" + expectedUuid0 + "\""); // Wrapped in quotes
    uuids.set(1, "\uFEFF" + expectedUuid1); // Starts with BOM
    var csvContent = String.join(System.lineSeparator(), uuids);
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    // Two passes: one for counting lines, one for processing
    when(s3Client.read(anyString()))
        .thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)))
        .thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));
    when(exportIdEntityRepository.countByJobExecutionId(jobExecutionId)).thenReturn(1005L);
    // Instantiate the real processor using existing mocks to execute the actual logic
    var realInputFileProcessor =
        new InputFileProcessor(
            exportIdEntityRepository,
            s3Client,
            searchClient,
            errorLogService,
            jobExecutionService,
            insertExportIdService);
    // Use doAnswer to capture copies of the batch because the implementation clears the list
    // reference
    List<List<ExportIdEntity>> capturedBatches = new java.util.ArrayList<>();
    doAnswer(
            invocation -> {
              List<ExportIdEntity> batchArg = invocation.getArgument(0);
              capturedBatches.add(new java.util.ArrayList<>(batchArg));
              return null;
            })
        .when(insertExportIdService)
        .saveBatch(any());
    var fileDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var fileName = "test.csv";
    var fileDefinition =
        new FileDefinition()
            .id(fileDefinitionId)
            .jobExecutionId(jobExecutionId)
            .fileName(fileName)
            .uploadFormat(FileDefinition.UploadFormatEnum.CSV);
    var commonExportStatistic = new CommonExportStatistic();
    // When
    realInputFileProcessor.readFile(
        fileDefinition, commonExportStatistic, ExportRequest.IdTypeEnum.INSTANCE);
    // Then
    assertThat(jobExecution.getProgress().getTotal()).isEqualTo(1005);
    assertThat(jobExecution.getProgress().getReadIds()).isEqualTo(1005);
    assertThat(commonExportStatistic.isFailedToReadInputFile()).isFalse();
    assertThat(commonExportStatistic.getInvalidUuidFormat()).isEmpty();
    assertThat(commonExportStatistic.getDuplicatedUuidAmount()).isZero();

    assertThat(capturedBatches).hasSize(2);
    assertThat(capturedBatches.get(0)).hasSize(1000);
    assertThat(capturedBatches.get(1)).hasSize(5);

    // Verify cleaning logic: check first two entities in first batch
    var firstBatch = capturedBatches.get(0);
    assertThat(firstBatch.get(0).getInstanceId().toString()).hasToString(expectedUuid0);
    assertThat(firstBatch.get(1).getInstanceId().toString()).hasToString(expectedUuid1);

    // 1 (initial total) + 1 (first batch of 1000) + 1 (final remainder)
    verify(jobExecutionService, times(3)).save(isA(JobExecution.class));
    verify(errorLogService, times(0)).saveGeneralErrorWithMessageValues(any(), any(), any());
  }

  @Test
  @TestMate(name = "TestMate-7363dc5f2faf8525b6352ef92c3cbbe6")
  void testReadFileWhenCsvHasDuplicatesShouldLogErrorsAndIncrementStats() {
    // Given
    var fileDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var instanceIdA = UUID.fromString("00000000-0000-0000-0000-00000000000A");
    var instanceIdB = UUID.fromString("00000000-0000-0000-0000-00000000000B");
    var jobExecution =
        new JobExecution()
            .id(jobExecutionId)
            .progress(new JobExecutionProgress().total(0).readIds(0).exported(0).failed(0));
    var csvContent = String.format("%s%n%s%n%s", instanceIdA, instanceIdA, instanceIdB);
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    // Two passes: one for counting lines, one for processing
    when(s3Client.read(anyString()))
        .thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)))
        .thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));
    // Stub the repository to return 2 unique IDs found in the DB after processing
    when(exportIdEntityRepository.countByJobExecutionId(jobExecutionId)).thenReturn(2L);
    // Use doAnswer to capture a copy of the batch because the implementation clears the list
    // reference
    List<ExportIdEntity> capturedEntities = new java.util.ArrayList<>();
    doAnswer(
            invocation -> {
              List<ExportIdEntity> batchArg = invocation.getArgument(0);
              capturedEntities.addAll(new java.util.ArrayList<>(batchArg));
              return null;
            })
        .when(insertExportIdService)
        .saveBatch(any());
    var inputFileProcessorInternal =
        new org.folio.dataexp.service.InputFileProcessor(
            exportIdEntityRepository,
            s3Client,
            searchClient,
            errorLogService,
            jobExecutionService,
            insertExportIdService);
    var commonExportStatistic = new CommonExportStatistic();
    var fileDefinition =
        new FileDefinition()
            .id(fileDefinitionId)
            .jobExecutionId(jobExecutionId)
            .fileName("duplicates.csv")
            .uploadFormat(FileDefinition.UploadFormatEnum.CSV);
    // When
    inputFileProcessorInternal.readFile(
        fileDefinition, commonExportStatistic, ExportRequest.IdTypeEnum.INSTANCE);
    // Then
    assertThat(jobExecution.getProgress().getTotal()).isEqualTo(3);
    assertThat(jobExecution.getProgress().getReadIds()).isEqualTo(3);
    assertThat(commonExportStatistic.getDuplicatedUuidAmount()).isEqualTo(1);
    assertThat(commonExportStatistic.isFailedToReadInputFile()).isFalse();
    assertThat(capturedEntities).hasSize(2);
    assertThat(capturedEntities.stream().map(ExportIdEntity::getInstanceId).toList())
        .containsExactlyInAnyOrder(instanceIdA, instanceIdB);
    verify(errorLogService)
        .saveGeneralErrorWithMessageValues(
            ERROR_DUPLICATED_IDS.getCode(), List.of(instanceIdA.toString(), "2"), jobExecutionId);
    verify(jobExecutionService, atLeastOnce()).save(isA(JobExecution.class));
  }

  @Test
  @TestMate(name = "TestMate-1e539259a7e54e170459af439c9c3dda")
  @SneakyThrows
  void testReadFileWhenUploadFormatIsCqlShouldSubmitSearchJobAndProcessResults() {
    // Given
    var instanceId = UUID.fromString("00000000-0000-0000-0000-00000000000A");

    var resourceIds = mock(ResourceIds.class);
    // Use the actual generated nested class type to avoid compilation errors
    var mockResourceId = mock(org.folio.dataexp.domain.dto.ResourceIds.Id.class);
    when(mockResourceId.getId()).thenReturn(instanceId);

    when(resourceIds.getTotalRecords()).thenReturn(1);
    when(resourceIds.getIds()).thenReturn(List.of(mockResourceId));

    inputFileProcessor =
        new InputFileProcessor(
            exportIdEntityRepository,
            s3Client,
            searchClient,
            errorLogService,
            jobExecutionService,
            insertExportIdService);

    Field waitField = InputFileProcessor.class.getDeclaredField("waitSearchIdsTimeSeconds");
    waitField.setAccessible(true);
    waitField.set(inputFileProcessor, 20);

    var fileDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var searchJobId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    var fileName = "query.cql";
    var cqlQuery = "cql.allRecords=1";
    var jobExecution =
        new JobExecution()
            .id(jobExecutionId)
            .progress(new JobExecutionProgress().total(0).readIds(0).exported(0).failed(0));
    var idsJobInProgress = new IdsJob().withId(searchJobId).withStatus(IdsJob.Status.IN_PROGRESS);
    var idsJobCompleted = new IdsJob().withId(searchJobId).withStatus(IdsJob.Status.COMPLETED);

    when(s3Client.read(anyString()))
        .thenReturn(new ByteArrayInputStream(cqlQuery.getBytes(StandardCharsets.UTF_8)));
    when(searchClient.submitIdsJob(any(IdsJobPayload.class))).thenReturn(idsJobInProgress);
    when(searchClient.getJobStatus(searchJobId.toString()))
        .thenReturn(idsJobInProgress)
        .thenReturn(idsJobCompleted);
    when(searchClient.getResourceIds(searchJobId.toString())).thenReturn(resourceIds);
    when(jobExecutionService.getById(jobExecutionId)).thenReturn(jobExecution);
    var fileDefinition =
        new FileDefinition()
            .id(fileDefinitionId)
            .jobExecutionId(jobExecutionId)
            .fileName(fileName)
            .uploadFormat(FileDefinition.UploadFormatEnum.CQL);
    var commonExportStatistic = new CommonExportStatistic();

    // When
    inputFileProcessor.readFile(
        fileDefinition, commonExportStatistic, ExportRequest.IdTypeEnum.INSTANCE);

    // Then
    var payloadCaptor = ArgumentCaptor.forClass(IdsJobPayload.class);
    verify(searchClient).submitIdsJob(payloadCaptor.capture());
    assertThat(payloadCaptor.getValue().getQuery()).isEqualTo(cqlQuery);
    assertThat(payloadCaptor.getValue().getEntityType())
        .isEqualTo(IdsJobPayload.EntityType.INSTANCE);
    verify(searchClient, atLeast(2)).getJobStatus(searchJobId.toString());
    verify(searchClient).getResourceIds(searchJobId.toString());

    verify(insertExportIdService).saveBatch(batchCaptor.capture());
    List<ExportIdEntity> capturedBatch = batchCaptor.getValue();
    assertThat(capturedBatch).hasSize(1);
    assertThat(capturedBatch.getFirst().getInstanceId()).isEqualTo(instanceId);
    assertThat(capturedBatch.getFirst().getJobExecutionId()).isEqualTo(jobExecutionId);
    assertThat(jobExecution.getProgress().getTotal()).isEqualTo(1);
    assertThat(jobExecution.getProgress().getReadIds()).isEqualTo(1);
    verify(jobExecutionService, atLeast(1)).save(jobExecution);
  }

  @Test
  @TestMate(name = "TestMate-7bec775a1cb3c9e884a6d907f10dbdec")
  @SneakyThrows
  void testReadFileWhenCqlSearchJobFailsShouldLogError() {
    // Given
    var cqlQuery = "cql.allRecords=1";

    // Use the real InputFileProcessor to execute the actual logic of readCqlFile
    var realInputFileProcessor =
        new InputFileProcessor(
            exportIdEntityRepository,
            s3Client,
            searchClient,
            errorLogService,
            jobExecutionService,
            insertExportIdService);

    // Set the wait time to a value strictly greater than SEARCH_POLL_INTERVAL_SECONDS (5L)
    // Awaitility requires timeout > poll interval to avoid IllegalArgumentException
    Field waitField = InputFileProcessor.class.getDeclaredField("waitSearchIdsTimeSeconds");
    waitField.setAccessible(true);
    waitField.set(realInputFileProcessor, 10);

    // Mocking dependencies
    when(s3Client.read(anyString()))
        .thenReturn(new ByteArrayInputStream(cqlQuery.getBytes(StandardCharsets.UTF_8)));

    // Submit job returns the job info
    var searchJobId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    var idsJobInitial = new IdsJob().withId(searchJobId).withStatus(IdsJob.Status.IN_PROGRESS);
    when(searchClient.submitIdsJob(any(IdsJobPayload.class))).thenReturn(idsJobInitial);

    // Mock getJobStatus to return ERROR.
    // The implementation calls it in until() and then again to check the final status.
    var idsJobError = new IdsJob().withId(searchJobId).withStatus(IdsJob.Status.ERROR);
    when(searchClient.getJobStatus(searchJobId.toString())).thenReturn(idsJobError);

    var commonExportStatistic = new CommonExportStatistic();
    var fileDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var fileName = "query.cql";
    var fileDefinition =
        new FileDefinition()
            .id(fileDefinitionId)
            .jobExecutionId(jobExecutionId)
            .fileName(fileName)
            .uploadFormat(FileDefinition.UploadFormatEnum.CQL);

    // When
    realInputFileProcessor.readFile(
        fileDefinition, commonExportStatistic, ExportRequest.IdTypeEnum.INSTANCE);

    // Then
    // Verify that the error was logged with the correct parameters
    verify(errorLogService)
        .saveGeneralErrorWithMessageValues(
            ERROR_INVALID_CQL_SYNTAX.getCode(),
            Collections.singletonList(fileName),
            jobExecutionId);

    // Verify search client interactions
    verify(searchClient, atLeastOnce()).getJobStatus(searchJobId.toString());
    verify(searchClient, never()).getResourceIds(anyString());
    verify(insertExportIdService, never()).saveBatch(any());
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

  private record Scenario(
      FileDefinitionEntity fileDefinitionEntity,
      JobProfileEntity jobProfileEntity,
      MappingProfileEntity mappingProfileEntity,
      JobExecution jobExecution,
      User user) {}
}
