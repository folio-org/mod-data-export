package org.folio.dataexp.service.permissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequiredPermissionResolverTest {

  private RequiredPermissionResolver requiredPermissionResolver;

  @BeforeEach
  void setUp() {
    requiredPermissionResolver = new RequiredPermissionResolver();
  }

  @Test
  void getReadPermissionTest() {
    assertEquals("ui-inventory.instance.view", requiredPermissionResolver.getReadPermission());
  }

  // ========== Tests for getLockJobProfilePermission ==========

  @Test
  void shouldReturnCorrectPermission_whenGetLockJobProfilePermission() {
    // When
    String permission = requiredPermissionResolver.getLockJobProfilePermission();

    // Then
    assertThat(permission).isEqualTo("data-export.job-profiles.item.lock.execute");
  }

  // ========== Tests for getUnlockJobProfilePermission ==========

  @Test
  void shouldReturnCorrectPermission_whenGetUnlockJobProfilePermission() {
    // When
    String permission = requiredPermissionResolver.getUnlockJobProfilePermission();

    // Then
    assertThat(permission).isEqualTo("data-export.job-profiles.item.unlock.execute");
  }
}
