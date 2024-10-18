package org.folio.dataexp.service.permissions;

import org.springframework.stereotype.Component;

@Component
public class RequiredPermissionResolver {

  public String getReadPermission() {
    return "ui-inventory.instance.view";
  }
}
