package org.folio.dataexp.service.permissions;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.service.UserPermissionsService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.folio.dataexp.util.FolioExecutionContextUtil.prepareContextForTenant;

@Component
@RequiredArgsConstructor
@Log4j2
public class PermissionsProvider {

  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;
  private final UserPermissionsService userPermissionsService;

  @Cacheable(cacheNames = "userPermissions")
  public List<String> getUserPermissions(String tenantId, String userId) {
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      log.info("getUserPermissions:: user {} tenant {}", userId, tenantId);

      return userPermissionsService.getPermissions();
    }
  }
}
