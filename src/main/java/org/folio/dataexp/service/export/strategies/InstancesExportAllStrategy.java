package org.folio.dataexp.service.export.strategies;

import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONObject;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.AuditInstanceEntityRepository;
import org.folio.dataexp.repository.FolioInstanceAllRepository;
import org.folio.dataexp.repository.InstanceCentralTenantRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.InstanceWithHridEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.repository.MarcInstanceAllRepository;
import org.folio.dataexp.repository.MarcInstanceRecordRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.dataexp.util.ErrorCode;
import org.folio.processor.RuleProcessor;
import org.folio.spring.FolioExecutionContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.folio.dataexp.service.export.Constants.DELETED_KEY;

@Log4j2
@Component
public class InstancesExportAllStrategy extends InstancesExportStrategy {

  private final FolioInstanceAllRepository folioInstanceAllRepository;
  private final MarcInstanceAllRepository marcInstanceAllRepository;
  private final AuditInstanceEntityRepository auditInstanceEntityRepository;

  public InstancesExportAllStrategy(ConsortiaService consortiaService,
      InstanceCentralTenantRepository instanceCentralTenantRepository, MarcInstanceRecordRepository marcInstanceRecordRepository,
      RuleFactory ruleFactory, RuleHandler ruleHandler, RuleProcessor ruleProcessor, ReferenceDataProvider referenceDataProvider,
      MappingProfileEntityRepository mappingProfileEntityRepository,
      InstanceWithHridEntityRepository instanceWithHridEntityRepository,
      MarcRecordEntityRepository marcRecordEntityRepository, InstanceEntityRepository instanceEntityRepository,
      FolioInstanceAllRepository folioInstanceAllRepository, HoldingsItemsResolverService holdingsItemsResolver,
      MarcInstanceAllRepository marcInstanceAllRepository, AuditInstanceEntityRepository auditInstanceEntityRepository) {
    super(consortiaService, instanceCentralTenantRepository, marcInstanceRecordRepository,
        ruleFactory, ruleHandler, ruleProcessor, referenceDataProvider, mappingProfileEntityRepository,
        instanceWithHridEntityRepository, holdingsItemsResolver, marcRecordEntityRepository, instanceEntityRepository);
    this.folioInstanceAllRepository = folioInstanceAllRepository;
    this.marcInstanceAllRepository = marcInstanceAllRepository;
    this.auditInstanceEntityRepository = auditInstanceEntityRepository;
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

  @Override
  public void setStatusBaseExportStatistic(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic) {
    if (exportStatistic.getFailed() == 0 && exportStatistic.getExported() >= 0) {
      exportFilesEntity.setStatus(JobExecutionExportFilesStatus.COMPLETED);
    }
    if (exportStatistic.getFailed() > 0 && exportStatistic.getExported() > 0) {
      exportFilesEntity.setStatus(JobExecutionExportFilesStatus.COMPLETED_WITH_ERRORS);
    }
    if (exportStatistic.getFailed() > 0 && exportStatistic.getExported() == 0) {
      exportFilesEntity.setStatus(JobExecutionExportFilesStatus.FAILED);
    }
  }

  @Override
  public Optional<ExportIdentifiersForDuplicateErrors> getIdentifiers(UUID id) {
    var identifiers = super.getIdentifiers(id);
    if (identifiers.isPresent() && Objects.isNull(identifiers.get().getAssociatedJsonObject())) {
      var auditInstances = auditInstanceEntityRepository.findByIdIn(Set.of(id));
      if (auditInstances.isEmpty()) {
        log.info("getIdentifiers:: not found for instance by id {}", id);
        return getDefaultIdentifiers(id);
      }
      var auditInstance = auditInstances.get(0);
      var exportIdentifiers = new ExportIdentifiersForDuplicateErrors();
      exportIdentifiers.setIdentifierHridMessage(auditInstance.getHrid());
      var instanceAssociatedJsonObject = new JSONObject();
      instanceAssociatedJsonObject.put(ErrorLogService.ID, auditInstance.getId());
      instanceAssociatedJsonObject.put(ErrorLogService.HRID, auditInstance.getHrid());
      instanceAssociatedJsonObject.put(ErrorLogService.TITLE, auditInstance.getTitle());
      exportIdentifiers.setAssociatedJsonObject(instanceAssociatedJsonObject);
      return Optional.of(exportIdentifiers);
    }
    return identifiers;
  }

  @Override
  public void saveConvertJsonRecordToMarcRecordError(MarcRecordEntity marcRecordEntity, UUID jobExecutionId, Exception e) {
    var instances = instanceEntityRepository.findByIdIn(Set.of(marcRecordEntity.getExternalId()));
    var errorMessage = e.getMessage();
    if (!instances.isEmpty() || !errorMessage.contains(LONG_MARC_RECORD_MESSAGE)) {
      super.saveConvertJsonRecordToMarcRecordError(marcRecordEntity, jobExecutionId, e);
    } else {
      var auditInstances = auditInstanceEntityRepository.findByIdIn(Set.of(marcRecordEntity.getExternalId()));
      if (!auditInstances.isEmpty()) {
        var auditInstance = auditInstances.get(0);
        var instanceAssociatedJsonObject = new JSONObject();
        instanceAssociatedJsonObject.put(ErrorLogService.ID, auditInstance.getId());
        instanceAssociatedJsonObject.put(ErrorLogService.HRID, auditInstance.getHrid());
        instanceAssociatedJsonObject.put(ErrorLogService.TITLE, auditInstance.getTitle());
        instanceAssociatedJsonObject.put(DELETED_KEY, true);
        errorLogService.saveWithAffectedRecord(instanceAssociatedJsonObject, e.getMessage(), ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(), jobExecutionId);
        log.error("Error converting record to marc " + marcRecordEntity.getExternalId() + " : " + e.getMessage());
        errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_DELETED_TOO_LONG_INSTANCE.getCode(), List.of(marcRecordEntity.getId().toString()), jobExecutionId);
        log.error(String.format(ErrorCode.ERROR_DELETED_TOO_LONG_INSTANCE.getDescription(), marcRecordEntity.getId()));
      } else {
        super.saveConvertJsonRecordToMarcRecordError(marcRecordEntity, jobExecutionId, e);
      }
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
    log.info("processMarcInstances instances all externalIds: {}", externalIds.size());
    createAndSaveMarcFromJsonRecord(externalIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(), new HashSet<>(),
        marcRecords, localStorageWriter);
  }

  private void processFolioInstances(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      List<InstanceEntity> folioInstances, LocalStorageWriter localStorageWriter) {
    var result = getGeneratedMarc(folioInstances, mappingProfile, exportFilesEntity.getJobExecutionId());
    createAndSaveGeneratedMarc(result, exportStatistic, localStorageWriter);
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
    List<InstanceEntity> result;
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      result = folioInstanceAllRepository.findFolioInstanceAllDeleted();
    } else {
      result = folioInstanceAllRepository.findFolioInstanceAllDeletedNonSuppressed();
    }
    result.forEach(del -> del.setDeleted(true));
    return result;
  }

