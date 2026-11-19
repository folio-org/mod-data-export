package org.folio.dataexp.service.export.strategies;

import lombok.AllArgsConstructor;
import org.folio.dataexp.service.RetryableUserClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/** Service for retrieving user information. */
@Service
@AllArgsConstructor
public class UserService {
  private final RetryableUserClient userClient;

  /** Gets the username for the given tenant and user ID. */
  @Retryable(
      maxAttemptsExpression = "${application.feign-client-retry.max-attempts}",
      backoff = @Backoff(
          delayExpression = "${application.feign-client-retry.initial-wait-time}",
          maxDelayExpression = "${application.feign-client-retry.max-wait-time}",
          multiplier = 2.0
      )
  )
  @Cacheable(value = "userNameCache")
  public String getUserName(String tenant, String userId) {
    return userClient.getUserById(userId).getUsername();
  }
}
