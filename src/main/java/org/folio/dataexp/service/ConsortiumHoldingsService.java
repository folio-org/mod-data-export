package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import org.folio.dataexp.client.ConsortiumSearchClient;
import org.folio.dataexp.domain.dto.Holdings;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/** Retryable wrapper around {@link ConsortiumSearchClient} for holdings lookups. */
@Service
@RequiredArgsConstructor
public class ConsortiumHoldingsService {

  private final ConsortiumSearchClient consortiumSearchClient;


  /** Get holdings record by id */
  @Retryable(
      maxAttemptsExpression = "${application.feign-client-retry.max-attempts}",
      backoff = @Backoff(
          delayExpression = "${application.feign-client-retry.initial-wait-time}",
          maxDelayExpression = "${application.feign-client-retry.max-wait-time}",
          multiplier = 2.0
      )
  )
  public Holdings getHoldingsById(String holdingId) {
    return consortiumSearchClient.getHoldingsById(holdingId);
  }
}
