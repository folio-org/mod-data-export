package org.folio.dataexp.config;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignRetryConfig {

  @Bean
  public Retryer feignRetryer() {
    // 500 ms initial backoff, 5 s max, 3 total attempts (2 retries)
    return new Retryer.Default(500L, TimeUnit.SECONDS.toMillis(5), 3);
  }

  @Bean
  public ErrorDecoder feignErrorDecoder() {
    return new FeignRetryableErrorDecoder();
  }
}
