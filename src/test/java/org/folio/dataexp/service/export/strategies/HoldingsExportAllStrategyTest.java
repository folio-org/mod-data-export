package org.folio.dataexp.service.export.strategies;

import org.folio.dataexp.client.ConsortiumSearchClient;
import org.folio.dataexp.repository.FolioHoldingsAllRepository;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.HoldingsRecordEntityTenantRepository;
import org.folio.dataexp.repository.InstanceCentralTenantRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.repository.MarcHoldingsAllRepository;
import org.folio.dataexp.repository.MarcInstanceRecordRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.dataexp.service.validators.PermissionsValidator;
import org.folio.processor.RuleProcessor;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import org.folio.dataexp.service.export.strategies.ExportStrategyStatistic;
import org.folio.dataexp.service.export.strategies.HoldingsExportAllStrategy;
import org.mockito.Spy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import jakarta.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Slice;
import org.folio.dataexp.service.export.strategies.RuleFactory;
import org.folio.dataexp.service.export.strategies.UserService;
import org.folio.dataexp.service.export.strategies.GeneratedMarcResult;
import org.folio.dataexp.service.export.strategies.ExportIdentifiersForDuplicateError;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.lenient;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.ArgumentMatchers;

@ExtendWith(MockitoExtension.class)
class HoldingsExportAllStrategyTest {

  @Mock
  private ItemEntityRepository itemEntityRepository;
  @Mock
  private RuleFactory ruleFactory;
  @Mock
  private RuleProcessor ruleProcessor;
  @Mock
  private RuleHandler ruleHandler;
  @Mock
  private ReferenceDataProvider referenceDataProvider;
  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private ConsortiumSearchClient consortiumSearchClient;
  @Mock
  private HoldingsRecordEntityTenantRepository holdingsRecordEntityTenantRepository;
  @Mock
  private MarcInstanceRecordRepository marcInstanceRecordRepository;
  @Mock
  private InstanceCentralTenantRepository instanceCentralTenantRepository;
  @Mock
  private FolioModuleMetadata folioModuleMetadata;
  @Mock
  private HoldingsRecordEntityRepository holdingsRecordEntityRepository;
  @Mock
  private MarcRecordEntityRepository marcRecordEntityRepository;
  @Mock
  private FolioHoldingsAllRepository folioHoldingsAllRepository;
  @Mock
  private MarcHoldingsAllRepository marcHoldingsAllRepository;
  @Mock
  private UserService userService;
  @Mock
  private PermissionsValidator permissionsValidator;
  @Mock
  private FolioExecutionContext folioExecutionContext;

  @InjectMocks
  private HoldingsExportAllStrategy holdingsExportAllStrategy;

    @Mock
  private EntityManager entityManager;

    @Mock
  private LocalStorageWriter localStorageWriter;

    @Mock
  private InstanceEntityRepository instanceEntityRepository;

