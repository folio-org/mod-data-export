package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.ConfigurationEntryCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "configurations")
public interface ConfigurationEntryClient {

  @GetMapping(path = "/entries",produces = MediaType.APPLICATION_JSON_VALUE)
  ConfigurationEntryCollection getConfigurationEntryCollectionByQuery(@RequestParam String query);
}
