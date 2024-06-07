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
import static org.folio.dataexp.util.FolioExecutionContextUtil.prepareContextForTenant;

import lombok.RequiredArgsConstructor;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.processor.referencedata.JsonObjectWrapper;
import org.folio.processor.referencedata.ReferenceDataWrapper;
import org.folio.processor.referencedata.ReferenceDataWrapperImpl;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
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
  private final ConsortiaService consortiaService;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;

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

  @Cacheable(cacheNames = "referenceDataForCentralTenantAndUserTenants")
  public ReferenceDataWrapper getReference(String centralTenant, String userId) {
    var userTenants = consortiaService.getAffiliatedTenants(centralTenant, userId);
    var referenceData = getReference(centralTenant);
    for (var userTenant : userTenants) {
      try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(userTenant, folioModuleMetadata, folioExecutionContext))) {
        var userTenantReferenceData = getReference(userTenant);
        putIfNotExist(referenceData.get(ALTERNATIVE_TITLE_TYPES), userTenantReferenceData.get(ALTERNATIVE_TITLE_TYPES));
        putIfNotExist(referenceData.get(CONTENT_TERMS), userTenantReferenceData.get(CONTENT_TERMS));
        putIfNotExist(referenceData.get(IDENTIFIER_TYPES), userTenantReferenceData.get(IDENTIFIER_TYPES));
        putIfNotExist(referenceData.get(CONTRIBUTOR_NAME_TYPES), userTenantReferenceData.get(CONTRIBUTOR_NAME_TYPES));
        putIfNotExist(referenceData.get(LOCATIONS), userTenantReferenceData.get(LOCATIONS));
        putIfNotExist(referenceData.get(LOAN_TYPES), userTenantReferenceData.get(LOAN_TYPES));
        putIfNotExist(referenceData.get(LIBRARIES), userTenantReferenceData.get(LIBRARIES));
        putIfNotExist(referenceData.get(CAMPUSES), userTenantReferenceData.get(CAMPUSES));
        putIfNotExist(referenceData.get(INSTITUTIONS), userTenantReferenceData.get(INSTITUTIONS));
        putIfNotExist(referenceData.get(MATERIAL_TYPES), userTenantReferenceData.get(MATERIAL_TYPES));
        putIfNotExist(referenceData.get(INSTANCE_TYPES), userTenantReferenceData.get(INSTANCE_TYPES));
        putIfNotExist(referenceData.get(INSTANCE_FORMATS), userTenantReferenceData.get(INSTANCE_FORMATS));
        putIfNotExist(referenceData.get(ELECTRONIC_ACCESS_RELATIONSHIPS), userTenantReferenceData.get(ELECTRONIC_ACCESS_RELATIONSHIPS));
        putIfNotExist(referenceData.get(ISSUANCE_MODES), userTenantReferenceData.get(ISSUANCE_MODES));
        putIfNotExist(referenceData.get(CALL_NUMBER_TYPES), userTenantReferenceData.get(CALL_NUMBER_TYPES));
      }
    }
    return referenceData;
  }

  private void putIfNotExist(Map<String,JsonObjectWrapper> target, Map<String,JsonObjectWrapper> source) {
    var tmp = new HashMap<>(source);
    tmp.keySet().removeAll(target.keySet());
    target.putAll(tmp);
  }
}
