package org.folio.dataexp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  @Primary
  public Caffeine caffeineConfig() { //NOSONAR
    return Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS);
  }

  @Bean
  public Caffeine noExpireCaffeineConfig() { //NOSONAR
    return Caffeine.newBuilder();
  }

  @Bean
  @Primary
  public CacheManager cacheManager(Caffeine caffeine) { //NOSONAR
    CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
    caffeineCacheManager.setCaffeine(caffeine);
    return caffeineCacheManager;
  }

  @Bean
  public CacheManager cacheManagerPerExport(@Qualifier("noExpireCaffeineConfig") Caffeine caffeine) { //NOSONAR
    CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
    caffeineCacheManager.setCaffeine(caffeine);
    return caffeineCacheManager;
  }

}
