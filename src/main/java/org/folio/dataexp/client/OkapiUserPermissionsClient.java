package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.UserPermissions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving user permissions from Okapi. */
@HttpExchange(url = "perms/users")
public interface OkapiUserPermissionsClient {

  /**
   * Retrieves user permissions for a given user.
   *
   * @param userId the user ID
   * @return the user permissions
   */
  @GetExchange(
      value = "/{userId}/permissions?expanded=true&indexField=userId",
      accept = MediaType.APPLICATION_JSON_VALUE)
  UserPermissions getPermissions(@PathVariable String userId);
}
