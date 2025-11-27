package org.folio.dataexp.service.export.strategies;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.client.ConsortiumSearchClient;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.FolioHoldingsAllRepository;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.HoldingsRecordEntityTenantRepository;
import org.folio.dataexp.repository.InstanceCentralTenantRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.repository.MarcHoldingsAllRepository;
import org.folio.dataexp.repository.MarcInstanceRecordRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.dataexp.service.validators.PermissionsValidator;
import org.folio.processor.RuleProcessor;
import org.folio.spring.FolioModuleMetadata;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

/** Export strategy for exporting all Holdings records. */
@Log4j2
@Component
public class HoldingsExportAllStrategy extends HoldingsExportStrategy {

  private final FolioHoldingsAllRepository folioHoldingsAllRepository;
  private final MarcHoldingsAllRepository marcHoldingsAllRepository;

  /**
   * Constructs a new HoldingsExportAllStrategy with all required dependencies.
   *
   * @param itemEntityRepository Repository for item entities.
   * @param ruleFactory Factory for creating rules.
   * @param ruleProcessor Processor for applying rules.
   * @param ruleHandler Handler for rule processing.
   * @param referenceDataProvider Provider for reference data.
   * @param consortiaService Service for consortia operations.
   * @param consortiumSearchClient Client for consortium search.
   * @param holdingsRecordEntityTenantRepository Repository for tenant-specific holdings records.
   * @param marcInstanceRecordRepository Repository for MARC instance records.
   * @param instanceCentralTenantRepository Repository for central tenant instances.
   * @param folioModuleMetadata Metadata for the Folio module.
   * @param holdingsRecordEntityRepository Repository for holdings record entities.
   * @param marcRecordEntityRepository Repository for MARC record entities.
   * @param folioHoldingsAllRepository Repository for all Folio holdings.
   * @param marcHoldingsAllRepository Repository for all MARC holdings.
   * @param userService Service for user operations.
   * @param permissionsValidator Validator for permissions.
   */
  public HoldingsExportAllStrategy(
      ItemEntityRepository itemEntityRepository,
      RuleFactory ruleFactory,
      RuleProcessor ruleProcessor,
      RuleHandler ruleHandler,
      ReferenceDataProvider referenceDataProvider,
      ConsortiaService consortiaService,
      ConsortiumSearchClient consortiumSearchClient,
      HoldingsRecordEntityTenantRepository holdingsRecordEntityTenantRepository,
      MarcInstanceRecordRepository marcInstanceRecordRepository,
      InstanceCentralTenantRepository instanceCentralTenantRepository,
      FolioModuleMetadata folioModuleMetadata,
      HoldingsRecordEntityRepository holdingsRecordEntityRepository,
      MarcRecordEntityRepository marcRecordEntityRepository,
      FolioHoldingsAllRepository folioHoldingsAllRepository,
      MarcHoldingsAllRepository marcHoldingsAllRepository,
      UserService userService,
      PermissionsValidator permissionsValidator) {
    super(
        itemEntityRepository,
        ruleFactory,
        ruleProcessor,
        ruleHandler,
        referenceDataProvider,
        consortiaService,
        consortiumSearchClient,
        holdingsRecordEntityTenantRepository,
        marcInstanceRecordRepository,
        instanceCentralTenantRepository,
        folioModuleMetadata,
        userService,
        holdingsRecordEntityRepository,
        marcRecordEntityRepository,
        permissionsValidator);
    this.folioHoldingsAllRepository = folioHoldingsAllRepository;
    this.marcHoldingsAllRepository = marcHoldingsAllRepository;
  }

