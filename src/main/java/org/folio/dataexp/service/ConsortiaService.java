package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.client.ConsortiaClient;
import org.folio.dataexp.client.ConsortiumClient;
import org.folio.dataexp.domain.dto.UserTenant;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class ConsortiaService {

  private final FolioExecutionContext context;

  private final ConsortiaClient consortiaClient;
  private final ConsortiumClient consortiumClient;

  @Cacheable(value = "centralTenantCache")
  public String getCentralTenantId(String currentTenantId) {
    var userTenantCollection = consortiaClient.getUserTenantCollection();
    var userTenants = userTenantCollection.getUserTenants();
    if (!userTenants.isEmpty()) {
      log.debug("userTenants: {}", userTenants);
      var centralTenantId = userTenants.get(0).getCentralTenantId();
      return centralTenantId;
    }
    log.debug("No central tenant found");
    return StringUtils.EMPTY;
  }

  @Cacheable(value = "affiliatedTenantsCache")
  public List<String> getAffiliatedTenants(String currentTenantId, String userId) {
    var consortia = consortiumClient.getConsortia();
    var consortiaList = consortia.getConsortia();
    if (!consortiaList.isEmpty()) {
      var userTenants = consortiumClient.getConsortiaUserTenants(consortiaList.get(0).getId(), userId, Integer.MAX_VALUE);
      return userTenants.getUserTenants().stream().map(UserTenant::getTenantId).toList();
    }
    return new ArrayList<>();
  }

  @Cacheable(value = "permittedTenantsCache")
  public List<String> getTenantsWithPermissions(List<String> affiliatedTenants) {
    throw new UnsupportedOperationException("This feature is not implemented yet.");
  }

  @Cacheable(value = "isCurrentTenantCentralTenant")
  public boolean isCurrentTenantCentralTenant(String currentTenantId) {
    return getCentralTenantId(currentTenantId).equals(context.getTenantId());
  }
}
