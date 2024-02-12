package org.folio.dataexp.service.export.strategies;

import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.FolioHoldingsAllRepository;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.repository.MarcHoldingsAllRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.processor.RuleProcessor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Component
public class HoldingsExportAllStrategy extends HoldingsExportStrategy {

  private final FolioHoldingsAllRepository folioHoldingsAllRepository;
  private final MarcHoldingsAllRepository marcHoldingsAllRepository;

  public HoldingsExportAllStrategy(InstanceEntityRepository instanceEntityRepository, ItemEntityRepository itemEntityRepository,
      RuleFactory ruleFactory, RuleProcessor ruleProcessor, RuleHandler ruleHandler, ReferenceDataProvider referenceDataProvider,
      ErrorLogService errorLogService, HoldingsRecordEntityRepository holdingsRecordEntityRepository,
      MarcRecordEntityRepository marcRecordEntityRepository, FolioHoldingsAllRepository folioHoldingsAllRepository,
      MarcHoldingsAllRepository marcHoldingsAllRepository) {
    super(instanceEntityRepository, itemEntityRepository, ruleFactory, ruleProcessor, ruleHandler, referenceDataProvider,
        errorLogService, holdingsRecordEntityRepository, marcRecordEntityRepository);
    this.folioHoldingsAllRepository = folioHoldingsAllRepository;
    this.marcHoldingsAllRepository = marcHoldingsAllRepository;
  }

  @Override
  protected void processSlices(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile, ExportRequest exportRequest) {
    processFolioSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest);
    processMarcSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest);
    if (Boolean.TRUE.equals(exportRequest.getDeletedRecords()) && exportRequest.getLastExport()) {
      handleDeleted(exportFilesEntity, exportStatistic, mappingProfile, exportRequest);
    }
  }

  private void handleDeleted(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      ExportRequest exportRequest) {
    var deletedFolioHoldings = getFolioDeleted(exportRequest);
    entityManager.clear();
    processFolioHoldingsDeleted(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, deletedFolioHoldings);
    var deletedMarcHoldings = getMarcDeleted(exportRequest);
    entityManager.clear();
    processMarcHoldings(exportFilesEntity, exportStatistic, mappingProfile, deletedMarcHoldings);
  }

  private void processFolioSlices(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      ExportRequest exportRequest) {
    var folioSlice = nextFolioSlice(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
    entityManager.clear();
    processFolioHoldings(exportFilesEntity, exportStatistic, mappingProfile, folioSlice.getContent());
    log.info("Slice size for holdings export all folio: {}", folioSlice.getContent().size());
    while (folioSlice.hasNext()) {
      folioSlice = nextFolioSlice(exportFilesEntity, exportRequest, folioSlice.nextPageable());
      entityManager.clear();
      processFolioHoldings(exportFilesEntity, exportStatistic, mappingProfile, folioSlice.getContent());
    }
  }

  private void processMarcSlices(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      ExportRequest exportRequest) {
    if (Boolean.TRUE.equals(mappingProfile.getDefault())) {
      var marcSlice = nextMarcSlice(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
      processMarcHoldings(exportFilesEntity, exportStatistic, mappingProfile, marcSlice.getContent());
      log.info("Slice size for holdings export all marc: {}", marcSlice.getContent().size());
      while (marcSlice.hasNext()) {
        marcSlice = nextMarcSlice(exportFilesEntity, exportRequest, marcSlice.nextPageable());
        processMarcHoldings(exportFilesEntity, exportStatistic, mappingProfile, marcSlice.getContent());
      }
    }
  }

  private void processMarcHoldings(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      List<MarcRecordEntity> marcRecords) {
    var externalIds = marcRecords.stream().map(MarcRecordEntity::getExternalId).collect(Collectors.toSet());
    createMarc(externalIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(), new HashSet<>(),
        marcRecords);
  }

  private void processFolioHoldings(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      List<HoldingsRecordEntity> folioHoldings) {
    var result = getGeneratedMarc(folioHoldings, mappingProfile, exportFilesEntity.getJobExecutionId());
    saveMarc(result, exportStatistic);
  }

  private void processFolioHoldingsDeleted(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile, ExportRequest exportRequest, List<HoldingsRecordEntity> folioHoldings) {
    var result = getGeneratedMarc(folioHoldings, mappingProfile, exportFilesEntity.getJobExecutionId());
    saveMarc(result, exportStatistic);
  }

  private Slice<HoldingsRecordEntity> nextFolioSlice(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, Pageable pageble) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return folioHoldingsAllRepository.findFolioHoldingsAllNonDeleted(exportFilesEntity.getFromId(), exportFilesEntity.getToId(),
          pageble);
    }
    return folioHoldingsAllRepository.findFolioHoldingsAllNonDeletedNonSuppressed(exportFilesEntity.getFromId(),
        exportFilesEntity.getToId(), pageble);
  }

  private Slice<MarcRecordEntity> nextMarcSlice(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, Pageable pageble) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return marcHoldingsAllRepository.findMarcHoldingsAllNonDeleted(exportFilesEntity.getFromId(), exportFilesEntity.getToId(),
          pageble);
    }
    return marcHoldingsAllRepository.findMarcHoldingsAllNonDeletedNonSuppressed(exportFilesEntity.getFromId(), exportFilesEntity.getToId(),
        pageble);
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

  private GeneratedMarcResult getGeneratedMarc(List<HoldingsRecordEntity> holdings, MappingProfile mappingProfile,
       UUID jobExecutionId) {
    var result = new GeneratedMarcResult();
    var instancesIds = holdings.stream().map(HoldingsRecordEntity::getInstanceId).collect(Collectors.toSet());
    var holdingsIds = holdings.stream().map(HoldingsRecordEntity::getId).collect(Collectors.toSet());
    var holdingsWithInstanceAndItems = getHoldingsWithInstanceAndItems(holdingsIds, result, mappingProfile, holdings, instancesIds);
    return getGeneratedMarc(mappingProfile, holdingsWithInstanceAndItems, jobExecutionId, result);
  }
}
