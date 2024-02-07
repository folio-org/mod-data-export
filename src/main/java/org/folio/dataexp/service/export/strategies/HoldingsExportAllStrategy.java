package org.folio.dataexp.service.export.strategies;

import jakarta.persistence.EntityManager;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONObject;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.HoldingsRecordDeletedEntity;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.HoldingsRecordEntityDeletedRepository;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
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

import static org.folio.dataexp.service.export.Constants.DELETED_AUDIT_RECORD;

@Log4j2
@Component
public class HoldingsExportAllStrategy extends HoldingsExportStrategy {

  private final HoldingsRecordEntityDeletedRepository holdingsRecordEntityDeletedRepository;

  public HoldingsExportAllStrategy(InstanceEntityRepository instanceEntityRepository, ItemEntityRepository itemEntityRepository, RuleFactory ruleFactory, RuleProcessor ruleProcessor, RuleHandler ruleHandler, ReferenceDataProvider referenceDataProvider, ErrorLogService errorLogService, HoldingsRecordEntityRepository holdingsRecordEntityRepository, MarcRecordEntityRepository marcRecordEntityRepository, EntityManager entityManager, HoldingsRecordEntityDeletedRepository holdingsRecordEntityDeletedRepository) {
    super(instanceEntityRepository, itemEntityRepository, ruleFactory, ruleProcessor, ruleHandler, referenceDataProvider, errorLogService, holdingsRecordEntityRepository, marcRecordEntityRepository, entityManager);
    this.holdingsRecordEntityDeletedRepository = holdingsRecordEntityDeletedRepository;
  }

