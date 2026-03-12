package org.folio.dataexp.service;

import lombok.AllArgsConstructor;
import org.folio.dataexp.client.SettingsBaseUrlClient;
import org.springframework.cache.annotation.Cacheable;
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
  @Cacheable(value = "baseUrl", key = "@folioExecutionContext.tenantId + '_base-url'")
  public String getBaseUrl() {
    return settingsBaseUrlClient.getBaseUrl().getBaseUrl();
  }
}
