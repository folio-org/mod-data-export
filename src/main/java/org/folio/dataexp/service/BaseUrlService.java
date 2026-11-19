package org.folio.dataexp.service;

import lombok.AllArgsConstructor;
import org.folio.dataexp.client.SettingsBaseUrlClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/** Service for retrieving and caching the FOLIO base URL per tenant. */
@Service
@AllArgsConstructor
public class BaseUrlService {
  private final SettingsBaseUrlClient settingsBaseUrlClient;

  /**
   * Returns the base URL for the current tenant.
   *
   * @return tenant-specific base URL
   */
  @Retryable(
      maxAttemptsExpression = "${application.feign-client-retry.max-attempts}",
      backoff = @Backoff(
          delayExpression = "${application.feign-client-retry.initial-wait-time}",
          maxDelayExpression = "${application.feign-client-retry.max-wait-time}",
          multiplier = 2.0
      )
  )
  @Cacheable(value = "baseUrl", key = "@folioExecutionContext.tenantId + '_base-url'")
  public String getBaseUrl() {
    return settingsBaseUrlClient.getBaseUrl().getBaseUrl();
  }
}
