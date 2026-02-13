package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.ConfigurationEntryCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for retrieving configuration entries. */
@FeignClient(name = "configurations")
public interface ConfigurationEntryClient {

  /**
   * Retrieves configuration entries matching the specified query.
   *
   * @param query the CQL query string
   * @return a collection of configuration entries
   */
  @GetMapping(path = "/entries", produces = MediaType.APPLICATION_JSON_VALUE)
  ConfigurationEntryCollection getConfigurationEntryCollectionByQuery(@RequestParam String query);
}
