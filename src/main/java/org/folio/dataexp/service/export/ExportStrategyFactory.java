package org.folio.dataexp.service.export;

import static org.folio.dataexp.service.export.Constants.DEFAULT_MAPPING_PROFILE_IDS;

import lombok.AllArgsConstructor;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.IdType;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.service.export.strategies.AbstractMarcExportStrategy;
import org.folio.dataexp.service.export.strategies.AuthorityExportAllStrategy;
import org.folio.dataexp.service.export.strategies.AuthorityExportStrategy;
import org.folio.dataexp.service.export.strategies.ExportStrategy;
import org.folio.dataexp.service.export.strategies.HoldingsExportAllStrategy;
import org.folio.dataexp.service.export.strategies.HoldingsExportStrategy;
import org.folio.dataexp.service.export.strategies.InstancesExportAllStrategy;
import org.folio.dataexp.service.export.strategies.InstancesExportStrategy;
import org.folio.dataexp.service.export.strategies.ld.LinkedDataExportStrategy;
import org.springframework.stereotype.Component;

/**
 * Factory for selecting the appropriate export strategy based on request or record type.
 */
@Component
@AllArgsConstructor
public class ExportStrategyFactory {

  private final HoldingsExportStrategy holdingsExportStrategy;
  private final InstancesExportStrategy instancesExportStrategy;
  private final AuthorityExportStrategy authorityExportStrategy;
  private final LinkedDataExportStrategy linkedDataExportStrategy;
  private final InstancesExportAllStrategy instancesExportAllStrategy;
  private final HoldingsExportAllStrategy holdingsExportAllStrategy;
  private final AuthorityExportAllStrategy authorityExportAllStrategy;
  private final JobProfileEntityRepository jobProfileEntityRepository;
  private final MappingProfileEntityRepository mappingProfileEntityRepository;

  /**
   * Gets the export strategy for the given export request.
   *
   * @param exportRequest the export request
   * @return the export strategy
   */
  public ExportStrategy getExportStrategy(ExportRequest exportRequest) {
    var mappingProfileEntity = getMappingProfileEntity(exportRequest);
    if (isHolding(exportRequest, mappingProfileEntity)) {
      if (Boolean.TRUE.equals(exportRequest.getAll())) {
        return holdingsExportAllStrategy;
      }
      return holdingsExportStrategy;
    } else if (isAuthority(exportRequest, mappingProfileEntity)) {
      if (Boolean.TRUE.equals(exportRequest.getAll())) {
        return authorityExportAllStrategy;
      }
      return authorityExportStrategy;
    }
    if (isLinkedData(mappingProfileEntity)) {
      return linkedDataExportStrategy;
    }
    if (Boolean.TRUE.equals(exportRequest.getAll())) {
      return instancesExportAllStrategy;
    }
    return instancesExportStrategy;
  }

  /**
   * Gets the export strategy for the given record ID type.
   *
   * @param recordIdType the record ID type
   * @return the abstract export strategy
   */
  public AbstractMarcExportStrategy getExportStrategy(IdType recordIdType) {
    return switch (recordIdType) {
      case AUTHORITY -> authorityExportStrategy;
      case INSTANCE -> instancesExportStrategy;
    };
  }

  /**
   * Extract the mapping profile from the job profile associated with the export request.
   */
  private MappingProfileEntity getMappingProfileEntity(ExportRequest exportRequest) {
    var jobProfileEntity = jobProfileEntityRepository.getReferenceById(
        exportRequest.getJobProfileId());
    var mappingProfileId = jobProfileEntity.getJobProfile().getMappingProfileId();
    return mappingProfileEntityRepository.getReferenceById(mappingProfileId);
  }

  /**
   * Determine if a mapping proflie is in the set of default mapping profiles.
   */
  private boolean isDefaultMappingProfile(MappingProfileEntity mappingProfileEntity) {
    return DEFAULT_MAPPING_PROFILE_IDS.contains(mappingProfileEntity.getId().toString());
  }

  /**
   * Evalute if an export request relates to holdings.
   */
  private boolean isHolding(ExportRequest exportRequest,
      MappingProfileEntity mappingProfileEntity) {
    var isHoldingProfile = isDefaultProfileRecordType(mappingProfileEntity,
        RecordTypes.HOLDINGS);
    var isHoldingRequest = isCustomProfileIdType(exportRequest, mappingProfileEntity,
        ExportRequest.IdTypeEnum.HOLDING);
    return isHoldingProfile || isHoldingRequest;
  }

  /**
   * Evalute if an export request relates to authorities.
   */
  private boolean isAuthority(ExportRequest exportRequest,
      MappingProfileEntity mappingProfileEntity) {
    var isAuthorityProfile = isDefaultProfileRecordType(mappingProfileEntity,
        RecordTypes.AUTHORITY);
    var isAuthorityRequest = isCustomProfileIdType(exportRequest, mappingProfileEntity,
        ExportRequest.IdTypeEnum.AUTHORITY);
    return isAuthorityProfile || isAuthorityRequest;
  }

  /**
   * Evalute if an export request relates to Linked Data.
   */
  private boolean isLinkedData(MappingProfileEntity mappingProfileEntity) {
    // Linked Data is not currently available as a custom mapping profile option, otherwise
    // additional input about the record type would be necessary from the user.
    return isDefaultProfileRecordType(mappingProfileEntity, RecordTypes.LINKED_DATA);
  }

  /**
   * Evalute if a default mapping profile and if it relates to the given record type.
   */
  private boolean isDefaultProfileRecordType(MappingProfileEntity mappingProfileEntity,
      RecordTypes type) {
    return isDefaultMappingProfile(mappingProfileEntity)
        && mappingProfileEntity.getMappingProfile().getRecordTypes().contains(type);
  }

  /**
   * Evaluate if a custom mapping profile and if it that relates to the given ID type.
   */
  private boolean isCustomProfileIdType(ExportRequest exportRequest,
      MappingProfileEntity mappingProfileEntity, ExportRequest.IdTypeEnum type) {
    return !isDefaultMappingProfile(mappingProfileEntity)
        && exportRequest.getIdType() == type;
  }
}
