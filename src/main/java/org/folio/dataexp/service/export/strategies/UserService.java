package org.folio.dataexp.service.export.strategies;

import lombok.AllArgsConstructor;
import org.folio.dataexp.client.UserClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service for retrieving user information.
 */
@Service
@AllArgsConstructor
public class UserService {
  private final UserClient userClient;

  /**
   * Gets the username for the given tenant and user ID.
   */
  @Cacheable(value = "userNameCache")
  public String getUserName(String tenant, String userId) {
    return userClient.getUserById(userId).getUsername();
  }
}
