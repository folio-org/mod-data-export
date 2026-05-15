package org.folio.dataexp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration class for setting up Caffeine cache. */
@Configuration
@EnableCaching
public class CacheConfig {

  /**
   * Configures the Caffeine cache with an expiration policy.
   *
   * @return a configured Caffeine instance
   */
  @Bean
  public Caffeine caffeineConfig() { // NOSONAR
    return Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS);
  }

  /**
   * Configures the CacheManager to use Caffeine.
   *
   * @param caffeine the Caffeine instance to use
   * @return a configured CacheManager
   */
  @Bean
  public CacheManager cacheManager(Caffeine caffeine) { // NOSONAR
    CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
    caffeineCacheManager.setCaffeine(caffeine);
    return caffeineCacheManager;
  }
}
