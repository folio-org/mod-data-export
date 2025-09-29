package org.folio.dataexp.service.validators;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.service.permissions.PermissionsProvider;
import org.folio.dataexp.service.permissions.RequiredPermissionResolver;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionsValidatorTest {

  @Mock
  private PermissionsProvider permissionsProvider;
  @Mock
  private RequiredPermissionResolver requiredPermissionResolver;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @InjectMocks
  private PermissionsValidator permissionsValidator;

  @Test
  @SneakyThrows
  void checkInstanceViewPermissions_whenNoPermissionTest() {
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(permissionsProvider
        .getUserPermissions("college", folioExecutionContext.getUserId().toString()))
            .thenReturn(List.of("bulk-edit.item.get"));
    when(requiredPermissionResolver.getReadPermission())
        .thenReturn("ui-inventory.instance.view");
    assertFalse(permissionsValidator.checkInstanceViewPermissions("college"));
  }

  @Test
  @SneakyThrows
  void checkInstanceViewPermissions_whenPermissionExistsTest() {
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(permissionsProvider
        .getUserPermissions("college", folioExecutionContext.getUserId().toString()))
            .thenReturn(List.of("ui-inventory.instance.view"));
    when(requiredPermissionResolver.getReadPermission())
        .thenReturn("ui-inventory.instance.view");
    assertDoesNotThrow(() -> permissionsValidator.checkInstanceViewPermissions("college"));
  }
}
