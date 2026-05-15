package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.BaseUrl;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Http client for fetching the tenant-specific FOLIO base URL. */
@HttpExchange(url = "base-url")
public interface SettingsBaseUrlClient {
  /**
   * Fetches base URL from the base-url service.
   *
   * @return base URL payload
   */
  @GetExchange
  BaseUrl getBaseUrl();
}
