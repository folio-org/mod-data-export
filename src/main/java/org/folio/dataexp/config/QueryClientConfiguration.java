package org.folio.dataexp.config;

import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/** Additional configuration for the FQM Feign client, adding a retryer. */
public class QueryClientConfiguration {

  @Value("${application.feign-query-client-retry.initial-wait-time}")
  private int initialWaitTime;

  @Value("${application.feign-query-client-retry.max-wait-time}")
  private int maxWaitTime;

  @Value("${application.feign-query-client-retry.max-attempts}")
  private int maxAttempts;

  /**
   * Add retries based on the Feign Retryer. See environment variables for configuring initial time
   * to wait before retrying, maximum wait time to allow as subsequent waits increase through
   * exponential backoff, and maximum number of attempts before considering the request failed.
   * Defaults of 5s, 30s, and 7 attempts should grant about 2 minutes to allow FQM to resume
   * operations.
   *
   * @return Retry algorithm for use with Feign client
   */
  @Bean
  public Retryer retryer() {
    return new Retryer.Default(initialWaitTime, maxWaitTime, maxAttempts);
  }
}
