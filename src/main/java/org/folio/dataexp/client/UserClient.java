package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.User;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving user information. */
@HttpExchange(url = "users")
public interface UserClient {

  /**
   * Retrieves a user by their user ID.
   *
   * @param userId the user ID
   * @return the user
   */
  @GetExchange(value = "/{userId}", accept = MediaType.APPLICATION_JSON_VALUE)
  User getUserById(@PathVariable String userId);
}
