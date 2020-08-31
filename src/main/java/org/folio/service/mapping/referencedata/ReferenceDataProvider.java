package org.folio.service.mapping.referencedata;

import org.folio.clients.InventoryClient;
import org.folio.processor.ReferenceData;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.folio.service.mapping.referencedata.ReferenceDataImpl.ALTERNATIVE_TITLE_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.CONTRIBUTOR_NAME_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.ELECTRONIC_ACCESS_RELATIONSHIPS;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.IDENTIFIER_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.INSTANCE_FORMATS;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.INSTANCE_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.LOAN_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.LOCATIONS;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.MATERIAL_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.MODES_OF_ISSUANCE;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.NATURE_OF_CONTENT_TERMS;
import static org.folio.util.ExternalPathResolver.HOLDING_NOTE_TYPES;
import static org.folio.util.ExternalPathResolver.ITEM_NOTE_TYPES;

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

  public ReferenceData getReferenceDataForTransformationFields(OkapiConnectionParams okapiConnectionParams) {
    ReferenceDataImpl referenceData = new ReferenceDataImpl();
    referenceData.put(ALTERNATIVE_TITLE_TYPES, inventoryClient.getAlternativeTitleTypes(okapiConnectionParams));
    referenceData.put(CONTRIBUTOR_NAME_TYPES, inventoryClient.getContributorNameTypes(okapiConnectionParams));
    referenceData.put(ELECTRONIC_ACCESS_RELATIONSHIPS, inventoryClient.getElectronicAccessRelationships(okapiConnectionParams));
    referenceData.put(INSTANCE_TYPES, inventoryClient.getInstanceTypes(okapiConnectionParams));
    referenceData.put(IDENTIFIER_TYPES, inventoryClient.getIdentifierTypes(okapiConnectionParams));
    referenceData.put(MODES_OF_ISSUANCE, inventoryClient.getModesOfIssuance(okapiConnectionParams));
    referenceData.put(HOLDING_NOTE_TYPES, inventoryClient.getHoldingsNoteTypes(okapiConnectionParams));
    referenceData.put(ITEM_NOTE_TYPES, inventoryClient.getItemNoteTypes(okapiConnectionParams));
    return referenceData;
  }

  private ReferenceDataImpl load(OkapiConnectionParams okapiConnectionParams) {
    ReferenceDataImpl referenceData = new ReferenceDataImpl();
    referenceData.put(NATURE_OF_CONTENT_TERMS, inventoryClient.getNatureOfContentTerms(okapiConnectionParams));
    referenceData.put(IDENTIFIER_TYPES, inventoryClient.getIdentifierTypes(okapiConnectionParams));
    referenceData.put(CONTRIBUTOR_NAME_TYPES, inventoryClient.getContributorNameTypes(okapiConnectionParams));
    referenceData.put(LOCATIONS, inventoryClient.getLocations(okapiConnectionParams));
    referenceData.put(LOAN_TYPES, inventoryClient.getLoanTypes(okapiConnectionParams));
    referenceData.put(MATERIAL_TYPES, inventoryClient.getMaterialTypes(okapiConnectionParams));
    referenceData.put(INSTANCE_TYPES, inventoryClient.getInstanceTypes(okapiConnectionParams));
    referenceData.put(INSTANCE_FORMATS, inventoryClient.getInstanceFormats(okapiConnectionParams));
    referenceData.put(ELECTRONIC_ACCESS_RELATIONSHIPS, inventoryClient.getElectronicAccessRelationships(okapiConnectionParams));
    referenceData.put(MODES_OF_ISSUANCE, inventoryClient.getModesOfIssuance(okapiConnectionParams));
    return referenceData;
  }
}
