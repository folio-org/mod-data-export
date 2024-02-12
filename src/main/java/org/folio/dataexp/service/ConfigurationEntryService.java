package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.client.ConfigurationEntryClient;
import org.folio.dataexp.domain.dto.ConfigurationEntry;
import org.folio.spring.exception.NotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Log4j2
@Service
public class ConfigurationEntryService {

  private final ConfigurationEntryClient configurationEntryClient;

  public ConfigurationEntry retrieveSingleConfigurationEntryByQuery(String query){
    return configurationEntryClient.getConfigurationEntryCollectionByQuery(query)
      .getConfigs()
      .stream()
      .findFirst()
      .orElseThrow(() -> new NotFoundException(String.format("The config entry wasn't found by query = %s ", query)));
  }


}
