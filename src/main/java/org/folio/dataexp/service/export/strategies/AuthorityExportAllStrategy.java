package org.folio.dataexp.service.export.strategies;

import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.MarcAuthorityRecordRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Component
public class AuthorityExportAllStrategy extends AuthorityExportStrategy {

  public AuthorityExportAllStrategy(ConsortiaService consortiaService, MarcAuthorityRecordRepository marcAuthorityRecordRepository, FolioExecutionContext context) {
    super(consortiaService, marcAuthorityRecordRepository, context);
  }

  @Override
  protected List<MarcRecordEntity> getMarcAuthorities(Set<UUID> externalIds) {
    return marcAuthorityRecordRepository.findAllByExternalIdIn(context.getTenantId(), externalIds);
  }

  @Override
  protected void processSlices(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile, ExportRequest exportRequest) {
    var slice = chooseSlice(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
    updateSliceState(slice, exportRequest);
    log.info("Slice size for authorities export all: {}", slice.getSize());
    var exportIds = slice.getContent().stream().map(MarcRecordEntity::getExternalId).collect(Collectors.toSet());
    log.info("Size of exportIds for authorities export all: {}", exportIds.size());
    createAndSaveMarc(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(), exportRequest);
    while (slice.hasNext()) {
      slice = chooseSlice(exportFilesEntity, exportRequest, slice.nextPageable());
      updateSliceState(slice, exportRequest);
      exportIds = slice.getContent().stream().map(MarcRecordEntity::getExternalId).collect(Collectors.toSet());
      createAndSaveMarc(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(), exportRequest);
    }
  }

  private Slice<MarcRecordEntity> chooseSlice(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, Pageable pageble) {
    if (Boolean.TRUE.equals(exportRequest.getDeletedRecords())) {
      return marcAuthorityRecordAllRepository.findAllWithDeleted(exportFilesEntity.getFromId(), exportFilesEntity.getToId(), pageble);
    }
    return marcAuthorityRecordAllRepository.findAllWithoutDeleted(exportFilesEntity.getFromId(), exportFilesEntity.getToId(), pageble);
  }
}