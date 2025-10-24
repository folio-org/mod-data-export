package org.folio.dataexp.service.export.strategies;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.repository.MarcAuthorityRecordRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

/**
 * Export strategy for exporting all authority records.
 */
@Log4j2
@Component
public class AuthorityExportAllStrategy extends AuthorityExportStrategy {

  /**
   * Constructs an AuthorityExportAllStrategy.
   */
  public AuthorityExportAllStrategy(
      ConsortiaService consortiaService,
      ErrorLogEntityCqlRepository errorLogEntityCqlRepository,
      MarcAuthorityRecordRepository marcAuthorityRecordRepository
  ) {
    super(
        consortiaService,
        errorLogEntityCqlRepository,
        marcAuthorityRecordRepository
    );
  }

  /**
   * Gets all MARC authority records for the given external IDs.
   */
  @Override
  protected List<MarcRecordEntity> getMarcAuthorities(Set<UUID> externalIds) {
    return marcAuthorityRecordRepository.findAllByExternalIdIn(
        folioExecutionContext.getTenantId(),
        externalIds
    );
  }

  /**
   * Processes slices for exporting all authority records.
   */
  @Override
  protected void processSlices(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      ExportRequest exportRequest,
      LocalStorageWriter localStorageWriter
  ) {
    var slice = chooseSlice(
        exportFilesEntity,
        exportRequest,
        PageRequest.of(0, exportIdsBatch)
    );
    log.info(
        "Slice size for authorities export all: {}",
        slice.getContent().size()
    );
    var exportIds = slice.getContent()
        .stream()
        .map(MarcRecordEntity::getExternalId)
        .collect(Collectors.toSet());
    log.info(
        "Size of exportIds for authorities export all: {}",
        exportIds.size()
    );
    createAndSaveMarc(
        exportIds,
        slice.getContent(),
        exportStatistic,
        mappingProfile,
        exportFilesEntity.getJobExecutionId(),
        localStorageWriter
    );
    while (slice.hasNext()) {
      slice = chooseSlice(
          exportFilesEntity,
          exportRequest,
          slice.nextPageable()
      );
      exportIds = slice.getContent()
          .stream()
          .map(MarcRecordEntity::getExternalId)
          .collect(Collectors.toSet());
      createAndSaveMarc(
          exportIds,
          slice.getContent(),
          exportStatistic,
          mappingProfile,
          exportFilesEntity.getJobExecutionId(),
          localStorageWriter
      );
    }
  }

  /**
   * Chooses the appropriate slice for exporting authorities.
   */
  private Slice<MarcRecordEntity> chooseSlice(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportRequest exportRequest,
      Pageable pageble
  ) {
    if (Boolean.TRUE.equals(exportRequest.getDeletedRecords())) {
      return marcAuthorityRecordAllRepository.findAllWithDeleted(
          exportFilesEntity.getFromId(),
          exportFilesEntity.getToId(),
          pageble
      );
    }
    return marcAuthorityRecordAllRepository.findAllWithoutDeleted(
        exportFilesEntity.getFromId(),
        exportFilesEntity.getToId(),
        pageble
    );
  }

  /**
   * Creates and saves MARC records for the given external IDs and records.
   */
  protected void createAndSaveMarc(
      Set<UUID> externalIds,
      List<MarcRecordEntity> marcRecords,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      UUID jobExecutionId,
      LocalStorageWriter localStorageWriter
  ) {
    var externalIdsWithMarcRecord = new HashSet<UUID>();
    createAndSaveMarcFromJsonRecord(
        externalIds,
        exportStatistic,
        mappingProfile,
        jobExecutionId,
        externalIdsWithMarcRecord,
        marcRecords,
        localStorageWriter
    );
  }
}
