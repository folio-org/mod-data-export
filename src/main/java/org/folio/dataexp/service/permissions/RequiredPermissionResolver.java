package org.folio.dataexp.service.permissions;

import static org.folio.dataexp.util.Constants.INVENTORY_VIEW_PERMISSION;

import org.springframework.stereotype.Component;

/**
 * Resolver for required permissions for reading inventory.
 */
@Component
public class RequiredPermissionResolver {

  /**
   * Gets the required read permission for inventory.
   *
   * @return permission string
   */
  public String getReadPermission() {
    return INVENTORY_VIEW_PERMISSION;
  }
}
