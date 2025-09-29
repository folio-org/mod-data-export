package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.Config;
import org.folio.dataexp.domain.dto.ConfigurationEntry;
import org.folio.dataexp.domain.entity.ConfigurationEntity;
import org.folio.dataexp.repository.ConfigurationRepository;
import org.folio.dataexp.service.validators.ConfigurationValidator;
import org.springframework.stereotype.Service;

/**
 * Service for managing configuration values in the data export module.
 */
@RequiredArgsConstructor
@Log4j2
@Service
public class ConfigurationService {

  /** Key for inventory record link configuration. */
  public static final String INVENTORY_RECORD_LINK_KEY = "inventory_record_link";
  private static final String QUERY_BY_FOLIO_HOST = "code=\"FOLIO_HOST\"";

  private final ConfigurationRepository configurationRepository;
  private final ConfigurationValidator configurationValidator;
  private final ConfigurationEntryService configurationEntryService;

  /**
   * Inserts or updates a configuration value.
   *
   * @param config The configuration to upsert.
   * @return The saved configuration.
   */
  public Config upsertConfiguration(Config config) {
    log.info("Upserting configuration by id {}", config.getKey());
    configurationValidator.validate(config);
    var entity = ConfigurationEntity.builder()
        .key(config.getKey())
        .value(config.getValue()).build();
    var saved = configurationRepository.save(entity);
    log.info("Upserted successfully: {}", saved.getValue());
    return new Config().key(saved.getKey()).value(saved.getValue());
  }

  /**
   * Produces the inventory record link based on the FOLIO_HOST config from remote.
   *
   * @return Config containing the inventory record link.
   */
  public Config produceInventoryRecordLinkBasedOnFolioHostConfigFromRemote() {
    log.info("Producing the inventory record link.");
    ConfigurationEntry entryFromRemote = configurationEntryService
        .retrieveSingleConfigurationEntryByQuery(QUERY_BY_FOLIO_HOST);
    var folioHostValueFromRemote = entryFromRemote.getValue();
    var inventoryRecordLinkValue = String.join(
        "",
        (folioHostValueFromRemote.endsWith("/") ? folioHostValueFromRemote
            : folioHostValueFromRemote.concat("/")),
        "inventory/view/"
    );

    return Config.builder()
      .key(INVENTORY_RECORD_LINK_KEY)
      .value(inventoryRecordLinkValue)
      .build();
  }

  /**
   * Gets the value for a given configuration key.
   *
   * @param key The configuration key.
   * @return The configuration value.
   */
  public String getValue(String key) {
    return configurationRepository.getReferenceById(key).getValue();
  }
}
