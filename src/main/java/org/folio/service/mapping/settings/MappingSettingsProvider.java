package org.folio.service.mapping.settings;

import org.folio.clients.InventoryClient;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The component is responsible to provide settings and parameters needed for mapping.
 * Caches settings for the given job id.
 * If requested settings are not found by the given job id then performs loading.
 */
@Component
public class MappingSettingsProvider {
  private static final int CACHE_EXPIRATION_AFTER_ACCESS_SECONDS = 60;
  private Cache<String, Settings> cache;
  private InventoryClient inventoryClient;

  public MappingSettingsProvider(@Autowired InventoryClient inventoryClient) {
    this.inventoryClient = inventoryClient;
    this.cache = new Cache<>(CACHE_EXPIRATION_AFTER_ACCESS_SECONDS);
  }

  public Settings getSettings(String jobExecutionId, OkapiConnectionParams okapiConnectionParams) {
    Settings cachedSettings = this.cache.get(jobExecutionId);
    if (cachedSettings == null) {
      Settings loadedSettings = loadSettings(okapiConnectionParams);
      this.cache.put(jobExecutionId, loadedSettings);
      return loadedSettings;
    } else {
      return cachedSettings;
    }
  }

  private Settings loadSettings(OkapiConnectionParams okapiConnectionParams) {
    Settings settings = new Settings();
    settings.addNatureOfContentTerms(inventoryClient.getNatureOfContentTerms(okapiConnectionParams));
    return settings;
  }
}
