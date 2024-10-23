package org.folio.dataexp.service.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.service.permissions.PermissionsProvider;
import org.folio.dataexp.service.permissions.RequiredPermissionResolver;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class PermissionsValidator {

  private final PermissionsProvider permissionsProvider;
  private final RequiredPermissionResolver requiredPermissionResolver;
  private final FolioExecutionContext folioExecutionContext;

  public boolean checkInstanceViewPermissions(String tenantId) {
    return isInstanceViewPermissionExists(tenantId);
  }

  public boolean isInstanceViewPermissionExists(String tenantId) {
    var readPermissionForEntity = requiredPermissionResolver.getReadPermission();
    var userPermissions = permissionsProvider.getUserPermissions(tenantId, folioExecutionContext.getUserId().toString());
    var isViewPermissionsExist = userPermissions.contains(readPermissionForEntity);
    log.info("isInstanceViewPermissionExists:: user {} has read permissions {} in tenant {}", folioExecutionContext.getUserId(),
      isViewPermissionsExist, tenantId);
    return isViewPermissionsExist;
  }
}