  /** Processes slices for Holdings export. */
  @Override
  protected void processSlices(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      ExportRequest exportRequest,
      LocalStorageWriter localStorageWriter) {
    processFolioSlices(
        exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    if (Boolean.TRUE.equals(mappingProfile.getDefault())) {
      processMarcSlices(
          exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    } else {
      processMarcHoldingsSlices(
          exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    }
    if (Boolean.TRUE.equals(exportRequest.getDeletedRecords())
        && Boolean.TRUE.equals(exportRequest.getLastExport())) {
      handleDeleted(
          exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    }
  }

  private void handleDeleted(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      ExportRequest exportRequest,
      LocalStorageWriter localStorageWriter) {
    var deletedFolioHoldings = getFolioDeleted(exportRequest);
    entityManager.clear();
    processFolioHoldings(
        exportFilesEntity,
        exportStatistic,
        mappingProfile,
        deletedFolioHoldings,
        localStorageWriter);
    if (Boolean.TRUE.equals(mappingProfile.getDefault())) {
      var deletedMarcHoldings = getMarcDeleted(exportRequest);
      entityManager.clear();
      processMarcHoldings(
          exportFilesEntity,
          exportStatistic,
          mappingProfile,
          deletedMarcHoldings,
          localStorageWriter);
    } else {
      var deletedFolioMarcHoldings = getMarcHoldingsDeleted(exportRequest);
      entityManager.clear();
      processFolioHoldings(
          exportFilesEntity,
          exportStatistic,
          mappingProfile,
          deletedFolioMarcHoldings,
          localStorageWriter);
    }
  }

  private void processFolioSlices(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      ExportRequest exportRequest,
      LocalStorageWriter localStorageWriter) {
    var folioSlice =
        nextFolioSlice(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
    entityManager.clear();
    processFolioHoldings(
        exportFilesEntity,
        exportStatistic,
        mappingProfile,
        folioSlice.getContent(),
        localStorageWriter);
    log.info("Slice size for holdings export all folio: {}", folioSlice.getContent().size());
    while (folioSlice.hasNext()) {
      folioSlice = nextFolioSlice(exportFilesEntity, exportRequest, folioSlice.nextPageable());
      entityManager.clear();
      processFolioHoldings(
          exportFilesEntity,
          exportStatistic,
          mappingProfile,
          folioSlice.getContent(),
          localStorageWriter);
    }
  }

  private void processMarcSlices(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      ExportRequest exportRequest,
      LocalStorageWriter localStorageWriter) {
    if (Boolean.TRUE.equals(mappingProfile.getDefault())) {
      var marcSlice =
          nextMarcSlice(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
      entityManager.clear();
      processMarcHoldings(
          exportFilesEntity,
          exportStatistic,
          mappingProfile,
          marcSlice.getContent(),
          localStorageWriter);
      log.info("Slice size for holdings export all marc: {}", marcSlice.getContent().size());
      while (marcSlice.hasNext()) {
        marcSlice = nextMarcSlice(exportFilesEntity, exportRequest, marcSlice.nextPageable());
        entityManager.clear();
        processMarcHoldings(
            exportFilesEntity,
            exportStatistic,
            mappingProfile,
            marcSlice.getContent(),
            localStorageWriter);
      }
    }
  }

  private void processMarcHoldingsSlices(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      ExportRequest exportRequest,
      LocalStorageWriter localStorageWriter) {
    var marcHoldingsSlice =
        nextMarcHoldingsSlice(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
    entityManager.clear();
    processFolioHoldings(
        exportFilesEntity,
        exportStatistic,
        mappingProfile,
        marcHoldingsSlice.getContent(),
        localStorageWriter);
    log.info("Slice size for holdings export all marc: {}", marcHoldingsSlice.getContent().size());
    while (marcHoldingsSlice.hasNext()) {
      marcHoldingsSlice =
          nextMarcHoldingsSlice(exportFilesEntity, exportRequest, marcHoldingsSlice.nextPageable());
      entityManager.clear();
      processFolioHoldings(
          exportFilesEntity,
          exportStatistic,
          mappingProfile,
          marcHoldingsSlice.getContent(),
          localStorageWriter);
    }
  }

  private void processMarcHoldings(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      List<MarcRecordEntity> marcRecords,
      LocalStorageWriter localStorageWriter) {
    var externalIds =
        marcRecords.stream().map(MarcRecordEntity::getExternalId).collect(Collectors.toSet());
    createAndSaveMarcFromJsonRecord(
        externalIds,
        exportStatistic,
        mappingProfile,
        exportFilesEntity.getJobExecutionId(),
        new HashSet<>(),
        marcRecords,
        localStorageWriter);
  }

  private void processFolioHoldings(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      List<HoldingsRecordEntity> folioHoldings,
      LocalStorageWriter localStorageWriter) {
    var result =
        getGeneratedMarc(folioHoldings, mappingProfile, exportFilesEntity.getJobExecutionId());
    createAndSaveGeneratedMarc(result, exportStatistic, localStorageWriter);
  }

  private Slice<HoldingsRecordEntity> nextFolioSlice(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportRequest exportRequest,
      Pageable pageble) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return folioHoldingsAllRepository.findFolioHoldingsAllNonDeleted(
          exportFilesEntity.getFromId(), exportFilesEntity.getToId(), pageble);
    }
    return folioHoldingsAllRepository.findFolioHoldingsAllNonDeletedNonSuppressed(
        exportFilesEntity.getFromId(), exportFilesEntity.getToId(), pageble);
  }

  private Slice<MarcRecordEntity> nextMarcSlice(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportRequest exportRequest,
      Pageable pageble) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return marcHoldingsAllRepository.findMarcHoldingsAllNonDeleted(
          exportFilesEntity.getFromId(), exportFilesEntity.getToId(), pageble);
    }
    return marcHoldingsAllRepository.findMarcHoldingsAllNonDeletedNonSuppressed(
        exportFilesEntity.getFromId(), exportFilesEntity.getToId(), pageble);
  }

  private Slice<HoldingsRecordEntity> nextMarcHoldingsSlice(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportRequest exportRequest,
      Pageable pageble) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return folioHoldingsAllRepository.findMarcHoldingsAllNonDeletedCustomHoldingsProfile(
          exportFilesEntity.getFromId(), exportFilesEntity.getToId(), pageble);
    }
    return folioHoldingsAllRepository
        .findMarcHoldingsAllNonDeletedNonSuppressedCustomHoldingsProfile(
            exportFilesEntity.getFromId(), exportFilesEntity.getToId(), pageble);
  }

  private List<HoldingsRecordEntity> getFolioDeleted(ExportRequest exportRequest) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return folioHoldingsAllRepository.findFolioHoldingsAllDeleted();
    }
    return folioHoldingsAllRepository.findFolioHoldingsAllDeletedNonSuppressed();
  }

  private List<MarcRecordEntity> getMarcDeleted(ExportRequest exportRequest) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return marcHoldingsAllRepository.findMarcHoldingsAllDeleted();
    }
    return marcHoldingsAllRepository.findMarcHoldingsAllDeletedNonSuppressed();
  }

  private List<HoldingsRecordEntity> getMarcHoldingsDeleted(ExportRequest exportRequest) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return folioHoldingsAllRepository.findMarcHoldingsAllDeletedCustomHoldingsProfile();
    }
    return folioHoldingsAllRepository
        .findMarcHoldingsAllDeletedNonSuppressedCustomHoldingsProfile();
  }

  private GeneratedMarcResult getGeneratedMarc(
      List<HoldingsRecordEntity> holdings, MappingProfile mappingProfile, UUID jobExecutionId) {
    var result = new GeneratedMarcResult(jobExecutionId);
    var instancesIds =
        holdings.stream().map(HoldingsRecordEntity::getInstanceId).collect(Collectors.toSet());
    var holdingsIds =
        holdings.stream().map(HoldingsRecordEntity::getId).collect(Collectors.toSet());
    var holdingsWithInstanceAndItems =
        getHoldingsWithInstanceAndItems(
            holdingsIds, result, mappingProfile, holdings, instancesIds);
    return getGeneratedMarc(mappingProfile, holdingsWithInstanceAndItems, jobExecutionId, result);
  }
}
