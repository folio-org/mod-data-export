package org.folio.dataexp.service.export.strategies;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.MarcInstanceRecordRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Component
@AllArgsConstructor
public class InstancesExportStrategy extends AbstractExportStrategy {

  private static final String INSTANCE_MARC_TYPE = "MARC_BIB";

  private final ConsortiaService consortiaService;
  private final MarcInstanceRecordRepository marcInstanceRecordRepository;
  private final MarcRecordEntityRepository marcRecordEntityRepository;

  @Override
  public List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds, MappingProfile mappingProfile) {
    if (Boolean.TRUE.equals(mappingProfile.getDefault())) {
      var marcInstances =  marcRecordEntityRepository.findByExternalIdInAndRecordTypeIs(externalIds, INSTANCE_MARC_TYPE);
      var foundIds = marcInstances.stream().map(MarcRecordEntity::getExternalId).collect(Collectors.toSet());
      externalIds.removeAll(foundIds);
      if (!externalIds.isEmpty()) {
        var centralTenantId = consortiaService.getCentralTenantId();
        if (StringUtils.isNotEmpty(centralTenantId)) {
          var marcInstancesFromCentralTenant = marcInstanceRecordRepository.findByExternalIdIn(centralTenantId, externalIds);
          marcInstances.addAll(marcInstancesFromCentralTenant);
        } else {
          log.error("Central tenant id not found: {}, external ids that cannot be found: {}",
            centralTenantId, externalIds);
        }
      }
      return marcInstances;
    }
    return new ArrayList<>();
  }

  @Override
  public GeneratedMarcResult getGeneratedMarc(Set<UUID> ids, MappingProfile mappingProfile) {
    return new GeneratedMarcResult();
  }

  @Override
  public Optional<String> getIdentifierMessage(UUID id) {
    return Optional.empty();
  }
}
