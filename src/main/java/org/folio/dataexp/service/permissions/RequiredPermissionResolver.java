package org.folio.dataexp.service.permissions;

import static org.folio.dataexp.util.Constants.INVENTORY_VIEW_PERMISSION;
import static org.folio.dataexp.util.Constants.LOCK_MAPPING_PROFILE_PERMISSION;
import static org.folio.dataexp.util.Constants.LOCK_JOB_PROFILE_PERMISSION;

import org.springframework.stereotype.Component;

/** Resolver for required permissions for reading inventory. */
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
  
   * Gets the required permission for locking a job profile.
   *
   * @return permission string
   */
  public String getLockJobProfilePermission() {
    return LOCK_JOB_PROFILE_PERMISSION;
  }
  
  /**
   * Gets the required permission for locking a mapping profile.
   *
   * @return permission string
   */
  public String getLockMappingProfilePermission() {
    return LOCK_MAPPING_PROFILE_PERMISSION;
  }
}
