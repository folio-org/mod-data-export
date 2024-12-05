package org.folio.dataexp.service.validators;

import lombok.RequiredArgsConstructor;
import org.folio.dataexp.service.permissions.PermissionsProvider;
import org.folio.dataexp.service.permissions.RequiredPermissionResolver;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
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
    return userPermissions.contains(readPermissionForEntity);
  }
}
