package org.folio.dataexp.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Additional configuration for the FQM Feign client, adding a retryer. */
@Configuration
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

}
