package org.folio.dataexp.service;

import static org.folio.dataexp.service.UserPermissionsService.EUREKA_PLATFORM;
import static org.folio.dataexp.service.UserPermissionsService.OKAPI_PLATFORM;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.dataexp.client.EurekaUserPermissionsClient;
import org.folio.dataexp.client.OkapiUserPermissionsClient;
import org.folio.dataexp.domain.dto.UserPermissions;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPermissionsServiceTest {

  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private OkapiUserPermissionsClient okapiUserPermissionsClient;
  @Mock private EurekaUserPermissionsClient eurekaUserPermissionsClient;

  @InjectMocks private UserPermissionsService userPermissionsService;

  @Test
  void getPermissionsTest() {
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(okapiUserPermissionsClient.getPermissions(isA(String.class)))
        .thenReturn(new UserPermissions());

    userPermissionsService.setPlatform(OKAPI_PLATFORM);
    userPermissionsService.getPermissions();
    verify(okapiUserPermissionsClient).getPermissions(isA(String.class));
  }

  @Test
  void getPermissionsIfEurekaTest() {
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(eurekaUserPermissionsClient.getPermissions(isA(String.class), anyList()))
        .thenReturn(new UserPermissions());

    userPermissionsService.setPlatform(EUREKA_PLATFORM);
    userPermissionsService.getPermissions();
    verify(eurekaUserPermissionsClient).getPermissions(isA(String.class), anyList());
  }
}
