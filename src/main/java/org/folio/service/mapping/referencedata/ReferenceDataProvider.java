package org.folio.service.mapping.referencedata;

import org.folio.clients.InventoryClient;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The component is responsible to provide reference data.
 * Caches data for the given job id.
 * If requested data is not found by the given job id then performs loading.
 */
@Component
public class ReferenceDataProvider {
  private static final int CACHE_EXPIRATION_AFTER_ACCESS_SECONDS = 60;
  private Cache<String, ReferenceData> cache;
  private InventoryClient inventoryClient;

  public ReferenceDataProvider(@Autowired InventoryClient inventoryClient) {
    this.inventoryClient = inventoryClient;
    this.cache = new Cache<>(CACHE_EXPIRATION_AFTER_ACCESS_SECONDS);
  }

  public ReferenceData get(String jobExecutionId, OkapiConnectionParams okapiConnectionParams) {
    ReferenceData cached = this.cache.get(jobExecutionId);
    if (cached == null) {
      ReferenceData loaded = load(okapiConnectionParams);
      this.cache.put(jobExecutionId, loaded);
      return loaded;
    } else {
      return cached;
    }
  }

  private ReferenceData load(OkapiConnectionParams okapiConnectionParams) {
    ReferenceData referenceData = new ReferenceData();
    referenceData.addNatureOfContentTerms(inventoryClient.getNatureOfContentTerms(okapiConnectionParams));
    referenceData.addIdentifierTypes(inventoryClient.getIdentifierTypes(okapiConnectionParams));
    referenceData.addContributorNameTypes(inventoryClient.getContributorNameTypes(okapiConnectionParams));
    referenceData.addLocations(inventoryClient.getLocations(okapiConnectionParams));
    return referenceData;
  }
}
