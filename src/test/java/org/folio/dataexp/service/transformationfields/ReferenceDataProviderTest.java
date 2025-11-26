package org.folio.dataexp.service.transformationfields;

import static org.folio.dataexp.util.ExternalPathResolver.LOCATIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
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
}
