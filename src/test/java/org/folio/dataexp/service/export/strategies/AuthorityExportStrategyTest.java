package org.folio.dataexp.service.export.strategies;

import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.client.ConsortiaClient;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.UserTenant;
import org.folio.dataexp.domain.dto.UserTenantCollection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class AuthorityExportStrategyTest extends BaseDataExportInitializer {

  private final static UUID LOCAL_AUTHORITY_UUID = UUID.fromString("4a090b0f-9da3-40f1-ab17-33d6a1e3abae");
  private final static UUID CENTRAL_AUTHORITY_UUID = UUID.fromString("26be956e-98f2-11ee-b9d1-0242ac120002");
  private final static UUID LOCAL_MARC_AUTHORITY_UUID = UUID.fromString("17eed93e-f9e2-4cb2-a52b-e9155acfc119");
  private final static UUID CENTRAL_MARC_AUTHORITY_UUID = UUID.fromString("ed0ad74c-98f1-11ee-b9d1-0242ac120002");

  @Autowired
  private AuthorityExportStrategy authorityExportStrategy;

  @MockBean
  private ConsortiaClient consortiaClient;

  @Test
  void shouldReturnOneLocalRecord() {
    var localAuthorityIds = new HashSet<UUID>();
    localAuthorityIds.add(LOCAL_AUTHORITY_UUID);
    var mappingProfile = new MappingProfile();
    mappingProfile.setDefault(true);
    var marcRecords = authorityExportStrategy.getMarcRecords(localAuthorityIds, mappingProfile);

    assertThat(marcRecords).hasSize(1);
    assertEquals(LOCAL_MARC_AUTHORITY_UUID, marcRecords.get(0).getId());
  }

  @Test
  void shouldReturnOneSharedRecord() {
    handleCentralTenant();

    var centralAuthorityIds = new HashSet<UUID>();
    centralAuthorityIds.add(CENTRAL_AUTHORITY_UUID);
    var mappingProfile = new MappingProfile();
    mappingProfile.setDefault(true);
    var marcRecords = authorityExportStrategy.getMarcRecords(centralAuthorityIds, mappingProfile);

    assertThat(marcRecords).hasSize(1);
    assertEquals(CENTRAL_MARC_AUTHORITY_UUID, marcRecords.get(0).getId());
  }

  @Test
  void shouldReturnOneSharedAndOneLocalRecord() {
    handleCentralTenant();

    var centralAuthorityIds = new HashSet<UUID>();
    centralAuthorityIds.add(CENTRAL_AUTHORITY_UUID);
    centralAuthorityIds.add(LOCAL_AUTHORITY_UUID);
    var mappingProfile = new MappingProfile();
    mappingProfile.setDefault(true);
    var marcRecords = authorityExportStrategy.getMarcRecords(centralAuthorityIds, mappingProfile);

    assertThat(marcRecords).hasSize(2);
    assertEquals(LOCAL_MARC_AUTHORITY_UUID, marcRecords.get(0).getId());
    assertEquals(CENTRAL_MARC_AUTHORITY_UUID, marcRecords.get(1).getId());
  }

  @Test
  void shouldReturnEmptyList_ifIdNotFound() {
    handleCentralTenant();

    var notFoundUUID = UUID.randomUUID();
    var centralAuthorityIds = new HashSet<UUID>();
    centralAuthorityIds.add(notFoundUUID);
    var mappingProfile = new MappingProfile();
    mappingProfile.setDefault(true);
    var marcRecords = authorityExportStrategy.getMarcRecords(centralAuthorityIds, mappingProfile);

    assertThat(marcRecords.size()).isZero();
  }

  @Test
  void shouldReturnOneRecordFromLocalAndOneNotFound_ifOneIdNotFound() {
    handleCentralTenant();

    var notFoundUUID = UUID.randomUUID();
    var centralAuthorityIds = new HashSet<UUID>();
    centralAuthorityIds.add(notFoundUUID);
    centralAuthorityIds.add(LOCAL_AUTHORITY_UUID);
    var mappingProfile = new MappingProfile();
    mappingProfile.setDefault(true);
    var marcRecords = authorityExportStrategy.getMarcRecords(centralAuthorityIds, mappingProfile);

    assertThat(marcRecords).hasSize(1);
    assertEquals(LOCAL_MARC_AUTHORITY_UUID, marcRecords.get(0).getId());
  }

  private void handleCentralTenant() {
    var userTenantCollection = new UserTenantCollection();
    var userTenants = new ArrayList<UserTenant>();
    var centralUserTenant = new UserTenant();
    centralUserTenant.setCentralTenantId("central");
    userTenants.add(centralUserTenant);
    userTenantCollection.setUserTenants(userTenants);
    when(consortiaClient.getUserTenantCollection()).thenReturn(userTenantCollection);
  }
}
