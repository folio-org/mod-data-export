package org.folio.dataexp.service.export.strategies;

import jakarta.persistence.EntityManager;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONObject;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.InstanceDeletedEntity;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceCentralTenantRepository;
import org.folio.dataexp.repository.InstanceEntityDeletedRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.InstanceWithHridEntityRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.repository.MarcInstanceRecordRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.dataexp.service.export.Constants;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.processor.RuleProcessor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Component
public class InstancesExportAllStrategy extends InstancesExportStrategy {

  private final InstanceEntityDeletedRepository instanceEntityDeletedRepository;

  public InstancesExportAllStrategy(ConsortiaService consortiaService,
      InstanceCentralTenantRepository instanceCentralTenantRepository, MarcInstanceRecordRepository marcInstanceRecordRepository,
      HoldingsRecordEntityRepository holdingsRecordEntityRepository, ItemEntityRepository itemEntityRepository,
      RuleFactory ruleFactory, RuleHandler ruleHandler, RuleProcessor ruleProcessor, ReferenceDataProvider referenceDataProvider,
      MappingProfileEntityRepository mappingProfileEntityRepository,
      InstanceWithHridEntityRepository instanceWithHridEntityRepository, ErrorLogService errorLogService,
      MarcRecordEntityRepository marcRecordEntityRepository, InstanceEntityRepository instanceEntityRepository,
      EntityManager entityManager, InstanceEntityDeletedRepository instanceEntityDeletedRepository) {
    super(consortiaService, instanceCentralTenantRepository, marcInstanceRecordRepository, holdingsRecordEntityRepository,
        itemEntityRepository, ruleFactory, ruleHandler, ruleProcessor, referenceDataProvider, mappingProfileEntityRepository,
        instanceWithHridEntityRepository, errorLogService, marcRecordEntityRepository, instanceEntityRepository, entityManager);
    this.instanceEntityDeletedRepository = instanceEntityDeletedRepository;
  }

