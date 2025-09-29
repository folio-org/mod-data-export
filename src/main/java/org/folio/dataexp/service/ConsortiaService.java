package org.folio.dataexp.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.client.ConsortiaClient;
import org.folio.dataexp.client.ConsortiumClient;
import org.folio.dataexp.domain.dto.UserTenant;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service for working with consortia and tenant relationships.
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ConsortiaService {

  private final FolioExecutionContext context;
  private final ConsortiaClient consortiaClient;
  private final ConsortiumClient consortiumClient;

  /**
   * Gets the central tenant ID for the current tenant.
   *
   * @param currentTenantId The current tenant ID.
   * @return Central tenant ID or empty string if not found.
   */
  @Cacheable(value = "centralTenantCache")
  public String getCentralTenantId(String currentTenantId) {
    var userTenantCollection = consortiaClient.getUserTenantCollection();
    var userTenants = userTenantCollection.getUserTenants();
    if (!userTenants.isEmpty()) {
      log.debug("userTenants: {}", userTenants);
      return userTenants.get(0).getCentralTenantId();
    }
    log.debug("No central tenant found for {}", currentTenantId);
    return StringUtils.EMPTY;
  }

  /**
   * Gets affiliated tenants for a user.
   *
   * @param currentTenantId The current tenant ID.
   * @param userId The user ID.
   * @return List of affiliated tenant IDs.
   */
  @Cacheable(value = "affiliatedTenantsCache")
  public List<String> getAffiliatedTenants(String currentTenantId, String userId) {
    var consortia = consortiumClient.getConsortia();
    var consortiaList = consortia.getConsortia();
    if (!consortiaList.isEmpty()) {
      var userTenants = consortiumClient.getConsortiaUserTenants(consortiaList.get(0)
          .getId(), userId, Integer.MAX_VALUE);
      return userTenants.getUserTenants().stream().map(UserTenant::getTenantId).toList();
    }
    return new ArrayList<>();
  }

  /**
   * Gets tenants with permissions from a list of affiliated tenants.
   *
   * @param affiliatedTenants List of affiliated tenant IDs.
   * @return List of tenant IDs with permissions.
   */
  @Cacheable(value = "permittedTenantsCache")
  public List<String> getTenantsWithPermissions(List<String> affiliatedTenants) {
    throw new UnsupportedOperationException("This feature is not implemented yet.");
  }

  /**
   * Checks if the current tenant is the central tenant.
   *
   * @param currentTenantId The current tenant ID.
   * @return True if current tenant is central, false otherwise.
   */
  @Cacheable(value = "isCurrentTenantCentralTenant")
  public boolean isCurrentTenantCentralTenant(String currentTenantId) {
    return getCentralTenantId(currentTenantId).equals(context.getTenantId());
  }
}
