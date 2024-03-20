package org.folio.dataexp.service.export.strategies;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.MarcAuthorityRecordRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_UUID_IS_SET_TO_DELETION;

@Log4j2
@Component
@RequiredArgsConstructor
public class AuthorityExportStrategy extends AbstractExportStrategy {

  private final ConsortiaService consortiaService;

  protected final MarcAuthorityRecordRepository marcAuthorityRecordRepository;
  protected final FolioExecutionContext context;

  @Override
  List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds, MappingProfile mappingProfile, ExportRequest exportRequest,
                                        UUID jobExecutionId) {
    if (Boolean.TRUE.equals(mappingProfile.getDefault())) {
      List<MarcRecordEntity> marcAuthorities = getMarcAuthorities(externalIds);
      log.info("Total marc authorities: {}", marcAuthorities.size());
      if (isDeletedJobProfile(exportRequest.getJobProfileId())) {
        log.info("Deleted job profile for authority is being used.");
      }
      handleDeleted(marcAuthorities, jobExecutionId, exportRequest);
      log.info("Marc authorities after removing: {}", marcAuthorities.size());
      entityManager.clear();
      var foundIds = marcAuthorities.stream().map(rec -> rec.getExternalId()).collect(Collectors.toSet());
      externalIds.removeAll(foundIds);
      log.info("Number of authority records found from local tenant: {}, not found: {}", foundIds.size(), externalIds.size());
      if (!externalIds.isEmpty()) {
        var centralTenantId = consortiaService.getCentralTenantId();
        if (StringUtils.isNotEmpty(centralTenantId)) {
          var authoritiesFromCentralTenant = marcAuthorityRecordRepository.findNonDeletedByExternalIdIn(centralTenantId, externalIds);
          log.info("Number of authority records found from central tenant: {}", authoritiesFromCentralTenant.size());
          entityManager.clear();
          marcAuthorities.addAll(authoritiesFromCentralTenant);
          log.info("Total number of authority records found: {}", marcAuthorities.size());
        } else {
          log.error("Central tenant id not found: {}, authorities that cannot be found: {}",
              centralTenantId, externalIds);
        }
      }
      log.debug("Final authority records: {}", marcAuthorities);
      return marcAuthorities;
    }
    return new ArrayList<>();
  }

  protected List<MarcRecordEntity> getMarcAuthorities(Set<UUID> externalIds) {
    return marcAuthorityRecordRepository.findNonDeletedByExternalIdIn(context.getTenantId(), externalIds);
  }

  private void handleDeleted(List<MarcRecordEntity> marcAuthorities, UUID jobExecutionId, ExportRequest exportRequest) {
    var iterator = marcAuthorities.iterator();
    while (iterator.hasNext()) {
      var rec = iterator.next();
      if (rec.getState().equals("DELETED")) {
        if (!isDeletedJobProfile(exportRequest.getJobProfileId())) {
          var msg = format(ERROR_MESSAGE_UUID_IS_SET_TO_DELETION.getDescription(), rec.getExternalId());
          errorLogService.saveGeneralErrorWithMessageValues(ERROR_MESSAGE_UUID_IS_SET_TO_DELETION.getCode(),
            List.of(msg), jobExecutionId);
          log.error(msg);
          iterator.remove();
        }
      } else if (rec.getState().equals("ACTUAL") && isDeletedJobProfile(exportRequest.getJobProfileId())) {
        var msg = ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getDescription();
        errorLogService.saveGeneralErrorWithMessageValues(ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getCode(),
          List.of(msg), jobExecutionId);
        log.error(msg);
        iterator.remove();
      }
    }
  }

  @Override
  GeneratedMarcResult getGeneratedMarc(Set<UUID> ids, MappingProfile mappingProfile, ExportRequest exportRequest,
      UUID jobExecutionId, ExportStrategyStatistic exportStatistic) {
    var result = new GeneratedMarcResult();
    ids.forEach(id -> {
      result.addIdToFailed(id);
      result.addIdToNotExist(id);
    });
    return result;
  }

  @Override
  Optional<ExportIdentifiersForDuplicateErrors> getIdentifiers(UUID id) {
    return Optional.empty();
  }

  @Override
  public Map<UUID,MarcFields> getAdditionalMarcFieldsByExternalId(List<MarcRecordEntity> marcRecords, MappingProfile mappingProfile) {
    return new HashMap<>();
  }
}
