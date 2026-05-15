package org.folio.dataexp.service.transformationfields;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.util.ExternalPathResolver.ALTERNATIVE_TITLE_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.CONTRIBUTOR_NAME_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.ELECTRONIC_ACCESS_RELATIONSHIPS;
import static org.folio.dataexp.util.ExternalPathResolver.HOLDING_NOTE_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.IDENTIFIER_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.INSTANCE_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.ISSUANCE_MODES;
import static org.folio.dataexp.util.ExternalPathResolver.ITEM_NOTE_TYPES;
import static org.folio.dataexp.util.ExternalPathResolver.LOCATIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.processor.referencedata.JsonObjectWrapper;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReferenceDataProviderTest {

  @Mock private ReferenceDataService referenceDataService;
  @Mock private ConsortiaService consortiaService;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private FolioModuleMetadata folioModuleMetadata;
  @InjectMocks private ReferenceDataProvider referenceDataProvider;

  @Test
  void getReferenceForCentralAndUsersTenantsTest() {
    var okapiHeaders = new HashMap<String, Collection<String>>();
    okapiHeaders.put("header", List.of("value"));

    var locationCentralId = UUID.randomUUID().toString();
    var locationCentral = new JsonObjectWrapper(new HashMap<>());
    var locationsCentralReferenceData = new HashMap<String, JsonObjectWrapper>();

    var locationMemberId = UUID.randomUUID().toString();
    var locationMember = new JsonObjectWrapper(new HashMap<>());
    var locationsMemberReferenceData = new HashMap<String, JsonObjectWrapper>();

    locationsCentralReferenceData.put(locationCentralId, locationCentral);
    locationsMemberReferenceData.put(locationMemberId, locationMember);

    var central = "central";
    var userId = UUID.randomUUID().toString();
    var userTenants = List.of("member");
    when(referenceDataService.getLocations())
        .thenReturn(locationsCentralReferenceData, locationsMemberReferenceData);
    when(consortiaService.getAffiliatedTenants(central, userId)).thenReturn(userTenants);
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);

    var referenceData = referenceDataProvider.getReference(central, userId);
    var locations = referenceData.get(LOCATIONS);
    assertEquals(2, locations.size());
  }

  @Test
  @TestMate(name = "TestMate-38225fe1eacdf2f408527664001b2431")
  void testGetReferenceDataForTransformationFieldsShouldReturnPopulatedWrapper() {
    // Given
    var tenantId = "diku";
    var altTitleTypeId = "11111111-1111-1111-1111-111111111111";
    var contributorTypeId = "22222222-2222-2222-2222-222222222222";
    var electronicAccessId = "33333333-3333-3333-3333-333333333333";
    var instanceTypeId = "44444444-4444-4444-4444-444444444444";
    var identifierTypeId = "55555555-5555-5555-5555-555555555555";
    var issuanceModeId = "66666666-6666-6666-6666-666666666666";
    var holdingsNoteTypeId = "77777777-7777-7777-7777-777777777777";
    var itemNoteTypeId = "88888888-8888-8888-8888-888888888888";
    var altTitleTypes = Map.of(altTitleTypeId, new JsonObjectWrapper(Map.of("name", "altTitle")));
    var contributorTypes =
        Map.of(contributorTypeId, new JsonObjectWrapper(Map.of("name", "contributor")));
    var electronicAccess =
        Map.of(electronicAccessId, new JsonObjectWrapper(Map.of("name", "electronic")));
    var instanceTypes = Map.of(instanceTypeId, new JsonObjectWrapper(Map.of("name", "instance")));
    var identifierTypes =
        Map.of(identifierTypeId, new JsonObjectWrapper(Map.of("name", "identifier")));
    var issuanceModes = Map.of(issuanceModeId, new JsonObjectWrapper(Map.of("name", "issuance")));
    var holdingsNoteTypes =
        Map.of(holdingsNoteTypeId, new JsonObjectWrapper(Map.of("name", "holdingsNote")));
    var itemNoteTypes = Map.of(itemNoteTypeId, new JsonObjectWrapper(Map.of("name", "itemNote")));
    when(referenceDataService.getAlternativeTitleTypes()).thenReturn(altTitleTypes);
    when(referenceDataService.getContributorNameTypes()).thenReturn(contributorTypes);
    when(referenceDataService.getElectronicAccessRelationships()).thenReturn(electronicAccess);
    when(referenceDataService.getInstanceTypes()).thenReturn(instanceTypes);
    when(referenceDataService.getIdentifierTypes()).thenReturn(identifierTypes);
    when(referenceDataService.getIssuanceModes()).thenReturn(issuanceModes);
    when(referenceDataService.getHoldingsNoteTypes()).thenReturn(holdingsNoteTypes);
    when(referenceDataService.getItemNoteTypes()).thenReturn(itemNoteTypes);
    // When
    var result = referenceDataProvider.getReferenceDataForTransformationFields(tenantId);
    // Then
    assertThat(result.get(ALTERNATIVE_TITLE_TYPES)).hasSize(1).containsKey(altTitleTypeId);
    assertThat(result.get(CONTRIBUTOR_NAME_TYPES)).hasSize(1).containsKey(contributorTypeId);
    assertThat(result.get(ELECTRONIC_ACCESS_RELATIONSHIPS))
        .hasSize(1)
        .containsKey(electronicAccessId);
    assertThat(result.get(INSTANCE_TYPES)).hasSize(1).containsKey(instanceTypeId);
    assertThat(result.get(IDENTIFIER_TYPES)).hasSize(1).containsKey(identifierTypeId);
    assertThat(result.get(ISSUANCE_MODES)).hasSize(1).containsKey(issuanceModeId);
    assertThat(result.get(HOLDING_NOTE_TYPES)).hasSize(1).containsKey(holdingsNoteTypeId);
    assertThat(result.get(ITEM_NOTE_TYPES)).hasSize(1).containsKey(itemNoteTypeId);
  }

  @Test
  @TestMate(name = "TestMate-4ab841877ce35b1af0e52bb7e5d978db")
  void testGetRefDataForTransformFieldsWhenServiceReturnsEmptyShouldReturnEmptyMapsInWrapper() {
    // Given
    var tenantId = "member_tenant";
    Map<String, JsonObjectWrapper> emptyMap = Collections.emptyMap();
    when(referenceDataService.getAlternativeTitleTypes()).thenReturn(emptyMap);
    when(referenceDataService.getContributorNameTypes()).thenReturn(emptyMap);
    when(referenceDataService.getElectronicAccessRelationships()).thenReturn(emptyMap);
    when(referenceDataService.getInstanceTypes()).thenReturn(emptyMap);
    when(referenceDataService.getIdentifierTypes()).thenReturn(emptyMap);
    when(referenceDataService.getIssuanceModes()).thenReturn(emptyMap);
    when(referenceDataService.getHoldingsNoteTypes()).thenReturn(emptyMap);
    when(referenceDataService.getItemNoteTypes()).thenReturn(emptyMap);
    // When
    var result = referenceDataProvider.getReferenceDataForTransformationFields(tenantId);
    // Then
    assertThat(result.get(ALTERNATIVE_TITLE_TYPES)).isEmpty();
    assertThat(result.get(CONTRIBUTOR_NAME_TYPES)).isEmpty();
    assertThat(result.get(ELECTRONIC_ACCESS_RELATIONSHIPS)).isEmpty();
    assertThat(result.get(INSTANCE_TYPES)).isEmpty();
    assertThat(result.get(IDENTIFIER_TYPES)).isEmpty();
    assertThat(result.get(ISSUANCE_MODES)).isEmpty();
    assertThat(result.get(HOLDING_NOTE_TYPES)).isEmpty();
    assertThat(result.get(ITEM_NOTE_TYPES)).isEmpty();
  }
}
