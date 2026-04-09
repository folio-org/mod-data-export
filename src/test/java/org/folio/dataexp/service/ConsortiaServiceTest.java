package org.folio.dataexp.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.client.ConsortiaClient;
import org.folio.dataexp.client.ConsortiumClient;
import org.folio.dataexp.domain.dto.Consortia;
import org.folio.dataexp.domain.dto.ConsortiaCollection;
import org.folio.dataexp.domain.dto.UserTenant;
import org.folio.dataexp.domain.dto.UserTenantCollection;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class ConsortiaServiceTest {

  @Mock private ConsortiaClient consortiaClient;
  @Mock private ConsortiumClient consortiumClient;
  @Mock private FolioExecutionContext folioExecutionContext;

  @InjectMocks private ConsortiaService consortiaService;

  @Test
  void shouldReturnNothing_whenNoCentralTenantExist() {
    var userTenantCollection = new UserTenantCollection();
    userTenantCollection.setUserTenants(List.of());
    when(consortiaClient.getUserTenantCollection()).thenReturn(userTenantCollection);

    assertThat(consortiaService.getCentralTenantId("tenantId")).isEqualTo(EMPTY);
  }

  @Test
  void shouldReturnFirstUserTenant_whenThereAreUserTenants() {
    var userTenantCollection = new UserTenantCollection();
    var centralTenant = new UserTenant();
    centralTenant.setCentralTenantId("consortium");
    var otherUserTenant = new UserTenant();
    otherUserTenant.setCentralTenantId("college");
    userTenantCollection.setUserTenants(List.of(centralTenant, otherUserTenant));
    when(consortiaClient.getUserTenantCollection()).thenReturn(userTenantCollection);

    assertThat(consortiaService.getCentralTenantId("college")).isEqualTo("consortium");
  }

  @Test
  void shouldReturnAffiliatedTenants() {
    var consortia = new Consortia();
    consortia.setId("consortiaId");
    var consortiaCollection = new ConsortiaCollection();
    consortiaCollection.setConsortia(List.of(consortia));

    var userTenant = new UserTenant();
    userTenant.setCentralTenantId("centralTenantId");
    userTenant.setTenantId("memberTenantId");
    var userTenantCollection = new UserTenantCollection();
    userTenantCollection.setUserTenants(List.of(userTenant));

    when(consortiumClient.getConsortia()).thenReturn(consortiaCollection);
    when(consortiumClient.getConsortiaUserTenants("consortiaId", "userId", Integer.MAX_VALUE))
        .thenReturn(userTenantCollection);

    var expected = List.of("memberTenantId");
    var actual = consortiaService.getAffiliatedTenants("currentTenantId", "userId");

    assertEquals(expected, actual);
  }

  @Test
  @TestMate(name = "TestMate-ba413db3d13135d5087b6f67529c2c9a")
  void isCurrentTenantCentralTenantShouldReturnTrueWhenIdsMatch() {
    // Given
    var tenantId = "central-tenant";
    var userTenant = new UserTenant();
    userTenant.setCentralTenantId(tenantId);
    var userTenantCollection = new UserTenantCollection();
    userTenantCollection.setUserTenants(List.of(userTenant));
    when(consortiaClient.getUserTenantCollection()).thenReturn(userTenantCollection);
    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    // When
    var result = consortiaService.isCurrentTenantCentralTenant(tenantId);
    // Then
    assertThat(result).isTrue();
  }

  @Test
  @TestMate(name = "TestMate-8a8eb209c7fe07d317427144d2c7eb98")
  void isCurrentTenantCentralTenantShouldReturnFalseWhenIdsDoNotMatch() {
    // Given
    var userTenant = new UserTenant();
    var centralTenantId = "central-tenant";
    userTenant.setCentralTenantId(centralTenantId);
    var userTenantCollection = new UserTenantCollection();
    var memberTenantId = "member-tenant";
    userTenantCollection.setUserTenants(List.of(userTenant));
    when(consortiaClient.getUserTenantCollection()).thenReturn(userTenantCollection);
    when(folioExecutionContext.getTenantId()).thenReturn(memberTenantId);
    // When
    var result = consortiaService.isCurrentTenantCentralTenant(memberTenantId);
    // Then
    assertThat(result).isFalse();
  }

  @Test
  @TestMate(name = "TestMate-a38fc71f25f95472598b111b2dd24f53")
  void isCurrentTenantCentralTenantShouldReturnFalseWhenNoCentralTenantFound() {
    // Given
    var tenantId = "any-tenant";
    var userTenantCollection = new UserTenantCollection();
    userTenantCollection.setUserTenants(List.of());
    when(consortiaClient.getUserTenantCollection()).thenReturn(userTenantCollection);
    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    // When
    var result = consortiaService.isCurrentTenantCentralTenant(tenantId);
    // Then
    assertThat(result).isFalse();
    verify(consortiaClient).getUserTenantCollection();
    verify(folioExecutionContext).getTenantId();
  }

    @Test
  @TestMate(name = "TestMate-getAffiliatedTenantsShouldReturnEmptyListWhenNoConsortiaExist")
  void getAffiliatedTenantsShouldReturnEmptyListWhenNoConsortiaExist() {
    // TestMate-eb6edcef2540a52e4d208cfd0cdeb101
    // Given
    var tenantId = "any-tenant";
    var userId = "any-user";
    var consortiaCollection = new ConsortiaCollection();
    consortiaCollection.setConsortia(Collections.emptyList());
    when(consortiumClient.getConsortia()).thenReturn(consortiaCollection);
    // When
    var result = consortiaService.getAffiliatedTenants(tenantId, userId);
    // Then
    assertThat(result).isEmpty();
    verify(consortiumClient).getConsortia();
    verifyNoMoreInteractions(consortiumClient);
  }

    @Test
@TestMate(name = "TestMate-getAffiliatedTenantsShouldReturnEmptyListWhenUserHasNoAffiliations")
void getAffiliatedTenantsShouldReturnEmptyListWhenUserHasNoAffiliations() {
  // TestMate-23cbd8203e563263f4f0047eafc6b315
  // Given
  var consortiumId = "cons-1";
  var userId = "unlinked-user";
  var currentTenantId = "tenant-1";
  var consortia = new Consortia();
  consortia.setId(consortiumId);
  var consortiaCollection = new ConsortiaCollection();
  consortiaCollection.setConsortia(List.of(consortia));
  var emptyUserTenantCollection = new UserTenantCollection();
  emptyUserTenantCollection.setUserTenants(List.of());
  when(consortiumClient.getConsortia()).thenReturn(consortiaCollection);
  when(consortiumClient.getConsortiaUserTenants(consortiumId, userId, Integer.MAX_VALUE))
      .thenReturn(emptyUserTenantCollection);
  // When
  var result = consortiaService.getAffiliatedTenants(currentTenantId, userId);
  // Then
  assertThat(result).isEmpty();
  verify(consortiumClient).getConsortia();
  verify(consortiumClient).getConsortiaUserTenants(consortiumId, userId, Integer.MAX_VALUE);
}

    @Test
  @TestMate(name = "TestMate-getAffiliatedTenantsShouldUseFirstConsortiumWhenMultipleExist")
  void getAffiliatedTenantsShouldUseFirstConsortiumWhenMultipleExist() {
    // TestMate-9770ee36596ab839901934234deaaf48
    // Given
    var userId = "user-1";
    var currentTenantId = "tenant-1";
    var firstConsortiumId = "first-cons";
    var secondConsortiumId = "second-cons";
    var affiliatedTenantId = "affiliated-tenant-id";
    var firstConsortia = new Consortia();
    firstConsortia.setId(firstConsortiumId);
    var secondConsortia = new Consortia();
    secondConsortia.setId(secondConsortiumId);
    var consortiaCollection = new ConsortiaCollection();
    consortiaCollection.setConsortia(List.of(firstConsortia, secondConsortia));
    var userTenant = new UserTenant();
    userTenant.setTenantId(affiliatedTenantId);
    var userTenantCollection = new UserTenantCollection();
    userTenantCollection.setUserTenants(List.of(userTenant));
    when(consortiumClient.getConsortia()).thenReturn(consortiaCollection);
    when(consortiumClient.getConsortiaUserTenants(firstConsortiumId, userId, Integer.MAX_VALUE))
        .thenReturn(userTenantCollection);
    // When
    var result = consortiaService.getAffiliatedTenants(currentTenantId, userId);
    // Then
    assertThat(result).hasSize(1).containsExactly(affiliatedTenantId);
    verify(consortiumClient).getConsortia();
    verify(consortiumClient).getConsortiaUserTenants(firstConsortiumId, userId, Integer.MAX_VALUE);
  }
}
