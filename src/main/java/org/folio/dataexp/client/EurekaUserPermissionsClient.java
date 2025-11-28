package org.folio.dataexp.client;

import java.util.List;
import org.folio.dataexp.domain.dto.UserPermissions;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for retrieving user permissions from Eureka. */
@FeignClient(name = "users-keycloak/users", configuration = FeignClientConfiguration.class)
public interface EurekaUserPermissionsClient {

  /**
   * Retrieves user permissions for a given user and desired permissions.
   *
   * @param userId the user ID
   * @param desiredPermissions the list of desired permissions
   * @return the user permissions
   */
  @GetMapping(value = "/{userId}/permissions", produces = MediaType.APPLICATION_JSON_VALUE)
  UserPermissions getPermissions(
      @PathVariable String userId, @RequestParam List<String> desiredPermissions);
}
