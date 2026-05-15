package org.folio.dataexp.client;

import java.util.List;
import org.folio.dataexp.domain.dto.UserPermissions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving user permissions from Eureka. */
@HttpExchange(url = "users-keycloak/users")
public interface EurekaUserPermissionsClient {

  /**
   * Retrieves user permissions for a given user and desired permissions.
   *
   * @param userId the user ID
   * @param desiredPermissions the list of desired permissions
   * @return the user permissions
   */
  @GetExchange(value = "/{userId}/permissions", accept = MediaType.APPLICATION_JSON_VALUE)
  UserPermissions getPermissions(
      @PathVariable String userId, @RequestParam List<String> desiredPermissions);
}
