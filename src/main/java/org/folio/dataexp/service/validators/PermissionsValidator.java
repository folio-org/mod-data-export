package org.folio.dataexp.service.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.folio.dataexp.service.permissions.PermissionsProvider;
import org.folio.dataexp.service.permissions.RequiredPermissionResolver;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

/** Validator for checking user permissions related to instance view. */
@Component
@RequiredArgsConstructor
@Slf4j
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
    var userPermissions =
        permissionsProvider.getUserPermissions(
            tenantId, folioExecutionContext.getUserId().toString());
    return userPermissions.contains(readPermissionForEntity);
  }

  /**
   * Checks if the user has permission to lock/unlock job profiles for the given tenant.
   *
   * @return true if permission exists, false otherwise
   */
  public boolean checkLockJobProfilePermission() {
    var lockPermission = requiredPermissionResolver.getLockJobProfilePermission();
    if (lockPermission == null) {
      return false;
    }
    var userPermissions =
        permissionsProvider.getUserPermissions(
            folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString());
    log.info("userPermissions: {}", userPermissions);
    return userPermissions.contains(lockPermission);
  }
    
  /**
   * Checks if the user has permission to lock/unlock mapping profiles for the given tenant.
   *
   * @return true if permission exists, false otherwise
   */
  public boolean checkLockMappingProfilePermission() {
    var lockPermission = requiredPermissionResolver.getLockMappingProfilePermission();
    if (lockPermission == null) {
      return false;
    }
    var userPermissions =
        permissionsProvider.getUserPermissions(
            folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString());
    log.info("userPermissions: {}", userPermissions);
    return userPermissions.contains(lockPermission);
  }
}
