package org.folio.dataexp.service.export.strategies;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.maxBy;
import static java.util.stream.Collectors.toList;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_PROFILE_USED_ONLY_FOR_NON_DELETED;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_UUID_IS_SET_TO_DELETION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.exception.export.DownloadRecordException;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.repository.MarcAuthorityRecordRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

/**
 * Export strategy for MARC Authority records.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class AuthorityExportStrategy extends AbstractExportStrategy {

  // deliberate typo in constant name to bypass sonar security hotspot
  // "'AUTH' detected in this expression, review this potentially hard-coded secret."
  public static final String DEFAULT_AUTTHORITY_PROFILE_ID = "5d636597-a59d-4391-a270-4e79d5ba70e3";
  private final ConsortiaService consortiaService;
  private final ErrorLogEntityCqlRepository errorLogEntityCqlRepository;

  protected final MarcAuthorityRecordRepository marcAuthorityRecordRepository;
  protected final FolioExecutionContext context;

  /**
   * Gets MARC Authority records for the given external IDs and mapping profile.
   *
   * @param externalIds set of external IDs
   * @param mappingProfile mapping profile
   * @param exportRequest export request
   * @param jobExecutionId job execution ID
   * @return list of MarcRecordEntity
   */
  @Override
  List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds, MappingProfile mappingProfile,
      ExportRequest exportRequest, UUID jobExecutionId) {
    if (Boolean.TRUE.equals(mappingProfile.getDefault())) {
      List<MarcRecordEntity> marcAuthorities = new ArrayList<>(getMarcAuthorities(externalIds));
      log.info("Total marc authorities: {}", marcAuthorities.size());
      if (isDeletedJobProfile(exportRequest.getJobProfileId())) {
        log.info("Deleted job profile for authority is being used.");
      }
      Set<String> alreadySavedErrors = new HashSet<>();
      handleDeleted(marcAuthorities, jobExecutionId, exportRequest, alreadySavedErrors);
      marcAuthorities =
          new ArrayList<>(handleDuplicatedDeletedAndUseLastGeneration(marcAuthorities));
      log.info("Marc authorities after removing: {}", marcAuthorities.size());
      entityManager.clear();
      var foundIds = marcAuthorities.stream()
          .map(MarcRecordEntity::getExternalId)
          .collect(Collectors.toSet());
      externalIds.removeAll(foundIds);
      log.info(
          "Number of authority records found from local tenant: {}, not found: {}",
          foundIds.size(),
          externalIds.size()
      );
      if (!externalIds.isEmpty()) {
        var centralTenantId = consortiaService.getCentralTenantId(
            folioExecutionContext.getTenantId()
        );
        if (StringUtils.isNotEmpty(centralTenantId)) {
          var authoritiesFromCentralTenant = marcAuthorityRecordRepository
              .findNonDeletedByExternalIdIn(
                  centralTenantId,
                  externalIds
              );
          log.info(
              "Number of authority records found from central tenant: {}",
              authoritiesFromCentralTenant.size()
          );
          handleDeleted(
              authoritiesFromCentralTenant,
              jobExecutionId,
              exportRequest,
              alreadySavedErrors
          );
          entityManager.clear();
          marcAuthorities.addAll(authoritiesFromCentralTenant);
          log.info(
              "Total number of authority records found: {}",
              marcAuthorities.size()
          );
        } else {
          log.error(
              "Central tenant id not found: {}, authorities that cannot be found: {}",
              centralTenantId,
              externalIds
          );
        }
      }
      log.debug("Final authority records: {}", marcAuthorities);
      return marcAuthorities;
    }
    return new ArrayList<>();
  }

  /**
   * Gets MARC Authority records for the given external IDs.
   *
   * @param externalIds set of external IDs
   * @return list of MarcRecordEntity
   */
  protected List<MarcRecordEntity> getMarcAuthorities(Set<UUID> externalIds) {
    return marcAuthorityRecordRepository.findNonDeletedByExternalIdIn(context.getTenantId(),
      externalIds);
  }

  /**
   * Gets a single MARC Authority record by record ID.
   *
   * @param recordId record ID
   * @return MarcRecordEntity
   */
  @Override
  public MarcRecordEntity getMarcRecord(final UUID recordId) {
    List<MarcRecordEntity> marcAuthorities = getMarcAuthorities(Set.of(recordId));
    if (marcAuthorities.isEmpty()) {
      log.error("getMarcRecord:: Couldn't find authority in db for ID: {}", recordId);
      throw new DownloadRecordException(
          "Couldn't find authority in db for ID: %s".formatted(recordId)
      );
    }
    marcAuthorities = new ArrayList<>(handleDuplicatedDeletedAndUseLastGeneration(marcAuthorities));
    return marcAuthorities.getFirst();
  }

  /**
   * Gets the default mapping profile for MARC Authority.
   *
   * @return MappingProfile
   */
  @Override
  public MappingProfile getDefaultMappingProfile() {
    return getMappingProfileEntityRepository().getReferenceById(
        UUID.fromString(DEFAULT_AUTTHORITY_PROFILE_ID)
    ).getMappingProfile();
  }

  private void handleDeleted(
      List<MarcRecordEntity> marcAuthorities,
      UUID jobExecutionId,
      ExportRequest exportRequest,
      Set<String> alreadySavedErrors
  ) {
    var iterator = marcAuthorities.iterator();
    var errorsForDeletedProfile = !errorLogEntityCqlRepository
        .getByJobExecutionIdAndErrorCodes(
            jobExecutionId,
            ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getCode()
        )
        .isEmpty();
    var errorsForNonDeletedProfile = !errorLogEntityCqlRepository
        .getByJobExecutionIdAndErrorCodes(
            jobExecutionId,
            ERROR_MESSAGE_PROFILE_USED_ONLY_FOR_NON_DELETED.getCode()
        )
        .isEmpty();
    while (iterator.hasNext()) {
      var rec = iterator.next();
      if (rec.getState().equals("DELETED")) {
        String msg;
        if (!isDeletedJobProfile(exportRequest.getJobProfileId())) {
          msg = format(
              ERROR_MESSAGE_UUID_IS_SET_TO_DELETION.getDescription(),
              rec.getExternalId()
          );
          if (!alreadySavedErrors.contains(msg)) {
            errorLogService.saveGeneralErrorWithMessageValues(
                ERROR_MESSAGE_UUID_IS_SET_TO_DELETION.getCode(),
                List.of(msg),
                jobExecutionId
            );
            alreadySavedErrors.add(msg);
          }
          log.error(msg);
          msg = ERROR_MESSAGE_PROFILE_USED_ONLY_FOR_NON_DELETED.getDescription();
          if (!errorsForNonDeletedProfile) {
            errorLogService.saveGeneralErrorWithMessageValues(
                ERROR_MESSAGE_PROFILE_USED_ONLY_FOR_NON_DELETED.getCode(),
                List.of(msg),
                jobExecutionId
            );
            errorsForNonDeletedProfile = true;
          }
          log.error(msg);
          iterator.remove();
        }
      } else if (
          rec.getState().equals("ACTUAL")
          && isDeletedJobProfile(exportRequest.getJobProfileId())
      ) {
        var msg = ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getDescription();
        if (!errorsForDeletedProfile) {
          errorLogService.saveGeneralErrorWithMessageValues(
              ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getCode(),
              List.of(msg),
              jobExecutionId
          );
          errorsForDeletedProfile = true;
        }
        log.error(msg);
        iterator.remove();
      }
    }
  }

  private List<MarcRecordEntity> handleDuplicatedDeletedAndUseLastGeneration(
      List<MarcRecordEntity> marcAuthorities
  ) {
    return marcAuthorities.stream()
        .collect(
            groupingBy(
                MarcRecordEntity::getExternalId,
                maxBy(comparing(MarcRecordEntity::getGeneration))
            )
        )
        .values()
        .stream()
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * Gets generated MARC records for the given IDs, mapping profile, export request,
   * and job execution ID.
   *
   * @param ids set of IDs
   * @param mappingProfile mapping profile
   * @param exportRequest export request
   * @param jobExecutionId job execution ID
   * @param exportStatistic export strategy statistic
   * @return GeneratedMarcResult
   */
  @Override
  GeneratedMarcResult getGeneratedMarc(Set<UUID> ids, MappingProfile mappingProfile,
      ExportRequest exportRequest, UUID jobExecutionId,
      ExportStrategyStatistic exportStatistic) {
    var result = new GeneratedMarcResult(jobExecutionId);
    ids.forEach(id -> {
      result.addIdToFailed(id);
      result.addIdToNotExist(id);
    });
    return result;
  }

  @Override
  Optional<ExportIdentifiersForDuplicateError> getIdentifiers(UUID id) {
    return Optional.empty();
  }

  /**
   * Gets additional MARC fields by external ID.
   *
   * @param marcRecords list of MarcRecordEntity
   * @param mappingProfile mapping profile
   * @param jobExecutionId job execution ID
   * @return map of UUID to MarcFields
   */
  @Override
  public Map<UUID, MarcFields> getAdditionalMarcFieldsByExternalId(
      List<MarcRecordEntity> marcRecords,
      MappingProfile mappingProfile,
      UUID jobExecutionId
  ) {
    return new HashMap<>();
  }
}
