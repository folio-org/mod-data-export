package org.folio.dataexp.config;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.folio.dataexp.util.FqmErrorDecoder;
import org.springframework.context.annotation.Bean;

/** Additional configuration for the FQM Feign client, adding a retryer. */
public class QueryClientConfiguration {
  
  /**
   * Add retries based on the Feign Retryer, starting at a 5s delay and
   * increasing exponentially between subsequent retries. This should try
   * again for about 2 minutes before failing.
   * 
   * @return Retry algorithm for use with Feign client
   */
  @Bean
  public Retryer retryer() {
    return new Retryer.Default(5000, 30000, 7);
  }

  /**
   * Use a custom error decoder to interpret additional HTTP error codes as
   * retryable beyond the default for HTTP 503.
   *
   * @return Feign error decoder
   */
  @Bean
  public ErrorDecoder errorDecoder() {
    return new FqmErrorDecoder();
  }
}
