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
import org.folio.dataexp.service.export.LocalStorageWriter;
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
      ExportRequest exportRequest, LocalStorageWriter localStorageWriter) {
    processFolioSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    if (Boolean.TRUE.equals(mappingProfile.getDefault()) || mappingProfile.getRecordTypes().contains(RecordTypes.SRS)) {
      processMarcSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    } else {
      processMarcInstanceSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    }
    if (Boolean.TRUE.equals(exportRequest.getDeletedRecords()) && Boolean.TRUE.equals(exportRequest.getLastExport())) {
      handleDeleted(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    }
  }

  private void handleDeleted(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      ExportRequest exportRequest, LocalStorageWriter localStorageWriter) {
    var deletedFolioInstances = getFolioDeleted(exportRequest);
    entityManager.clear();
    processFolioInstances(exportFilesEntity, exportStatistic, mappingProfile, deletedFolioInstances, localStorageWriter);
    if (Boolean.TRUE.equals(mappingProfile.getDefault()) || mappingProfile.getRecordTypes().contains(RecordTypes.SRS)) {
      var deletedMarcRecords = getMarcDeleted(exportRequest);
      entityManager.clear();
      processMarcInstances(exportFilesEntity, exportStatistic, mappingProfile, deletedMarcRecords, localStorageWriter);
    } else {
      var deletedMarcInstances = getMarcInstanceDeleted(exportRequest);
      entityManager.clear();
      processFolioInstances(exportFilesEntity, exportStatistic, mappingProfile, deletedMarcInstances, localStorageWriter);
    }
  }

  private void processFolioSlices(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      ExportRequest exportRequest, LocalStorageWriter localStorageWriter) {
    var folioSlice = nextFolioSlice(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
    entityManager.clear();
    processFolioInstances(exportFilesEntity, exportStatistic, mappingProfile, folioSlice.getContent(), localStorageWriter);
    log.info("Slice size for instances export all folio: {}", folioSlice.getContent().size());
    while (folioSlice.hasNext()) {
      folioSlice = nextFolioSlice(exportFilesEntity, exportRequest, folioSlice.nextPageable());
      entityManager.clear();
      processFolioInstances(exportFilesEntity, exportStatistic, mappingProfile, folioSlice.getContent(), localStorageWriter);
    }
  }

  private void processMarcSlices(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile, ExportRequest exportRequest, LocalStorageWriter localStorageWriter) {
    var marcSlice = nextMarcSlice(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
    entityManager.clear();
    processMarcInstances(exportFilesEntity, exportStatistic, mappingProfile, marcSlice.getContent(), localStorageWriter);
    log.info("Slice size for instances export all marc: {}", marcSlice.getContent().size());
    log.info("Slice content: {}", marcSlice.getContent().stream().map(MarcRecordEntity::getExternalId).toList());
    while (marcSlice.hasNext()) {
      marcSlice = nextMarcSlice(exportFilesEntity, exportRequest, marcSlice.nextPageable());
      entityManager.clear();
      processMarcInstances(exportFilesEntity, exportStatistic, mappingProfile, marcSlice.getContent(), localStorageWriter);
    }
  }

  private void processMarcInstanceSlices(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile, ExportRequest exportRequest, LocalStorageWriter localStorageWriter) {
    var marcInstanceSlice = nextMarcInstanceSlice(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
    entityManager.clear();
    processFolioInstances(exportFilesEntity, exportStatistic, mappingProfile, marcInstanceSlice.getContent(), localStorageWriter);
    log.info("Slice size for marc instances export all marc: {}", marcInstanceSlice.getContent().size());
    while (marcInstanceSlice.hasNext()) {
      marcInstanceSlice = nextMarcInstanceSlice(exportFilesEntity, exportRequest, marcInstanceSlice.nextPageable());
      entityManager.clear();
      processFolioInstances(exportFilesEntity, exportStatistic, mappingProfile, marcInstanceSlice.getContent(), localStorageWriter);
    }
  }

  private void processMarcInstances(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      List<MarcRecordEntity> marcRecords, LocalStorageWriter localStorageWriter) {
    var externalIds = marcRecords.stream().map(MarcRecordEntity::getExternalId).collect(Collectors.toSet());
    log.info("processMarcInstances instances all externalIds: {}", externalIds);
    createMarc(externalIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(), new HashSet<>(),
        marcRecords, localStorageWriter);
  }

  private void processFolioInstances(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      List<InstanceEntity> folioInstances, LocalStorageWriter localStorageWriter) {
    var result = getGeneratedMarc(folioInstances, mappingProfile, exportFilesEntity.getJobExecutionId());
    saveMarc(result, exportStatistic, localStorageWriter);
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

  private Slice<InstanceEntity> nextMarcInstanceSlice(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, Pageable pageble) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return folioInstanceAllRepository.findMarcInstanceAllNonDeletedCustomInstanceProfile(exportFilesEntity.getFromId(), exportFilesEntity.getToId(),
          pageble);
    }
    return  folioInstanceAllRepository.findMarcInstanceAllNonDeletedNonSuppressedForCustomInstanceProfile(exportFilesEntity.getFromId(), exportFilesEntity.getToId(),
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

  private List<InstanceEntity> getMarcInstanceDeleted(ExportRequest exportRequest) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return folioInstanceAllRepository.findMarcInstanceAllDeletedForCustomInstanceProfile();
    }
    return folioInstanceAllRepository.findMarcInstanceAllDeletedNonSuppressedCustomInstanceProfile();
  }

  private GeneratedMarcResult getGeneratedMarc(List<InstanceEntity> listFolioInstances, MappingProfile mappingProfile,
      UUID jobExecutionId) {
    var generatedMarcResult = new GeneratedMarcResult();
    var instancesIds = listFolioInstances.stream().map(InstanceEntity::getId).collect(Collectors.toSet());
    var instancesWithHoldingsAndItems = getInstancesWithHoldingsAndItems(instancesIds, generatedMarcResult, mappingProfile, listFolioInstances);
    return getGeneratedMarc(generatedMarcResult, instancesWithHoldingsAndItems, mappingProfile, jobExecutionId);
  }
}
