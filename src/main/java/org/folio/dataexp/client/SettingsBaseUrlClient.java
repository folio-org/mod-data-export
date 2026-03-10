package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.BaseUrl;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/** Feign client for fetching the tenant-specific FOLIO base URL. */
@FeignClient(name = "base-url")
public interface SettingsBaseUrlClient {
  /**
   * Fetches base URL from the base-url service.
   *
   * @return base URL payload
   */
  @GetMapping
  BaseUrl getBaseUrl();
}
