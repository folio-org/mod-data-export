package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.User;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/** Retryable wrapper around {@link UserClient}. */
@Service
@RequiredArgsConstructor
public class RetryableUserClient {

  private final UserClient userClient;

  /** Get user by id. */
  @Retryable(
      maxAttemptsExpression = "${application.feign-client-retry.max-attempts}",
      backoff = @Backoff(
          delayExpression = "${application.feign-client-retry.initial-wait-time}",
          maxDelayExpression = "${application.feign-client-retry.max-wait-time}",
          multiplier = 2.0
      )
  )
  public User getUserById(String userId) {
    return userClient.getUserById(userId);
  }
}
