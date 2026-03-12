package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.ConfigurationEntryCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving configuration entries. */
@HttpExchange(url = "configurations")
public interface ConfigurationEntryClient {

  /**
   * Retrieves configuration entries matching the specified query.
   *
   * @param query the CQL query string
   * @return a collection of configuration entries
   */
  @GetExchange(value = "/entries", accept = MediaType.APPLICATION_JSON_VALUE)
  ConfigurationEntryCollection getConfigurationEntryCollectionByQuery(@RequestParam String query);
}
