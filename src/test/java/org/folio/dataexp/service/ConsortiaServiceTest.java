package org.folio.dataexp.service;

import org.folio.dataexp.client.ConsortiaClient;
import org.folio.dataexp.domain.dto.UserTenant;
import org.folio.dataexp.domain.dto.UserTenantCollection;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsortiaServiceTest {

  @Mock
  private ConsortiaClient consortiaClient;

  @Mock
  private FolioExecutionContext folioExecutionContext;

  @InjectMocks
  private ConsortiaService consortiaService;

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
    when(folioExecutionContext.getTenantId()).thenReturn("college");

    assertThat(consortiaService.getCentralTenantId("college")).isEqualTo("consortium");
  }
}