  @Override
  public List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds, MappingProfile mappingProfile, ExportRequest exportRequest) {
    if (Boolean.TRUE.equals(mappingProfile.getDefault()) || mappingProfile.getRecordTypes().contains(RecordTypes.SRS)) {
      if (Boolean.TRUE.equals(exportRequest.getDeletedRecords())) {
        if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
          return marcRecordEntityRepository.findByExternalIdInAndRecordTypeIs(externalIds, INSTANCE_MARC_TYPE);
        }
        return marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndSuppressDiscoveryIs(externalIds, INSTANCE_MARC_TYPE,
          false);
      }
      if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
        return marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndStateIsAndLeaderRecordStatusNot (externalIds,
          INSTANCE_MARC_TYPE, "ACTUAL", 'd');
      }
      return marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndStateIsAndLeaderRecordStatusNotAndSuppressDiscoveryIs(
        externalIds, INSTANCE_MARC_TYPE, "ACTUAL", 'd', false);
    }
    return new ArrayList<>();
  }

  @Override
  protected void processSlices(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile, ExportRequest exportRequest) {
    var slice = chooseSlice(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
    updateSliceState(slice, exportRequest);
    log.info("Slice size for instances export all: {}", slice.getSize());
    var exportIds = slice.getContent().stream().map(InstanceEntity::getId).collect(Collectors.toSet());
    var instances = slice.getContent().stream().collect(Collectors.toList());
    log.info("Size of exportIds for instances export all: {}", exportIds.size());
    createAndSaveMarc(exportIds, instances, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
      exportRequest);
    while (slice.hasNext()) {
      slice = chooseSlice(exportFilesEntity, exportRequest, slice.nextPageable());
      updateSliceState(slice, exportRequest);
      exportIds = slice.getContent().stream().map(InstanceEntity::getId).collect(Collectors.toSet());
      instances = slice.getContent().stream().collect(Collectors.toList());
      createAndSaveMarc(exportIds, instances, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
        exportRequest);
    }
  }

  private Slice<InstanceEntity> chooseSlice(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, Pageable pageble) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      if (Boolean.FALSE.equals(exportRequest.getDeletedRecords())) {
        var deletedMarcIds = marcRecordEntityRepository.getUUIDsOfDeletedMarcRecords();
        log.info("instance export all, deletedMarcIds: {}", deletedMarcIds);
        if (!deletedMarcIds.isEmpty()) {
          return instanceEntityRepository.findByIdGreaterThanEqualAndIdLessThanEqualAndIdNotInOrderByIdAsc(
            exportFilesEntity.getFromId(), exportFilesEntity.getToId(), deletedMarcIds, pageble);
        }
      }
      return instanceEntityRepository.findByIdGreaterThanEqualAndIdLessThanEqualOrderByIdAsc(exportFilesEntity.getFromId(),
        exportFilesEntity.getToId(), pageble);
    }
    if (Boolean.FALSE.equals(exportRequest.getDeletedRecords())) {
      var deletedMarcIds = marcRecordEntityRepository.getUUIDsOfDeletedAndNotSuppressedMarcRecords();
      log.info("instance export all, not suppressed deletedMarcIds: {}", deletedMarcIds);
      if (!deletedMarcIds.isEmpty()) {
        return instanceEntityRepository.findAllWhenSkipDiscoverySuppressedAndSkipDeletedMarc(exportFilesEntity.getFromId(),
          exportFilesEntity.getToId(), deletedMarcIds, pageble);
      }
    }
    return instanceEntityRepository.findAllWhenSkipDiscoverySuppressed(exportFilesEntity.getFromId(),
      exportFilesEntity.getToId(), pageble);
  }

  private List<InstanceEntity> instanceDeletedToInstanceEntities(List<InstanceDeletedEntity> instanceDeleted) {
    return instanceDeleted.stream()
      .map(hold -> new InstanceEntity().withId(UUID.fromString(getAsJsonObject(getAsJsonObject(hold.getJsonb()).get()
          .getAsString(Constants.DELETED_AUDIT_RECORD)).get()
          .getAsString("id")))
        .withJsonb(getAsJsonObject(hold.getJsonb()).get()
          .getAsString(Constants.DELETED_AUDIT_RECORD)))
      .toList();
  }

  private void createAndSaveMarc(Set<UUID> externalIds, List<InstanceEntity> instances, ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile, UUID jobExecutionId, ExportRequest exportRequest) {
    var externalIdsWithMarcRecord = new HashSet<UUID>();
    createMarc(externalIds, exportStatistic, mappingProfile, jobExecutionId, exportRequest, externalIdsWithMarcRecord);
    instances.removeIf(inst -> externalIdsWithMarcRecord.contains(inst.getId()));
    var result = getGeneratedMarc(externalIds, instances, mappingProfile, exportRequest, jobExecutionId);
    saveMarc(result, exportStatistic);
  }

  private GeneratedMarcResult getGeneratedMarc(Set<UUID> instanceIds, List<InstanceEntity> instances, MappingProfile mappingProfile,
      ExportRequest exportRequest, UUID jobExecutionId) {
    var generatedMarcResult = new GeneratedMarcResult();
    var instancesWithHoldingsAndItems = getInstancesWithHoldingsAndItems(instanceIds, instances, generatedMarcResult, mappingProfile, exportRequest);
    return getGeneratedMarc(generatedMarcResult, instancesWithHoldingsAndItems, mappingProfile, jobExecutionId);
  }

  private List<JSONObject> getInstancesWithHoldingsAndItems(Set<UUID> instancesIds, List<InstanceEntity> instances, GeneratedMarcResult generatedMarcResult,
      MappingProfile mappingProfile, ExportRequest exportRequest) {
    if (Boolean.TRUE.equals(exportRequest.getDeletedRecords()) && isExportCompleted(exportRequest)) {
      List<InstanceDeletedEntity> instanceDeleted;
      if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
        instanceDeleted = instanceEntityDeletedRepository.findAll();
      } else {
        instanceDeleted = instanceEntityDeletedRepository.findAllDeletedWhenSkipDiscoverySuppressed();
      }
      var instanceDeletedToInstanceEntities = instanceDeletedToInstanceEntities(instanceDeleted);
      instances.addAll(instanceDeletedToInstanceEntities);
    }
    return getInstancesWithHoldingsAndItems(instancesIds, generatedMarcResult, mappingProfile, instances);
  }
}
