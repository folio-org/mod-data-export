package org.folio.dataexp.service.permissions;


import org.folio.dataexp.client.PermissionsSelfCheckClient;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionsProviderTest {

  @Mock
  private PermissionsSelfCheckClient permissionsSelfCheckClient;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private FolioModuleMetadata folioModuleMetadata;
  @InjectMocks
  private PermissionsProvider permissionsProvider;

  @Test
  void getUserPermissionsTest() {
    when(permissionsSelfCheckClient.getUserPermissionsForSelfCheck()).thenReturn(List.of("some permission"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.of(XOkapiHeaders.TENANT, List.of("college")));
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    var userPerms = permissionsProvider.getUserPermissions("college", folioExecutionContext.getUserId().toString());
    assertEquals("some permission", userPerms.get(0));
  }
}
