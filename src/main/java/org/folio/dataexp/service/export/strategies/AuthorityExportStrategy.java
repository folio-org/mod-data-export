package org.folio.dataexp.service.export.strategies;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.MarcAuthorityRecordRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Component
@RequiredArgsConstructor
public class AuthorityExportStrategy extends AbstractExportStrategy {

  private final RuleFactory ruleFactory;
  private final ConsortiaService consortiaService;
  private final MarcAuthorityRecordRepository marcAuthorityRecordRepository;

  @Value("${myuniversity}")
  private String tenantId;

  @Override
  List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds, MappingProfile mappingProfile) {
    if (Boolean.TRUE.equals(mappingProfile.getDefault())) {
      var marcAuthorities = marcAuthorityRecordRepository.findByExternalIdIn(tenantId, externalIds);
      var foundIds = marcAuthorities.stream().map(rec -> rec.getExternalId()).collect(Collectors.toSet());
      externalIds.removeAll(foundIds);
      log.info("Number of authority records found from local tenant: {}, not found: {}", foundIds.size(), externalIds.size());
      if (!externalIds.isEmpty()) {
        var centralTenantId = consortiaService.getCentralTenantId();
        if (StringUtils.isNotEmpty(centralTenantId)) {
          var authoritiesFromCentralTenant = marcAuthorityRecordRepository.findByExternalIdIn(centralTenantId, externalIds);
          log.info("Number of authority records found from central tenant: {}", authoritiesFromCentralTenant.size());
          marcAuthorities.addAll(authoritiesFromCentralTenant);
          log.info("Total number of authority records found: {}", marcAuthorities.size());
        } else {
          log.error("Central tenant id not found: {}, authorities that cannot be found: {}",
              centralTenantId, externalIds);
        }
      }
      //TODO change to debug
      log.info("Final authority records: {}", marcAuthorities);
      return marcAuthorities;
    }
    return new ArrayList<>();
  }

  @Override
  GeneratedMarcResult getGeneratedMarc(Set<UUID> ids, MappingProfile mappingProfile) {
    return new GeneratedMarcResult();
  }

  @Override
  Optional<String> getIdentifierMessage(UUID id) {
    return Optional.empty();
  }
}
