package org.folio.dataexp.service.validators;

import lombok.RequiredArgsConstructor;
import org.folio.dataexp.service.permissions.PermissionsProvider;
import org.folio.dataexp.service.permissions.RequiredPermissionResolver;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

/**
 * Validator for checking user permissions related to instance view.
 */
@Component
@RequiredArgsConstructor
public class PermissionsValidator {

  private final PermissionsProvider permissionsProvider;
  private final RequiredPermissionResolver requiredPermissionResolver;
  private final FolioExecutionContext folioExecutionContext;

  /**
   * Checks if the user has instance view permissions for the given tenant.
   *
   * @param tenantId the tenant ID
   * @return true if permission exists, false otherwise
   */
  public boolean checkInstanceViewPermissions(String tenantId) {
    return isInstanceViewPermissionExists(tenantId);
  }

  /**
   * Determines if the user has the required instance view permission for the given tenant.
   *
   * @param tenantId the tenant ID
   * @return true if permission exists, false otherwise
   */
  public boolean isInstanceViewPermissionExists(String tenantId) {
    var readPermissionForEntity = requiredPermissionResolver.getReadPermission();
    var userPermissions = permissionsProvider.getUserPermissions(
        tenantId, folioExecutionContext.getUserId().toString());
    return userPermissions.contains(readPermissionForEntity);
  }
}
