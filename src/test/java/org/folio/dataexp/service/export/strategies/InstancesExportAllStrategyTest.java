package org.folio.dataexp.service.export.strategies;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.service.export.Constants.DELETED_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_KEY;
import static org.folio.dataexp.util.ErrorCode.ERROR_DELETED_TOO_LONG_INSTANCE;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.SneakyThrows;
import net.minidev.json.JSONObject;
import org.apache.maven.shared.utils.StringUtils;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.AuditInstanceEntity;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.AuditInstanceEntityRepository;
import org.folio.dataexp.repository.FolioInstanceAllRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.MarcInstanceAllRepository;
import org.folio.dataexp.repository.MarcInstanceRecordRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.dataexp.util.ErrorCode;
import org.folio.processor.RuleProcessor;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.ap.internal.util.Collections;
import org.marc4j.MarcException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InstancesExportAllStrategyTest {

  @Mock private AuditInstanceEntityRepository auditInstanceEntityRepository;
  @Mock private InstanceEntityRepository instanceEntityRepository;
  @Mock private JobExecutionEntityRepository jobExecutionEntityRepository;
  @Mock private ErrorLogService errorLogService;
  @Mock private LocalStorageWriter localStorageWriter;
  @Mock private HoldingsItemsResolverService holdingsItemsResolver;
  @Mock private JsonToMarcConverter jsonToMarcConverter;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private ConsortiaService consortiaService;
  @Mock private ReferenceDataProvider referenceDataProvider;
  @Mock private RuleFactory ruleFactory;
  @Mock private RuleHandler ruleHandler;
  @Mock private RuleProcessor ruleProcessor;
  @Mock private MarcInstanceRecordRepository marcInstanceRecordRepository;

  @InjectMocks private InstancesExportAllStrategy instancesExportAllStrategy;

  @Mock private FolioInstanceAllRepository folioInstanceAllRepository;

  @Mock private MarcInstanceAllRepository marcInstanceAllRepository;

  @Mock private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    instancesExportAllStrategy.folioExecutionContext = folioExecutionContext;
    instancesExportAllStrategy.setInstanceEntityRepository(instanceEntityRepository);
  }

  @Test
  void getIdentifierMessageTest() {
    var auditInstanceEntity =
        AuditInstanceEntity.builder().id(UUID.randomUUID()).hrid("123").title("title").build();

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());
    when(auditInstanceEntityRepository.findByIdIn(anySet()))
        .thenReturn(List.of(auditInstanceEntity));

    var opt = instancesExportAllStrategy.getIdentifiers(UUID.randomUUID());

    assertTrue(opt.isPresent());
    assertEquals("Instance with HRID : 123", opt.get().getIdentifierHridMessage());

    assertEquals(
        auditInstanceEntity.getId().toString(),
        opt.get().getAssociatedJsonObject().getAsString("id"));
    assertEquals("title", opt.get().getAssociatedJsonObject().getAsString("title"));
    assertEquals("123", opt.get().getAssociatedJsonObject().getAsString("hrid"));
  }

  @Test
  void getIdentifierMessageIfInstanceDoesNotExistTest() {
    var instanceId = UUID.fromString("b9d26945-9757-4855-ae6e-fd5d2f7d778e");
    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());
    when(auditInstanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());

    var opt = instancesExportAllStrategy.getIdentifiers(instanceId);

    assertTrue(opt.isPresent());
    assertEquals(
        "Instance with ID : b9d26945-9757-4855-ae6e-fd5d2f7d778e",
        opt.get().getIdentifierHridMessage());
  }

  @Test
  void saveConvertJsonRecordToMarcRecordErrorIfErrorRecordTooLongAndInstanceDeletedTest() {
    instancesExportAllStrategy.setErrorLogService(errorLogService);

    var auditInstanceEntity =
        AuditInstanceEntity.builder().id(UUID.randomUUID()).hrid("123").title("title").build();
    var jobExecutionId = UUID.randomUUID();
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var marcRecord =
        MarcRecordEntity.builder().externalId(instanceId).id(UUID.randomUUID()).build();
    var errorMessage =
        "Record is too long to be a valid MARC binary record, it's length would "
            + "be 113937 which is more thatn 99999 bytes 2024";

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());
    when(auditInstanceEntityRepository.findByIdIn(anySet()))
        .thenReturn(List.of(auditInstanceEntity));

    instancesExportAllStrategy.saveConvertJsonRecordToMarcRecordError(
        marcRecord, jobExecutionId, new IOException(errorMessage));
    verify(errorLogService)
        .saveWithAffectedRecord(
            isA(JSONObject.class),
            eq(errorMessage),
            eq(ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode()),
            isA(UUID.class));
  }

  @Test
  void saveDuplicateErrorsIfInstanceDeletedTest() {
    instancesExportAllStrategy.setErrorLogService(errorLogService);
    instancesExportAllStrategy.setInstanceEntityRepository(instanceEntityRepository);

    ReflectionTestUtils.setField(
        instancesExportAllStrategy, "jsonToMarcConverter", jsonToMarcConverter);

    var auditInstanceEntity =
        AuditInstanceEntity.builder().id(UUID.randomUUID()).hrid("123").title("title").build();
    var jobExecutionId = UUID.randomUUID();
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());
    when(jobExecutionEntityRepository.getReferenceById(jobExecutionId))
        .thenReturn(
            new JobExecutionEntity()
                .withJobExecution(new JobExecution().progress(new JobExecutionProgress())));
    when(auditInstanceEntityRepository.findByIdIn(anySet()))
        .thenReturn(List.of(auditInstanceEntity));

    var externalIds = Collections.asSet(instanceId);
    var statistic =
        new ExportStrategyStatistic(
            new ExportedRecordsListener(jobExecutionEntityRepository, 1, jobExecutionId));
    var marcRecordDuplicate =
        MarcRecordEntity.builder().externalId(instanceId).id(UUID.randomUUID()).build();
    var marcRecord =
        MarcRecordEntity.builder()
            .externalId(instanceId)
            .id(UUID.randomUUID())
            .deleted(true)
            .build();
    instancesExportAllStrategy.createAndSaveMarcFromJsonRecord(
        externalIds,
        statistic,
        new MappingProfile(),
        jobExecutionId,
        Set.of(instanceId),
        List.of(marcRecord, marcRecordDuplicate),
        localStorageWriter);
    var expectedErrorMessage =
        format(
            "Instance with HRID : 123 has following SRS records associated: %s, %s",
            marcRecord.getId(), marcRecordDuplicate.getId());
    verify(errorLogService)
        .saveWithAffectedRecord(
            any(),
            eq(expectedErrorMessage),
            eq(ErrorCode.ERROR_DUPLICATE_SRS_RECORD.getCode()),
            eq(jobExecutionId));
  }

  @Test
  void saveInstanceTooLongErrorsIfInstanceDeletedTest() {
    instancesExportAllStrategy.setErrorLogService(errorLogService);

    ReflectionTestUtils.setField(
        instancesExportAllStrategy, "jsonToMarcConverter", jsonToMarcConverter);
    var auditInstanceEntity =
        AuditInstanceEntity.builder()
            .id(UUID.randomUUID())
            .hrid("123")
            .title(generateTooLongString())
            .build();

    var instanceWithHoldingsAndItems = new JSONObject();
    var jsonInstance = new JSONObject();
    instanceWithHoldingsAndItems.put(INSTANCE_KEY, jsonInstance);
    jsonInstance.put("id", auditInstanceEntity.getId());
    jsonInstance.put("title", auditInstanceEntity.getTitle());
    jsonInstance.put(DELETED_KEY, true);
    var instancesWithHoldingsAndItems = List.of(instanceWithHoldingsAndItems);

    when(ruleProcessor.process(any(), any(), any(), any(), any())).thenThrow(new MarcException());

    var jobExecutionId = UUID.randomUUID();
    instancesExportAllStrategy.getGeneratedMarc(
        new GeneratedMarcResult(jobExecutionId),
        instancesWithHoldingsAndItems,
        new MappingProfile(),
        jobExecutionId);
    verify(errorLogService)
        .saveWithAffectedRecord(
            isA(JSONObject.class),
            eq(ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode()),
            isA(UUID.class),
            isA(MarcException.class));
    verify(errorLogService)
        .saveGeneralErrorWithMessageValues(
            eq(ERROR_DELETED_TOO_LONG_INSTANCE.getCode()),
            eq(List.of(auditInstanceEntity.getId().toString())),
            isA(UUID.class));
  }

  @Test
  void saveConvertJsonRecordToMarcRecordErrorIfErrorRecordTooLongAndInstanceNotDeletedTest() {
    instancesExportAllStrategy.setErrorLogService(errorLogService);

    var jobExecutionId = UUID.randomUUID();
    var instance =
        "{'id' : '1eaa1eef-1633-4c7e-af09-796315ebc576', 'hrid' : 'instHrid', 'title' : 'title'}";
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var instanceEntity = InstanceEntity.builder().jsonb(instance).id(instanceId).build();
    var marcRecord = MarcRecordEntity.builder().externalId(instanceId).build();
    var errorMessage =
        "Record is too long to be a valid MARC binary record, it's length would be 113937 which"
            + " is more than 99999 bytes 2024";

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceEntity));

    instancesExportAllStrategy.saveConvertJsonRecordToMarcRecordError(
        marcRecord, jobExecutionId, new IOException(errorMessage));

    verify(errorLogService)
        .saveWithAffectedRecord(
            isA(JSONObject.class),
            eq(errorMessage),
            eq(ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode()),
            isA(UUID.class));
  }

  @Test
  void saveConvertJsonRecordToMarcRecordErrorIfNotErrorRecordTooLongTest() {
    instancesExportAllStrategy.setErrorLogService(errorLogService);

    var jobExecutionId = UUID.randomUUID();
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var marcRecord = MarcRecordEntity.builder().externalId(instanceId).build();
    var errorMessage = "error message";

    instancesExportAllStrategy.saveConvertJsonRecordToMarcRecordError(
        marcRecord, jobExecutionId, new IOException(errorMessage));

    var expectedErrorMessage =
        "Error converting json to marc for record 1eaa1eef-1633-4c7e-af09-796315ebc576";
    verify(errorLogService).saveGeneralError(expectedErrorMessage, jobExecutionId);
  }

  @Test
  @TestMate(name = "TestMate-134ad8035ee1864307dd8f6ab3e97cf2")
  void processSlicesShouldProcessMarcInstanceSlicesWhenProfileIsNotDefaultAndNoSrsType() {
    // Given
    var batchSize = 10;
    ReflectionTestUtils.setField(instancesExportAllStrategy, "exportIdsBatch", batchSize);
    ReflectionTestUtils.setField(instancesExportAllStrategy, "entityManager", entityManager);
    var exportRequest = new ExportRequest();
    exportRequest.setSuppressedFromDiscovery(true);
    exportRequest.setDeletedRecords(false);
    exportRequest.setLastExport(false);
    var mappingProfile = new MappingProfile();
    mappingProfile.setDefault(false);
    mappingProfile.setRecordTypes(List.of(RecordTypes.INSTANCE));
    var instanceEntity =
        InstanceEntity.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000004"))
            .jsonb("{\"id\":\"00000000-0000-0000-0000-000000000004\", \"hrid\":\"inst001\"}")
            .build();
    Slice<InstanceEntity> folioSlice =
        new SliceImpl<>(List.of(instanceEntity), PageRequest.of(0, batchSize), false);
    Slice<InstanceEntity> marcInstanceSlice =
        new SliceImpl<>(List.of(instanceEntity), PageRequest.of(0, batchSize), false);
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var fromId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var toId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    when(folioInstanceAllRepository.findFolioInstanceAllNonDeletedSuppressed(
            eq(fromId), eq(toId), any(PageRequest.class)))
        .thenReturn(folioSlice);
    when(folioInstanceAllRepository.findMarcInstanceAllNonDeletedCustomInstanceProfile(
            eq(fromId), eq(toId), any(PageRequest.class)))
        .thenReturn(marcInstanceSlice);
    var exportStatistic = new ExportStrategyStatistic(mock(ExportedRecordsListener.class));
    var exportFilesEntity =
        JobExecutionExportFilesEntity.builder()
            .jobExecutionId(jobExecutionId)
            .fromId(fromId)
            .toId(toId)
            .fileLocation("test-file")
            .build();
    // When
    instancesExportAllStrategy.processSlices(
        exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    // Then
    verify(folioInstanceAllRepository)
        .findFolioInstanceAllNonDeletedSuppressed(eq(fromId), eq(toId), any(PageRequest.class));
    verify(folioInstanceAllRepository)
        .findMarcInstanceAllNonDeletedCustomInstanceProfile(
            eq(fromId), eq(toId), any(PageRequest.class));
    verify(marcInstanceAllRepository, never()).findMarcInstanceAllNonDeleted(any(), any(), any());
    verify(marcInstanceAllRepository, never())
        .findMarcInstanceAllNonDeletedNonSuppressed(any(), any(), any());
    verify(entityManager, atLeastOnce()).clear();
  }

  @Test
  @TestMate(name = "TestMate-40b1392229d286cc534f4c16ed5e09b4")
  void processSlicesShouldHandleDeletedMarcRecordsWhenSrsRequested() throws IOException {
    // Given
    var batchSize = 10;

    var mappingProfileEntityRepository =
        mock(org.folio.dataexp.repository.MappingProfileEntityRepository.class);

    ReflectionTestUtils.setField(instancesExportAllStrategy, "exportIdsBatch", batchSize);
    ReflectionTestUtils.setField(instancesExportAllStrategy, "entityManager", entityManager);
    ReflectionTestUtils.setField(
        instancesExportAllStrategy, "jsonToMarcConverter", jsonToMarcConverter);
    ReflectionTestUtils.setField(
        instancesExportAllStrategy,
        "mappingProfileEntityRepository",
        mappingProfileEntityRepository);
    var exportRequest = new ExportRequest();
    exportRequest.setSuppressedFromDiscovery(true);
    exportRequest.setDeletedRecords(true);
    exportRequest.setLastExport(true);

    var mappingProfile = new MappingProfile();
    mappingProfile.setDefault(false);
    mappingProfile.setRecordTypes(List.of(RecordTypes.SRS));

    var defaultMappingProfile = new MappingProfile();
    defaultMappingProfile.setRecordTypes(new java.util.ArrayList<>());
    var defaultMappingProfileEntity = new org.folio.dataexp.domain.entity.MappingProfileEntity();
    defaultMappingProfileEntity.setMappingProfile(defaultMappingProfile);

    var deletedMarcRecord =
        MarcRecordEntity.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000004"))
            .externalId(UUID.fromString("00000000-0000-0000-0000-000000000005"))
            .content("{\"leader\":\"00000nam  2200000 i 4500\"}")
            .build();
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var fromId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var toId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    when(mappingProfileEntityRepository.getReferenceById(any(UUID.class)))
        .thenReturn(defaultMappingProfileEntity);
    when(folioInstanceAllRepository.findFolioInstanceAll(
            eq(fromId), eq(toId), any(PageRequest.class)))
        .thenReturn(new SliceImpl<>(List.of()));
    when(marcInstanceAllRepository.findMarcInstanceAllNonDeleted(
            eq(fromId), eq(toId), any(PageRequest.class)))
        .thenReturn(new SliceImpl<>(List.of()));
    when(marcInstanceAllRepository.findMarcInstanceAllDeleted())
        .thenReturn(List.of(deletedMarcRecord));
    when(jsonToMarcConverter.convertJsonRecordToMarcRecord(any(), any(), any()))
        .thenReturn("marc-content");
    when(consortiaService.getCentralTenantId(any())).thenReturn("central");

    var exportStatistic = new ExportStrategyStatistic(mock(ExportedRecordsListener.class));
    var exportFilesEntity =
        JobExecutionExportFilesEntity.builder()
            .jobExecutionId(jobExecutionId)
            .fromId(fromId)
            .toId(toId)
            .build();

    // When
    instancesExportAllStrategy.processSlices(
        exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);

    // Then
    verify(folioInstanceAllRepository)
        .findFolioInstanceAll(eq(fromId), eq(toId), any(PageRequest.class));
    verify(marcInstanceAllRepository)
        .findMarcInstanceAllNonDeleted(eq(fromId), eq(toId), any(PageRequest.class));
    verify(marcInstanceAllRepository).findMarcInstanceAllDeleted();
    verify(entityManager, atLeastOnce()).clear();
    verify(localStorageWriter).write("marc-content");

    assertThat(deletedMarcRecord.isDeleted()).isTrue();
  }

  @Test
  @SneakyThrows
  void processSlicesShouldExcludeSharedWhenHandleDeletedMarcRecordsWhenSrsRequested() {
    // Given
    var batchSize = 10;

    var mappingProfileEntityRepository =
        mock(org.folio.dataexp.repository.MappingProfileEntityRepository.class);

    ReflectionTestUtils.setField(instancesExportAllStrategy, "exportIdsBatch", batchSize);
    ReflectionTestUtils.setField(instancesExportAllStrategy, "entityManager", entityManager);
    ReflectionTestUtils.setField(
        instancesExportAllStrategy, "jsonToMarcConverter", jsonToMarcConverter);
    ReflectionTestUtils.setField(
        instancesExportAllStrategy,
        "mappingProfileEntityRepository",
        mappingProfileEntityRepository);
    var exportRequest = new ExportRequest();
    exportRequest.setSuppressedFromDiscovery(true);
    exportRequest.setDeletedRecords(true);
    exportRequest.setLastExport(true);

    var mappingProfile = new MappingProfile();
    mappingProfile.setDefault(false);
    mappingProfile.setRecordTypes(List.of(RecordTypes.SRS));

    var defaultMappingProfile = new MappingProfile();
    defaultMappingProfile.setRecordTypes(new java.util.ArrayList<>());
    var defaultMappingProfileEntity = new org.folio.dataexp.domain.entity.MappingProfileEntity();
    defaultMappingProfileEntity.setMappingProfile(defaultMappingProfile);

    var deletedMarcRecord =
        MarcRecordEntity.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000004"))
            .externalId(UUID.fromString("00000000-0000-0000-0000-000000000005"))
            .content("{\"leader\":\"00000dam  2200000 i 4500\"}")
            .build();
    var sharedMarcRecord =
        MarcRecordEntity.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000006"))
            .externalId(UUID.fromString("00000000-0000-0000-0000-000000000005"))
            .content("{\"leader\":\"00000nam  2200000 i 4500\"}")
            .build();
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var fromId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var toId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    when(mappingProfileEntityRepository.getReferenceById(any(UUID.class)))
        .thenReturn(defaultMappingProfileEntity);
    when(folioInstanceAllRepository.findFolioInstanceAll(
            eq(fromId), eq(toId), any(PageRequest.class)))
        .thenReturn(new SliceImpl<>(List.of()));
    when(marcInstanceAllRepository.findMarcInstanceAllNonDeleted(
            eq(fromId), eq(toId), any(PageRequest.class)))
        .thenReturn(new SliceImpl<>(List.of()));
    when(marcInstanceAllRepository.findMarcInstanceAllDeleted())
        .thenReturn(List.of(deletedMarcRecord));
    when(consortiaService.getCentralTenantId(any())).thenReturn("central");
    when(jsonToMarcConverter.convertJsonRecordToMarcRecord(any(), any(), any()))
      .thenReturn("marc-content");
    when(marcInstanceRecordRepository.findByExternalIdIn(
            "central", Set.of(sharedMarcRecord.getExternalId())))
        .thenReturn(
            List.of(
                MarcRecordEntity.builder().externalId(sharedMarcRecord.getExternalId()).build()));

    var exportStatistic = new ExportStrategyStatistic(mock(ExportedRecordsListener.class));
    var exportFilesEntity =
        JobExecutionExportFilesEntity.builder()
            .jobExecutionId(jobExecutionId)
            .fromId(fromId)
            .toId(toId)
            .build();

    // When
    instancesExportAllStrategy.processSlices(
        exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);

    // Then
    verify(folioInstanceAllRepository)
        .findFolioInstanceAll(eq(fromId), eq(toId), any(PageRequest.class));
    verify(marcInstanceAllRepository)
        .findMarcInstanceAllNonDeleted(eq(fromId), eq(toId), any(PageRequest.class));
    verify(marcInstanceAllRepository).findMarcInstanceAllDeleted();
    verify(entityManager, atLeastOnce()).clear();
    verify(localStorageWriter).write("marc-content");

    assertThat(deletedMarcRecord.isDeleted()).isFalse();
  }

  private String generateTooLongString() {
    return StringUtils.repeat("abcd", 99999);
  }
}