    @Test
  void processSlicesShouldProcessFolioAndDefaultMarcSlicesWhenMappingProfileIsDefault() {
    // TestMate-183d7632f74bf8f575592cc3dafd05c5
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var fromId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var toId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    var tenantId = "tenant";
    var exportFilesEntity =
        new JobExecutionExportFilesEntity()
            .withJobExecutionId(jobExecutionId)
            .withFromId(fromId)
            .withToId(toId);
    var exportRequest =
        new ExportRequest()
            .deletedRecords(false)
            .lastExport(false)
            .suppressedFromDiscovery(false);
    var mappingProfile = new MappingProfile();
    mappingProfile.setDefault(true);
    var folioHolding = new HoldingsRecordEntity();
    folioHolding.setId(UUID.fromString("00000000-0000-0000-0000-000000000004"));
    folioHolding.setJsonb("{}");
    folioHolding.setInstanceId(UUID.fromString("00000000-0000-0000-0000-000000000007"));
    var folioSlice = new SliceImpl<>(List.of(folioHolding), PageRequest.of(0, 1), false);
    var marcRecord = new MarcRecordEntity();
    marcRecord.setId(UUID.fromString("00000000-0000-0000-0000-000000000005"));
    marcRecord.setExternalId(UUID.fromString("00000000-0000-0000-0000-000000000006"));
    var marcSlice = new SliceImpl<>(List.of(marcRecord), PageRequest.of(0, 1), false);
    var exportStatistic = new ExportStrategyStatistic(null);
    holdingsExportAllStrategy.setExportIdsBatch(1);
    // Manually inject dependencies not handled by @InjectMocks for parent classes
    holdingsExportAllStrategy.entityManager = entityManager;
    holdingsExportAllStrategy.folioExecutionContext = folioExecutionContext;
    holdingsExportAllStrategy.setInstanceEntityRepository(instanceEntityRepository);
    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(folioHoldingsAllRepository.findFolioHoldingsAllNonDeletedNonSuppressed(
            eq(fromId), eq(toId), any(Pageable.class)))
        .thenReturn(folioSlice);
    when(marcHoldingsAllRepository.findMarcHoldingsAllNonDeletedNonSuppressed(
            eq(fromId), eq(toId), any(Pageable.class)))
        .thenReturn(marcSlice);
    // Use Spy to isolate processSlices routing from complex transformation logic
    var spyStrategy = org.mockito.Mockito.spy(holdingsExportAllStrategy);
    doNothing().when(spyStrategy).createAndSaveGeneratedMarc(any(), any(), any());
    doNothing()
        .when(spyStrategy)
        .createAndSaveMarcFromJsonRecord(any(), any(), any(), any(), any(), any(), any());
    // When
    spyStrategy.processSlices(
        exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    // Then
    verify(folioHoldingsAllRepository)
        .findFolioHoldingsAllNonDeletedNonSuppressed(fromId, toId, PageRequest.of(0, 1));
    verify(marcHoldingsAllRepository)
        .findMarcHoldingsAllNonDeletedNonSuppressed(fromId, toId, PageRequest.of(0, 1));
    verify(folioHoldingsAllRepository, never())
        .findMarcHoldingsAllNonDeletedCustomHoldingsProfile(any(), any(), any());
    verify(folioHoldingsAllRepository, never()).findFolioHoldingsAllDeleted();
    // entityManager.clear() is called 3 times:
    // 1. in processFolioSlices
    // 2. in getHoldingsWithInstanceAndItems (called by processFolioHoldings)
    // 3. in processMarcSlices
    verify(entityManager, times(3)).clear();
  }

    @Test
  void processSlicesShouldProcessFolioAndCustomMarcHoldingsSlicesWhenMappingProfileIsNotDefault() {
    // TestMate-7aaa467b039667401ad2053bbfc2c189
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var fromId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var toId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    var tenantId = "tenant";
    var exportFilesEntity = new JobExecutionExportFilesEntity()
        .withJobExecutionId(jobExecutionId)
        .withFromId(fromId)
        .withToId(toId);
    var exportRequest = new ExportRequest()
        .deletedRecords(false)
        .lastExport(false)
        .suppressedFromDiscovery(false);
    var mappingProfile = new MappingProfile();
    mappingProfile.setDefault(false);
    var folioHolding = new HoldingsRecordEntity();
    folioHolding.setId(UUID.fromString("00000000-0000-0000-0000-000000000004"));
    folioHolding.setJsonb("{}");
    folioHolding.setInstanceId(UUID.fromString("00000000-0000-0000-0000-000000000007"));
    var folioSlice = new SliceImpl<>(List.of(folioHolding), PageRequest.of(0, 1), false);
    var marcCustomHolding = new HoldingsRecordEntity();
    marcCustomHolding.setId(UUID.fromString("00000000-0000-0000-0000-000000000005"));
    marcCustomHolding.setJsonb("{}");
    marcCustomHolding.setInstanceId(UUID.fromString("00000000-0000-0000-0000-000000000008"));
    var marcCustomSlice = new SliceImpl<>(List.of(marcCustomHolding), PageRequest.of(0, 1), false);
    var exportStatistic = new ExportStrategyStatistic(null);
    holdingsExportAllStrategy.setExportIdsBatch(1);
    holdingsExportAllStrategy.entityManager = entityManager;
    holdingsExportAllStrategy.folioExecutionContext = folioExecutionContext;
    holdingsExportAllStrategy.setInstanceEntityRepository(instanceEntityRepository);
    when(folioHoldingsAllRepository.findFolioHoldingsAllNonDeletedNonSuppressed(
        eq(fromId), eq(toId), any(Pageable.class)))
        .thenReturn(folioSlice);
    when(folioHoldingsAllRepository.findMarcHoldingsAllNonDeletedNonSuppressedCustomHoldingsProfile(
        eq(fromId), eq(toId), any(Pageable.class)))
        .thenReturn(marcCustomSlice);
    var spyStrategy = org.mockito.Mockito.spy(holdingsExportAllStrategy);
    doNothing().when(spyStrategy).createAndSaveGeneratedMarc(any(), any(), any());
    // When
    spyStrategy.processSlices(
        exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    // Then
    verify(folioHoldingsAllRepository)
        .findFolioHoldingsAllNonDeletedNonSuppressed(fromId, toId, PageRequest.of(0, 1));
    verify(folioHoldingsAllRepository)
        .findMarcHoldingsAllNonDeletedNonSuppressedCustomHoldingsProfile(fromId, toId, PageRequest.of(0, 1));
    verify(marcHoldingsAllRepository, never())
        .findMarcHoldingsAllNonDeletedNonSuppressed(any(), any(), any());
    verify(folioHoldingsAllRepository, never()).findFolioHoldingsAllDeleted();
    // entityManager.clear() is called 4 times:
    // 1. in processFolioSlices
    // 2. in getHoldingsWithInstanceAndItems (called by processFolioHoldings inside processFolioSlices)
    // 3. in processMarcHoldingsSlices
    // 4. in getHoldingsWithInstanceAndItems (called by processFolioHoldings inside processMarcHoldingsSlices)
    verify(entityManager, times(4)).clear();
    verify(spyStrategy, times(2)).createAndSaveGeneratedMarc(any(), any(), any());
  }

    @ParameterizedTest
  @CsvSource({
      "true, true, 1",
      "true, false, 0",
      "false, true, 0",
      "false, false, 0"
  })
  void processSlicesShouldHandleDeletedRecordsOnlyOnLastExport(boolean deletedRecords, boolean lastExport, int expectedDeletedCalls) {
    // TestMate-f577e751ee8ca9d5ec7fb9ddcc3899c6
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var fromId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var toId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    
    var exportFilesEntity = new JobExecutionExportFilesEntity()
        .withJobExecutionId(jobExecutionId)
        .withFromId(fromId)
        .withToId(toId);
    
    var exportRequest = new ExportRequest()
        .deletedRecords(deletedRecords)
        .lastExport(lastExport)
        .suppressedFromDiscovery(false);
    
    var mappingProfile = new MappingProfile();
    mappingProfile.setDefault(true);
    
    Slice<HoldingsRecordEntity> emptyFolioSlice = new SliceImpl<>(Collections.emptyList(), PageRequest.of(0, 1), false);
    Slice<MarcRecordEntity> emptyMarcSlice = new SliceImpl<>(Collections.emptyList(), PageRequest.of(0, 1), false);
    
    holdingsExportAllStrategy.setExportIdsBatch(1);
    holdingsExportAllStrategy.entityManager = entityManager;
    holdingsExportAllStrategy.folioExecutionContext = folioExecutionContext;
    holdingsExportAllStrategy.setInstanceEntityRepository(instanceEntityRepository);
    when(folioHoldingsAllRepository.findFolioHoldingsAllNonDeletedNonSuppressed(eq(fromId), eq(toId), any(Pageable.class)))
        .thenReturn(emptyFolioSlice);
    when(marcHoldingsAllRepository.findMarcHoldingsAllNonDeletedNonSuppressed(eq(fromId), eq(toId), any(Pageable.class)))
        .thenReturn(emptyMarcSlice);
    var spyStrategy = org.mockito.Mockito.spy(holdingsExportAllStrategy);
    
    doNothing().when(spyStrategy).createAndSaveGeneratedMarc(any(), any(), any());
    doNothing().when(spyStrategy).createAndSaveMarcFromJsonRecord(any(), any(), any(), any(), any(), any(), any());
    var exportStatistic = new ExportStrategyStatistic(null);
    
    // When
    spyStrategy.processSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    
    // Then
    if (expectedDeletedCalls > 0) {
      verify(folioHoldingsAllRepository, times(1)).findFolioHoldingsAllDeletedNonSuppressed();
      verify(marcHoldingsAllRepository, times(1)).findMarcHoldingsAllDeletedNonSuppressed();
    } else {
      verify(folioHoldingsAllRepository, never()).findFolioHoldingsAllDeletedNonSuppressed();
      verify(marcHoldingsAllRepository, never()).findMarcHoldingsAllDeletedNonSuppressed();
    }
    
    // entityManager.clear() is called:
    // 1. processFolioSlices (direct call)
    // 2. getHoldingsWithInstanceAndItems (inside processFolioHoldings called by processFolioSlices)
    // 3. processMarcSlices (direct call because mappingProfile is default)
    int standardClears = 3; 
    
    // If handleDeleted is called:
    // 4. handleDeleted (direct call before processFolioHoldings)
    // 5. getHoldingsWithInstanceAndItems (inside processFolioHoldings for deleted folio)
    // 6. handleDeleted (direct call before processMarcHoldings)
    int deletedClears = expectedDeletedCalls > 0 ? 3 : 0;
    
    verify(entityManager, times(standardClears + deletedClears)).clear();
  }

    @ParameterizedTest
  @CsvSource({"true, true", "true, false", "false, true", "false, false"})
  @MockitoSettings(strictness = Strictness.LENIENT)
  void processSlicesShouldRespectDiscoverySuppressionFilterInAllSlices(
      boolean suppressedFromDiscovery, boolean isDefaultProfile) {
    // TestMate-ff6881d8344afc7e4780199898f6f31c
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var fromId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var toId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    var exportFilesEntity =
        new JobExecutionExportFilesEntity()
            .withJobExecutionId(jobExecutionId)
            .withFromId(fromId)
            .withToId(toId);
    var exportRequest =
        new ExportRequest()
            .deletedRecords(false)
            .lastExport(false)
            .suppressedFromDiscovery(suppressedFromDiscovery);
    var mappingProfile = new MappingProfile();
    mappingProfile.setDefault(isDefaultProfile);
    var folioHolding = new HoldingsRecordEntity();
    folioHolding.setId(UUID.randomUUID());
    folioHolding.setJsonb("{}");
    folioHolding.setInstanceId(UUID.randomUUID());
    var marcRecord = new MarcRecordEntity();
    marcRecord.setExternalId(UUID.randomUUID());
    Slice<HoldingsRecordEntity> folioSlice =
        new SliceImpl<>(List.of(folioHolding), PageRequest.of(0, 1), false);
    Slice<MarcRecordEntity> marcSlice =
        new SliceImpl<>(List.of(marcRecord), PageRequest.of(0, 1), false);
    Slice<HoldingsRecordEntity> customSlice =
        new SliceImpl<>(List.of(folioHolding), PageRequest.of(0, 1), false);
    holdingsExportAllStrategy.setExportIdsBatch(1);
    holdingsExportAllStrategy.entityManager = entityManager;
    holdingsExportAllStrategy.folioExecutionContext = folioExecutionContext;
    holdingsExportAllStrategy.setInstanceEntityRepository(instanceEntityRepository);
    if (suppressedFromDiscovery) {
      lenient()
          .when(
              folioHoldingsAllRepository.findFolioHoldingsAllNonDeleted(
                  eq(fromId), eq(toId), any(Pageable.class)))
          .thenReturn(folioSlice);
      if (isDefaultProfile) {
        lenient()
            .when(
                marcHoldingsAllRepository.findMarcHoldingsAllNonDeleted(
                    eq(fromId), eq(toId), any(Pageable.class)))
            .thenReturn(marcSlice);
      } else {
        lenient()
            .when(
                folioHoldingsAllRepository.findMarcHoldingsAllNonDeletedCustomHoldingsProfile(
                    eq(fromId), eq(toId), any(Pageable.class)))
            .thenReturn(customSlice);
      }
    } else {
      lenient()
          .when(
              folioHoldingsAllRepository.findFolioHoldingsAllNonDeletedNonSuppressed(
                  eq(fromId), eq(toId), any(Pageable.class)))
          .thenReturn(folioSlice);
      if (isDefaultProfile) {
        lenient()
            .when(
                marcHoldingsAllRepository.findMarcHoldingsAllNonDeletedNonSuppressed(
                    eq(fromId), eq(toId), any(Pageable.class)))
            .thenReturn(marcSlice);
      } else {
        lenient()
            .when(
                folioHoldingsAllRepository
                    .findMarcHoldingsAllNonDeletedNonSuppressedCustomHoldingsProfile(
                        eq(fromId), eq(toId), any(Pageable.class)))
            .thenReturn(customSlice);
      }
    }
    var spyStrategy = org.mockito.Mockito.spy(holdingsExportAllStrategy);
    doNothing().when(spyStrategy).createAndSaveGeneratedMarc(any(), any(), any());
    doNothing()
        .when(spyStrategy)
        .createAndSaveMarcFromJsonRecord(any(), any(), any(), any(), any(), any(), any());
    var exportStatistic = new ExportStrategyStatistic(null);
    // When
    spyStrategy.processSlices(
        exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    // Then
    if (suppressedFromDiscovery) {
      verify(folioHoldingsAllRepository)
          .findFolioHoldingsAllNonDeleted(fromId, toId, PageRequest.of(0, 1));
      verify(folioHoldingsAllRepository, never())
          .findFolioHoldingsAllNonDeletedNonSuppressed(any(), any(), any());
      if (isDefaultProfile) {
        verify(marcHoldingsAllRepository).findMarcHoldingsAllNonDeleted(fromId, toId, PageRequest.of(0, 1));
        verify(marcHoldingsAllRepository, never())
            .findMarcHoldingsAllNonDeletedNonSuppressed(any(), any(), any());
      } else {
        verify(folioHoldingsAllRepository)
            .findMarcHoldingsAllNonDeletedCustomHoldingsProfile(fromId, toId, PageRequest.of(0, 1));
        verify(folioHoldingsAllRepository, never())
            .findMarcHoldingsAllNonDeletedNonSuppressedCustomHoldingsProfile(any(), any(), any());
      }
    } else {
      verify(folioHoldingsAllRepository)
          .findFolioHoldingsAllNonDeletedNonSuppressed(fromId, toId, PageRequest.of(0, 1));
      verify(folioHoldingsAllRepository, never()).findFolioHoldingsAllDeleted();
      if (isDefaultProfile) {
        verify(marcHoldingsAllRepository)
            .findMarcHoldingsAllNonDeletedNonSuppressed(fromId, toId, PageRequest.of(0, 1));
        verify(marcHoldingsAllRepository, never()).findMarcHoldingsAllNonDeleted(any(), any(), any());
      } else {
        verify(folioHoldingsAllRepository)
            .findMarcHoldingsAllNonDeletedNonSuppressedCustomHoldingsProfile(
                fromId, toId, PageRequest.of(0, 1));
        verify(folioHoldingsAllRepository, never())
            .findMarcHoldingsAllNonDeletedCustomHoldingsProfile(any(), any(), any());
      }
    }
    int expectedClears = isDefaultProfile ? 3 : 4;
    verify(entityManager, times(expectedClears)).clear();
  }

    @Test
  void processSlicesShouldIterateThroughAllSlicesUntilExhausted() {
    // TestMate-80b6d2e0307b7e2b6a6d1e5651a81cd9
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var fromId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var toId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    var tenantId = "test-tenant";
    var exportFilesEntity = new JobExecutionExportFilesEntity()
        .withJobExecutionId(jobExecutionId)
        .withFromId(fromId)
        .withToId(toId);
    var exportRequest = new ExportRequest()
        .deletedRecords(false)
        .lastExport(false)
        .suppressedFromDiscovery(false);
    var mappingProfile = new MappingProfile();
    mappingProfile.setDefault(true);
    
    // Initialize entities with JSON content to avoid NPE in getAsJsonObject
    var instanceId = UUID.randomUUID();
    var h1 = new HoldingsRecordEntity();
    h1.setId(UUID.fromString("00000000-0000-0000-0000-000000000011"));
    h1.setJsonb("{}");
    h1.setInstanceId(instanceId);
    var h2 = new HoldingsRecordEntity();
    h2.setId(UUID.fromString("00000000-0000-0000-0000-000000000012"));
    h2.setJsonb("{}");
    h2.setInstanceId(instanceId);
    var h3 = new HoldingsRecordEntity();
    h3.setId(UUID.fromString("00000000-0000-0000-0000-000000000013"));
    h3.setJsonb("{}");
    h3.setInstanceId(instanceId);
    var h4 = new HoldingsRecordEntity();
    h4.setId(UUID.fromString("00000000-0000-0000-0000-000000000014"));
    h4.setJsonb("{}");
    h4.setInstanceId(instanceId);
    var h5 = new HoldingsRecordEntity();
    h5.setId(UUID.fromString("00000000-0000-0000-0000-000000000015"));
    h5.setJsonb("{}");
    h5.setInstanceId(instanceId);
    var slice0 = new SliceImpl<>(List.of(h1, h2), PageRequest.of(0, 2), true);
    var slice1 = new SliceImpl<>(List.of(h3, h4), PageRequest.of(1, 2), true);
    var slice2 = new SliceImpl<>(List.of(h5), PageRequest.of(2, 2), false);
    holdingsExportAllStrategy.setExportIdsBatch(2);
    holdingsExportAllStrategy.entityManager = entityManager;
    holdingsExportAllStrategy.folioExecutionContext = folioExecutionContext;
    holdingsExportAllStrategy.setInstanceEntityRepository(instanceEntityRepository);
    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(folioHoldingsAllRepository.findFolioHoldingsAllNonDeletedNonSuppressed(eq(fromId), eq(toId), eq(PageRequest.of(0, 2))))
        .thenReturn(slice0);
    when(folioHoldingsAllRepository.findFolioHoldingsAllNonDeletedNonSuppressed(eq(fromId), eq(toId), eq(PageRequest.of(1, 2))))
        .thenReturn(slice1);
    when(folioHoldingsAllRepository.findFolioHoldingsAllNonDeletedNonSuppressed(eq(fromId), eq(toId), eq(PageRequest.of(2, 2))))
        .thenReturn(slice2);
    
    // Stub MARC repository to return empty to isolate Folio slice testing
    when(marcHoldingsAllRepository.findMarcHoldingsAllNonDeletedNonSuppressed(eq(fromId), eq(toId), any(Pageable.class)))
        .thenReturn(new SliceImpl<>(Collections.emptyList(), PageRequest.of(0, 2), false));
    var spyStrategy = org.mockito.Mockito.spy(holdingsExportAllStrategy);
    doNothing().when(spyStrategy).createAndSaveGeneratedMarc(any(), any(), any());
    doNothing().when(spyStrategy).createAndSaveMarcFromJsonRecord(any(), any(), any(), any(), any(), any(), any());
    
    var exportStatistic = new ExportStrategyStatistic(null);
    // When
    spyStrategy.processSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    // Then
    verify(folioHoldingsAllRepository).findFolioHoldingsAllNonDeletedNonSuppressed(fromId, toId, PageRequest.of(0, 2));
    verify(folioHoldingsAllRepository).findFolioHoldingsAllNonDeletedNonSuppressed(fromId, toId, PageRequest.of(1, 2));
    verify(folioHoldingsAllRepository).findFolioHoldingsAllNonDeletedNonSuppressed(fromId, toId, PageRequest.of(2, 2));
    verify(spyStrategy, times(3)).createAndSaveGeneratedMarc(any(), any(), any());
    
    // entityManager.clear() is called:
    // 1. In processFolioSlices loop (3 slices = 3 calls)
    // 2. In getHoldingsWithInstanceAndItems (called by processFolioHoldings for each slice = 3 calls)
    // 3. In processMarcSlices (1 call for initial slice)
    // Total: 7 calls
    verify(entityManager, times(7)).clear();
  }

}