  @Override
  public List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds, MappingProfile mappingProfile, ExportRequest exportRequest) {
    if (Boolean.TRUE.equals(mappingProfile.getDefault())) {
      if (Boolean.TRUE.equals(exportRequest.getDeletedRecords())) {
        if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
          return marcRecordEntityRepository.findByExternalIdInAndRecordTypeIs(externalIds, HOLDING_MARC_TYPE);
        }
        return marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndSuppressDiscoveryIs(externalIds, HOLDING_MARC_TYPE,
          false);
      }
      if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
        return marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndStateIsAndLeaderRecordStatusNot (externalIds,
          HOLDING_MARC_TYPE, "ACTUAL", 'd');
      }
      return marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndStateIsAndLeaderRecordStatusNotAndSuppressDiscoveryIs(
        externalIds, HOLDING_MARC_TYPE, "ACTUAL", 'd', false);
    }
    return new ArrayList<>();
  }

  @Override
  protected void processSlices(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile, ExportRequest exportRequest) {
    var slice = chooseSlice(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
    updateSliceState(slice, exportRequest);
    log.info("Slice size for holdings export all: {}", slice.getSize());
    var exportIds = slice.getContent().stream().map(HoldingsRecordEntity::getId).collect(Collectors.toSet());
    var holdings = slice.getContent().stream().collect(Collectors.toList());
    log.info("Size of exportIds for holdings export all: {}", exportIds.size());
    createAndSaveMarc(exportIds, holdings, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
      exportRequest);
    while (slice.hasNext()) {
      slice = chooseSlice(exportFilesEntity, exportRequest, slice.nextPageable());
      updateSliceState(slice, exportRequest);
      exportIds = slice.getContent().stream().map(HoldingsRecordEntity::getId).collect(Collectors.toSet());
      holdings = slice.getContent().stream().collect(Collectors.toList());
      createAndSaveMarc(exportIds, holdings, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
        exportRequest);
    }
  }

  private Slice<HoldingsRecordEntity> chooseSlice(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, Pageable pageble) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      if (Boolean.FALSE.equals(exportRequest.getDeletedRecords())) {
        var deletedMarcIds = marcRecordEntityRepository.getUUIDsOfDeletedHoldingsMarcRecords();
        return holdingsRecordEntityRepository.findByIdGreaterThanEqualAndIdLessThanEqualAndIdNotInOrderByIdAsc(
          exportFilesEntity.getFromId(), exportFilesEntity.getToId(), deletedMarcIds, pageble);
      }
      return holdingsRecordEntityRepository.findByIdGreaterThanEqualAndIdLessThanEqualOrderByIdAsc(exportFilesEntity.getFromId(),
        exportFilesEntity.getToId(), pageble);
    }
    if (Boolean.FALSE.equals(exportRequest.getDeletedRecords())) {
      var deletedMarcIds = marcRecordEntityRepository.getUUIDsOfDeletedAndNotSuppressedHoldingsMarcRecords();
      return holdingsRecordEntityRepository.findAllWhenSkipDiscoverySuppressedAndSkipDeletedMarc(exportFilesEntity.getFromId(),
        exportFilesEntity.getToId(), deletedMarcIds, pageble
       );
    }
    return holdingsRecordEntityRepository.findAllWhenSkipDiscoverySuppressed(exportFilesEntity.getFromId(),
      exportFilesEntity.getToId(), pageble);
  }

  private List<HoldingsRecordEntity> holdingsDeletedToHoldingsEntities(List<HoldingsRecordDeletedEntity> holdingsDeleted) {
    return holdingsDeleted.stream()
      .map(hold -> new HoldingsRecordEntity().withId(UUID.fromString(getAsJsonObject(getAsJsonObject(hold.getJsonb()).get()
          .getAsString(DELETED_AUDIT_RECORD)).get()
          .getAsString("id")))
        .withJsonb(getAsJsonObject(hold.getJsonb()).get()
          .getAsString(DELETED_AUDIT_RECORD))
        .withInstanceId(UUID.fromString(getAsJsonObject(getAsJsonObject(hold.getJsonb()).get()
          .getAsString(DELETED_AUDIT_RECORD)).get()
          .getAsString("instanceId"))))
      .toList();
  }

  private void createAndSaveMarc(Set<UUID> holdingsIds, List<HoldingsRecordEntity> holdings, ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile, UUID jobExecutionId, ExportRequest exportRequest) {
    var externalIdsWithMarcRecord = new HashSet<UUID>();
    createMarc(holdingsIds, exportStatistic, mappingProfile, jobExecutionId, exportRequest, externalIdsWithMarcRecord);
    holdings.removeIf(hold -> externalIdsWithMarcRecord.contains(hold.getId()));
    var result = getGeneratedMarc(holdingsIds, holdings, mappingProfile, jobExecutionId, exportRequest);
    saveMarc(result, exportStatistic);
  }

  private GeneratedMarcResult getGeneratedMarc(Set<UUID> holdingsIds, List<HoldingsRecordEntity> holdings, MappingProfile mappingProfile,
      UUID jobExecutionId, ExportRequest exportRequest) {
    var result = new GeneratedMarcResult();
    var holdingsWithInstanceAndItems = getHoldingsWithInstanceAndItems(holdingsIds, holdings, result, mappingProfile, exportRequest);
    return getGeneratedMarc(mappingProfile, holdingsWithInstanceAndItems, jobExecutionId, result);
  }

  private List<JSONObject> getHoldingsWithInstanceAndItems(Set<UUID> holdingsIds, List<HoldingsRecordEntity> holdings, GeneratedMarcResult result,
      MappingProfile mappingProfile, ExportRequest exportRequest) {
    var instancesIds = holdings.stream().map(HoldingsRecordEntity::getInstanceId).collect(Collectors.toSet());
    if (Boolean.TRUE.equals(exportRequest.getDeletedRecords()) && isExportCompleted(exportRequest)) {
      List<HoldingsRecordDeletedEntity> holdingsDeleted;
      if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
        holdingsDeleted = holdingsRecordEntityDeletedRepository.findAll();
      } else {
        holdingsDeleted = holdingsRecordEntityDeletedRepository.findAllDeletedWhenSkipDiscoverySuppressed();
      }
      var holdingsDeletedToHoldingsEntities = holdingsDeletedToHoldingsEntities(holdingsDeleted);
      var instanceIdsDeleted = holdingsDeletedToHoldingsEntities.stream().map(HoldingsRecordEntity::getInstanceId).collect(Collectors.toSet());
      holdings.addAll(holdingsDeletedToHoldingsEntities);
      instancesIds.addAll(instanceIdsDeleted);
    }
    return getHoldingsWithInstanceAndItems(holdingsIds, result, mappingProfile, holdings, instancesIds);
  }
}
