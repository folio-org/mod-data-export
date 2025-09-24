package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.UserPermissions;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for retrieving user permissions from Okapi.
 */
@FeignClient(name = "perms/users", configuration = FeignClientConfiguration.class)
public interface OkapiUserPermissionsClient {

  /**
   * Retrieves user permissions for a given user.
   *
   * @param userId the user ID
   * @return the user permissions
   */
  @GetMapping(value = "/{userId}/permissions?expanded=true&indexField=userId",
      produces = MediaType.APPLICATION_JSON_VALUE)
  UserPermissions getPermissions(@PathVariable String userId);
}
