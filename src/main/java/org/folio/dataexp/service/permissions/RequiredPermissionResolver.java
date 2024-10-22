package org.folio.dataexp.service.permissions;

import static org.folio.dataexp.util.Constants.INVENTORY_VIEW_PERMISSION;

import org.springframework.stereotype.Component;

@Component
public class RequiredPermissionResolver {

  public String getReadPermission() {
    return INVENTORY_VIEW_PERMISSION;
  }
}
