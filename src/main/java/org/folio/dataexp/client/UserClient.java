package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for retrieving user information.
 */
@FeignClient(name = "users")
public interface UserClient {

  /**
   * Retrieves a user by their user ID.
   *
   * @param userId the user ID
   * @return the user
   */
  @GetMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
  User getUserById(@PathVariable String userId);

}
