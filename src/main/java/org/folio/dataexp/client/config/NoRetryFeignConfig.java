package org.folio.dataexp.client.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

public class NoRetryFeignConfig {

  @Bean
  @Primary
  public Retryer noRetryRetryer() {
    return Retryer.NEVER_RETRY;
  }
}