  private List<MarcRecordEntity> getMarcDeleted(ExportRequest exportRequest) {
    List<MarcRecordEntity> result;
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      result = marcInstanceAllRepository.findMarcInstanceAllDeleted();
    } else {
      result = marcInstanceAllRepository.findMarcInstanceAllDeletedNonSuppressed();
    }
//    result.forEach(del -> del.setDeleted(true));
    return result;
  }

  private List<InstanceEntity> getMarcInstanceDeleted(ExportRequest exportRequest) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return folioInstanceAllRepository.findMarcInstanceAllDeletedForCustomInstanceProfile();
    }
    return folioInstanceAllRepository.findMarcInstanceAllDeletedNonSuppressedCustomInstanceProfile();
  }

  private GeneratedMarcResult getGeneratedMarc(List<InstanceEntity> listFolioInstances, MappingProfile mappingProfile,
      UUID jobExecutionId) {
    var generatedMarcResult = new GeneratedMarcResult(jobExecutionId);
    var instancesIds = listFolioInstances.stream().map(InstanceEntity::getId).collect(Collectors.toSet());
    var instancesWithHoldingsAndItems = getInstancesWithHoldingsAndItems(instancesIds, generatedMarcResult, mappingProfile, listFolioInstances);
    return getGeneratedMarc(generatedMarcResult, instancesWithHoldingsAndItems, mappingProfile, jobExecutionId);
  }
}
