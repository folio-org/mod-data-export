package org.folio.dataexp.service.export.strategies;

import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.FolioInstanceAllRepository;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceCentralTenantRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.InstanceWithHridEntityRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.repository.MarcInstanceAllRepository;
import org.folio.dataexp.repository.MarcInstanceRecordRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
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
public class InstancesExportAllStrategy extends InstancesExportStrategy {

  private final FolioInstanceAllRepository folioInstanceAllRepository;
  private final MarcInstanceAllRepository marcInstanceAllRepository;

  public InstancesExportAllStrategy(ConsortiaService consortiaService,
      InstanceCentralTenantRepository instanceCentralTenantRepository, MarcInstanceRecordRepository marcInstanceRecordRepository,
      HoldingsRecordEntityRepository holdingsRecordEntityRepository, ItemEntityRepository itemEntityRepository,
      RuleFactory ruleFactory, RuleHandler ruleHandler, RuleProcessor ruleProcessor, ReferenceDataProvider referenceDataProvider,
      MappingProfileEntityRepository mappingProfileEntityRepository,
      InstanceWithHridEntityRepository instanceWithHridEntityRepository, ErrorLogService errorLogService,
      MarcRecordEntityRepository marcRecordEntityRepository, InstanceEntityRepository instanceEntityRepository,
      FolioInstanceAllRepository folioInstanceAllRepository,
      MarcInstanceAllRepository marcInstanceAllRepository) {
    super(consortiaService, instanceCentralTenantRepository, marcInstanceRecordRepository, holdingsRecordEntityRepository,
        itemEntityRepository, ruleFactory, ruleHandler, ruleProcessor, referenceDataProvider, mappingProfileEntityRepository,
        instanceWithHridEntityRepository, errorLogService, marcRecordEntityRepository, instanceEntityRepository);
    this.folioInstanceAllRepository = folioInstanceAllRepository;
    this.marcInstanceAllRepository = marcInstanceAllRepository;
  }

  @Override
  protected void processSlices(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      ExportRequest exportRequest) {
    processFolioSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest);
    if (Boolean.TRUE.equals(mappingProfile.getDefault()) || mappingProfile.getRecordTypes().contains(RecordTypes.SRS)) {
      processMarcSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest);
    }
    if (Boolean.TRUE.equals(exportRequest.getDeletedRecords()) && Boolean.TRUE.equals(exportRequest.getLastExport())) {
      handleDeleted(exportFilesEntity, exportStatistic, mappingProfile, exportRequest);
    }
  }

  private void handleDeleted(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      ExportRequest exportRequest) {
    var deletedFolioInstances = getFolioDeleted(exportRequest);
    entityManager.clear();
    processFolioInstances(exportFilesEntity, exportStatistic, mappingProfile, deletedFolioInstances);
    var deletedMarcInstances = getMarcDeleted(exportRequest);
    entityManager.clear();
    processMarcInstances(exportFilesEntity, exportStatistic, mappingProfile, deletedMarcInstances);
  }

  private void processFolioSlices(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      ExportRequest exportRequest) {
    var folioSlice = nextFolioSlice(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
    entityManager.clear();
    processFolioInstances(exportFilesEntity, exportStatistic, mappingProfile, folioSlice.getContent());
    log.info("Slice size for instances export all folio: {}", folioSlice.getContent().size());
    while (folioSlice.hasNext()) {
      folioSlice = nextFolioSlice(exportFilesEntity, exportRequest, folioSlice.nextPageable());
      entityManager.clear();
      processFolioInstances(exportFilesEntity, exportStatistic, mappingProfile, folioSlice.getContent());
    }
  }

  private void processMarcSlices(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile, ExportRequest exportRequest) {
    var marcSlice = nextMarcSlice(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
    entityManager.clear();
    processMarcInstances(exportFilesEntity, exportStatistic, mappingProfile, marcSlice.getContent());
    log.info("Slice size for instances export all marc: {}", marcSlice.getContent().size());
    while (marcSlice.hasNext()) {
      marcSlice = nextMarcSlice(exportFilesEntity, exportRequest, marcSlice.nextPageable());
      entityManager.clear();
      processMarcInstances(exportFilesEntity, exportStatistic, mappingProfile, marcSlice.getContent());
    }
  }

  private void processMarcInstances(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      List<MarcRecordEntity> marcRecords) {
    var externalIds = marcRecords.stream().map(MarcRecordEntity::getExternalId).collect(Collectors.toSet());
    createMarc(externalIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(), new HashSet<>(),
        marcRecords);
  }

  private void processFolioInstances(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      List<InstanceEntity> folioInstances) {
    var result = getGeneratedMarc(folioInstances, mappingProfile, exportFilesEntity.getJobExecutionId());
    saveMarc(result, exportStatistic);
  }

  private Slice<InstanceEntity> nextFolioSlice(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, Pageable pageble) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return folioInstanceAllRepository.findFolioInstanceAllNonDeleted(exportFilesEntity.getFromId(), exportFilesEntity.getToId(),
          pageble);
    }
    return folioInstanceAllRepository.findFolioInstanceAllNonDeletedNonSuppressed(exportFilesEntity.getFromId(),
        exportFilesEntity.getToId(), pageble);
  }

  private Slice<MarcRecordEntity> nextMarcSlice(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, Pageable pageble) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return marcInstanceAllRepository.findMarcInstanceAllNonDeleted(exportFilesEntity.getFromId(), exportFilesEntity.getToId(),
          pageble);
    }
    return  marcInstanceAllRepository.findMarcInstanceAllNonDeletedNonSuppressed(exportFilesEntity.getFromId(), exportFilesEntity.getToId(),
        pageble);
  }

  private List<InstanceEntity> getFolioDeleted(ExportRequest exportRequest) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return folioInstanceAllRepository.findFolioInstanceAllDeleted();
    }
    return folioInstanceAllRepository.findFolioInstanceAllDeletedNonSuppressed();
  }

  private List<MarcRecordEntity> getMarcDeleted(ExportRequest exportRequest) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return marcInstanceAllRepository.findMarcInstanceAllDeleted();
    }
    return marcInstanceAllRepository.findMarcInstanceAllDeletedNonSuppressed();
  }

  private GeneratedMarcResult getGeneratedMarc(List<InstanceEntity> listFolioInstances, MappingProfile mappingProfile,
      UUID jobExecutionId) {
    var generatedMarcResult = new GeneratedMarcResult();
    var instancesIds = listFolioInstances.stream().map(InstanceEntity::getId).collect(Collectors.toSet());
    var instancesWithHoldingsAndItems = getInstancesWithHoldingsAndItems(instancesIds, generatedMarcResult, mappingProfile, listFolioInstances);
    return getGeneratedMarc(generatedMarcResult, instancesWithHoldingsAndItems, mappingProfile, jobExecutionId);
  }
}
