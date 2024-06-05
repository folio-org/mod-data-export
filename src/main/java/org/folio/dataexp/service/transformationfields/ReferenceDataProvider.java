package org.folio.dataexp.service.transformationfields;

import static org.folio.dataexp.util.ExternalPathResolver.ALTERNATIVE_TITLE_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.CALL_NUMBER_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.CAMPUSES;
import static org.folio.dataexp.util.ExternalPathResolver.CONTENT_TERMS;
import static org.folio.dataexp.util.ExternalPathResolver.CONTRIBUTOR_NAME_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.ELECTRONIC_ACCESS_RELATIONSHIPS;
import static org.folio.dataexp.util.ExternalPathResolver.HOLDING_NOTE_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.IDENTIFIER_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.INSTANCE_FORMATS;
import static org.folio.dataexp.util.ExternalPathResolver.INSTANCE_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.INSTITUTIONS;
import static org.folio.dataexp.util.ExternalPathResolver.ISSUANCE_MODES;
import static org.folio.dataexp.util.ExternalPathResolver.ITEM_NOTE_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.LIBRARIES;
import static org.folio.dataexp.util.ExternalPathResolver.LOAN_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.LOCATIONS;
import static org.folio.dataexp.util.ExternalPathResolver.MATERIAL_TYPES;

import lombok.RequiredArgsConstructor;
import org.folio.processor.referencedata.JsonObjectWrapper;
import org.folio.processor.referencedata.ReferenceDataWrapper;
import org.folio.processor.referencedata.ReferenceDataWrapperImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * The component is responsible to provide reference data.
 * Caches data for the given job id.
 * If requested data is not found by the given job id then performs loading.
 */
@Component
@RequiredArgsConstructor
public class ReferenceDataProvider {
  private final ReferenceDataService referenceDataService;

  /**
   * This method returns the reference data that is required for generating the transformation fields during the call for
   * /transformation-fields API
   */
  @Cacheable(cacheNames = "referenceDataForTransformationFields")
  public ReferenceDataWrapper getReferenceDataForTransformationFields(String tenantId) {
    HashMap<String, Map<String, JsonObjectWrapper>> map = new HashMap<>() ;
    map.put(ALTERNATIVE_TITLE_TYPES, referenceDataService.getAlternativeTitleTypes());
    map.put(CONTRIBUTOR_NAME_TYPES, referenceDataService.getContributorNameTypes());
    map.put(ELECTRONIC_ACCESS_RELATIONSHIPS, referenceDataService.getElectronicAccessRelationships());
    map.put(INSTANCE_TYPES, referenceDataService.getInstanceTypes());
    map.put(IDENTIFIER_TYPES, referenceDataService.getIdentifierTypes());
    map.put(ISSUANCE_MODES, referenceDataService.getIssuanceModes());
    map.put(HOLDING_NOTE_TYPES, referenceDataService.getHoldingsNoteTypes());
    map.put(ITEM_NOTE_TYPES, referenceDataService.getItemNoteTypes());
    return new ReferenceDataWrapperImpl(map);
  }

  /**
   * This methods returns the reference data that is needed to map the fields to MARC , while generating marc records on the fly
   */
  @Cacheable(cacheNames = "referenceData")
  public ReferenceDataWrapper getReference(String tenantId) {
    HashMap<String, Map<String, JsonObjectWrapper>> map = new HashMap<>() ;
    map.put(ALTERNATIVE_TITLE_TYPES, referenceDataService.getAlternativeTitleTypes());
    map.put(CONTENT_TERMS, referenceDataService.getNatureOfContentTerms());
    map.put(IDENTIFIER_TYPES, referenceDataService.getIdentifierTypes());
    map.put(CONTRIBUTOR_NAME_TYPES, referenceDataService.getContributorNameTypes());
    map.put(LOCATIONS, referenceDataService.getLocations());
    map.put(LOAN_TYPES, referenceDataService.getLoanTypes());
    map.put(LIBRARIES, referenceDataService.getLibraries());
    map.put(CAMPUSES, referenceDataService.getCampuses());
    map.put(INSTITUTIONS, referenceDataService.getInstitutions());
    map.put(MATERIAL_TYPES, referenceDataService.getMaterialTypes());
    map.put(INSTANCE_TYPES, referenceDataService.getInstanceTypes());
    map.put(INSTANCE_FORMATS, referenceDataService.getInstanceFormats());
    map.put(ELECTRONIC_ACCESS_RELATIONSHIPS, referenceDataService.getElectronicAccessRelationships());
    map.put(ISSUANCE_MODES, referenceDataService.getIssuanceModes());
    map.put(CALL_NUMBER_TYPES, referenceDataService.getCallNumberTypes());
    return new ReferenceDataWrapperImpl(map);
  }
}
