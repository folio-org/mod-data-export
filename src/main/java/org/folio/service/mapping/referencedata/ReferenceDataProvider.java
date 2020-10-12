package org.folio.service.mapping.referencedata;

import org.folio.clients.InventoryClient;
import org.folio.processor.ReferenceData;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.util.ExternalPathResolver.ALTERNATIVE_TITLE_TYPES;
import static org.folio.util.ExternalPathResolver.CONTRIBUTOR_NAME_TYPES;
import static org.folio.util.ExternalPathResolver.ELECTRONIC_ACCESS_RELATIONSHIPS;
import static org.folio.util.ExternalPathResolver.INSTANCE_TYPES;
import static org.folio.util.ExternalPathResolver.IDENTIFIER_TYPES;
import static org.folio.util.ExternalPathResolver.ISSUANCE_MODES;
import static org.folio.util.ExternalPathResolver.HOLDING_NOTE_TYPES;
import static org.folio.util.ExternalPathResolver.ITEM_NOTE_TYPES;
import static org.folio.util.ExternalPathResolver.CONTENT_TERMS;
import static org.folio.util.ExternalPathResolver.LOCATIONS;
import static org.folio.util.ExternalPathResolver.LOAN_TYPES;
import static org.folio.util.ExternalPathResolver.LIBRARIES;
import static org.folio.util.ExternalPathResolver.CAMPUSES;
import static org.folio.util.ExternalPathResolver.INSTITUTIONS;
import static org.folio.util.ExternalPathResolver.MATERIAL_TYPES;
import static org.folio.util.ExternalPathResolver.INSTANCE_FORMATS;
import static org.folio.util.ExternalPathResolver.CALL_NUMBER_TYPES;

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
      ReferenceData loaded = load(jobExecutionId, okapiConnectionParams);
      this.cache.put(jobExecutionId, loaded);
      return loaded;
    } else {
      return cached;
    }
  }

  /**
   * This method returns the reference data that is required for generating the transformation fields during the call for
   * /transformation-fields API
   *
   * @param okapiConnectionParams
   */
  public ReferenceData getReferenceDataForTransformationFields(OkapiConnectionParams okapiConnectionParams) {
    ReferenceDataImpl referenceData = new ReferenceDataImpl();
    referenceData.put(ALTERNATIVE_TITLE_TYPES, inventoryClient.getAlternativeTitleTypes(EMPTY, okapiConnectionParams));
    referenceData.put(CONTRIBUTOR_NAME_TYPES, inventoryClient.getContributorNameTypes(EMPTY, okapiConnectionParams));
    referenceData.put(ELECTRONIC_ACCESS_RELATIONSHIPS, inventoryClient.getElectronicAccessRelationships(EMPTY, okapiConnectionParams));
    referenceData.put(INSTANCE_TYPES, inventoryClient.getInstanceTypes(EMPTY, okapiConnectionParams));
    referenceData.put(IDENTIFIER_TYPES, inventoryClient.getIdentifierTypes(EMPTY, okapiConnectionParams));
    referenceData.put(ISSUANCE_MODES, inventoryClient.getModesOfIssuance(EMPTY, okapiConnectionParams));
    referenceData.put(HOLDING_NOTE_TYPES, inventoryClient.getHoldingsNoteTypes(EMPTY, okapiConnectionParams));
    referenceData.put(ITEM_NOTE_TYPES, inventoryClient.getItemNoteTypes(EMPTY, okapiConnectionParams));
    return referenceData;
  }

  /**
   * This methods returns the reference data that is needed to map the fields to MARC , while generating marc records on the fly
   *
   * @param okapiConnectionParams
   */
  private ReferenceDataImpl load(String jobExecutionId, OkapiConnectionParams okapiConnectionParams) {
    ReferenceDataImpl referenceData = new ReferenceDataImpl();
    referenceData.put(CONTENT_TERMS, inventoryClient.getNatureOfContentTerms(jobExecutionId, okapiConnectionParams));
    referenceData.put(IDENTIFIER_TYPES, inventoryClient.getIdentifierTypes(jobExecutionId, okapiConnectionParams));
    referenceData.put(CONTRIBUTOR_NAME_TYPES, inventoryClient.getContributorNameTypes(jobExecutionId, okapiConnectionParams));
    referenceData.put(LOCATIONS, inventoryClient.getLocations(jobExecutionId, okapiConnectionParams));
    referenceData.put(LOAN_TYPES, inventoryClient.getLoanTypes(jobExecutionId, okapiConnectionParams));
    referenceData.put(LIBRARIES, inventoryClient.getLibraries(jobExecutionId, okapiConnectionParams));
    referenceData.put(CAMPUSES, inventoryClient.getCampuses(jobExecutionId, okapiConnectionParams));
    referenceData.put(INSTITUTIONS, inventoryClient.getInstitutions(jobExecutionId, okapiConnectionParams));
    referenceData.put(MATERIAL_TYPES, inventoryClient.getMaterialTypes(jobExecutionId, okapiConnectionParams));
    referenceData.put(INSTANCE_TYPES, inventoryClient.getInstanceTypes(jobExecutionId, okapiConnectionParams));
    referenceData.put(INSTANCE_FORMATS, inventoryClient.getInstanceFormats(jobExecutionId, okapiConnectionParams));
    referenceData.put(ELECTRONIC_ACCESS_RELATIONSHIPS, inventoryClient.getElectronicAccessRelationships(jobExecutionId, okapiConnectionParams));
    referenceData.put(ISSUANCE_MODES, inventoryClient.getModesOfIssuance(jobExecutionId, okapiConnectionParams));
    referenceData.put(CALL_NUMBER_TYPES, inventoryClient.getCallNumberTypes(jobExecutionId, okapiConnectionParams));
    return referenceData;
  }
}
