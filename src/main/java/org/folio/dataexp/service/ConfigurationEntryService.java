package org.folio.dataexp.service;

import static com.github.jknack.handlebars.internal.lang3.StringUtils.EMPTY;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.client.ConfigurationEntryClient;
import org.folio.dataexp.domain.dto.ConfigurationEntry;
import org.springframework.stereotype.Service;

/** Service for retrieving configuration entries from remote sources. */
@RequiredArgsConstructor
@Log4j2
@Service
public class ConfigurationEntryService {

  private final ConfigurationEntryClient configurationEntryClient;

  /**
   * Retrieves a single configuration entry by CQL query.
   *
   * @param query CQL query string.
   * @return The first matching ConfigurationEntry, or an empty value if none found.
   */
  public ConfigurationEntry retrieveSingleConfigurationEntryByQuery(String query) {
    return configurationEntryClient
        .getConfigurationEntryCollectionByQuery(query)
        .getConfigs()
        .stream()
        .findFirst()
        .orElse(new ConfigurationEntry().value(EMPTY));
  }
}
