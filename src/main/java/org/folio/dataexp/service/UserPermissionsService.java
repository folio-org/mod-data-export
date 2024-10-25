package org.folio.dataexp.service;

import static org.folio.dataexp.util.Constants.INVENTORY_VIEW_PERMISSION;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.client.EurekaUserPermissionsClient;
import org.folio.dataexp.client.OkapiUserPermissionsClient;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
@Service
public class UserPermissionsService {

  public static final String EUREKA_PLATFORM = "eureka";
  public static final String OKAPI_PLATFORM = "okapi";

  @Setter
  @Value("${application.platform}")
  private String platform;

  private final FolioExecutionContext folioExecutionContext;
  private final OkapiUserPermissionsClient okapiUserPermissionsClient;
  private final EurekaUserPermissionsClient eurekaUserPermissionsClient;

  public List<String> getPermissions() {
    if (StringUtils.equals(EUREKA_PLATFORM, platform)) {
      var desiredPermissions = getDesiredPermissions();
      return eurekaUserPermissionsClient.getPermissions(folioExecutionContext.getUserId().toString(),
        desiredPermissions).getPermissions();
    }
    return okapiUserPermissionsClient.getPermissions(folioExecutionContext.getUserId().toString()).getPermissionNames();
  }

  private List<String> getDesiredPermissions() {
    return List.of(INVENTORY_VIEW_PERMISSION);
  }
}
