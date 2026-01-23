package org.folio.dataexp.service;

import static org.folio.dataexp.util.Constants.INVENTORY_VIEW_PERMISSION;
import static org.folio.dataexp.util.Constants.LOCK_JOB_PROFILE_PERMISSION;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.client.EurekaUserPermissionsClient;
import org.folio.dataexp.client.OkapiUserPermissionsClient;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Service for retrieving user permissions from different platforms. */
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

  /**
   * Gets the permissions for the current user based on the configured platform.
   *
   * @return list of permission names
   */
  public List<String> getPermissions() {
    if (StringUtils.equals(EUREKA_PLATFORM, platform)) {
      var desiredPermissions = getDesiredPermissions();
      return eurekaUserPermissionsClient
          .getPermissions(folioExecutionContext.getUserId().toString(), desiredPermissions)
          .getPermissions();
    }
    return okapiUserPermissionsClient
        .getPermissions(folioExecutionContext.getUserId().toString())
        .getPermissionNames();
  }

  /**
   * Gets the desired permissions for the user.
   *
   * @return list of desired permission names
   */
  private List<String> getDesiredPermissions() {
    return List.of(INVENTORY_VIEW_PERMISSION, LOCK_JOB_PROFILE_PERMISSION);
  }
}
