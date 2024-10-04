package org.folio.dataexp.service.permissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class RequiredPermissionResolverTest {

  @Test
  void getReadPermissionTest() {
    assertEquals("ui-inventory.instance.view", new RequiredPermissionResolver().getReadPermission());
  }
}
