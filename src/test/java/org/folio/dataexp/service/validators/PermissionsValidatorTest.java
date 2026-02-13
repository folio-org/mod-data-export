package org.folio.dataexp.service.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.service.permissions.PermissionsProvider;
import org.folio.dataexp.service.permissions.RequiredPermissionResolver;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionsValidatorTest {

  private static final String TENANT_ID = "college";
  private static final String LOCK_MAPPING_PROFILE_PERMISSION =
      "data-export.mapping-profiles.item.lock.execute";
  private static final String READ_PERMISSION = "ui-inventory.instance.view";

  @Mock private PermissionsProvider permissionsProvider;
  @Mock private RequiredPermissionResolver requiredPermissionResolver;
  @Mock private FolioExecutionContext folioExecutionContext;
  @InjectMocks private PermissionsValidator permissionsValidator;

  private static final String LOCK_PERMISSION = "data-export.job-profile.lock";
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
  }

  @Test
  @SneakyThrows
  void checkInstanceViewPermissions_whenNoPermissionTest() {
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(permissionsProvider.getUserPermissions(TENANT_ID, userId.toString()))
        .thenReturn(List.of("bulk-edit.item.get"));
    when(requiredPermissionResolver.getReadPermission()).thenReturn(READ_PERMISSION);
    assertFalse(permissionsValidator.checkInstanceViewPermissions(TENANT_ID));
  }

  @Test
  @SneakyThrows
  void checkInstanceViewPermissions_whenPermissionExistsTest() {
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(permissionsProvider.getUserPermissions(TENANT_ID, userId.toString()))
        .thenReturn(List.of(READ_PERMISSION));
    when(requiredPermissionResolver.getReadPermission()).thenReturn(READ_PERMISSION);
    assertDoesNotThrow(() -> permissionsValidator.checkInstanceViewPermissions(TENANT_ID));
  }

  // ========== Tests for checkLockJobProfilePermission ==========

  @Test
  void shouldReturnTrue_whenUserHasLockPermission() {
    // Given
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(requiredPermissionResolver.getLockJobProfilePermission()).thenReturn(LOCK_PERMISSION);
    when(permissionsProvider.getUserPermissions(TENANT_ID, userId.toString()))
        .thenReturn(List.of(LOCK_PERMISSION));

    // When
    boolean result = permissionsValidator.checkLockJobProfilePermission();

    // Then
    assertThat(result).isTrue();
    verify(requiredPermissionResolver).getLockJobProfilePermission();
    verify(permissionsProvider).getUserPermissions(TENANT_ID, userId.toString());
  }

  @Test
  void shouldReturnFalse_whenUserDoesNotHaveLockPermission() {
    // Given
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(requiredPermissionResolver.getLockJobProfilePermission()).thenReturn(LOCK_PERMISSION);
    when(permissionsProvider.getUserPermissions(TENANT_ID, userId.toString()))
        .thenReturn(List.of("some.other.permission"));

    // When
    boolean result = permissionsValidator.checkLockJobProfilePermission();

    // Then
    assertThat(result).isFalse();
    verify(requiredPermissionResolver).getLockJobProfilePermission();
    verify(permissionsProvider).getUserPermissions(TENANT_ID, userId.toString());
  }

  @Test
  void shouldReturnFalse_whenUserHasEmptyPermissionsList_forLock() {
    // Given
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(requiredPermissionResolver.getLockJobProfilePermission()).thenReturn(LOCK_PERMISSION);
    when(permissionsProvider.getUserPermissions(TENANT_ID, userId.toString()))
        .thenReturn(Collections.emptyList());

    // When
    boolean result = permissionsValidator.checkLockJobProfilePermission();

    // Then
    assertThat(result).isFalse();
    verify(requiredPermissionResolver).getLockJobProfilePermission();
    verify(permissionsProvider).getUserPermissions(TENANT_ID, userId.toString());
  }

  @Test
  void shouldReturnTrue_whenUserHasMultiplePermissions_includingLock() {
    // Given
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(requiredPermissionResolver.getLockJobProfilePermission()).thenReturn(LOCK_PERMISSION);
    when(permissionsProvider.getUserPermissions(TENANT_ID, userId.toString()))
        .thenReturn(List.of("permission1", LOCK_PERMISSION, "permission2"));

    // When
    boolean result = permissionsValidator.checkLockJobProfilePermission();

    // Then
    assertThat(result).isTrue();
    verify(requiredPermissionResolver).getLockJobProfilePermission();
    verify(permissionsProvider).getUserPermissions(TENANT_ID, userId.toString());
  }

  @Test
  void shouldUseCorrectTenantId_whenCheckingLockPermission() {
    // Given
    String customTenantId = "custom-tenant";
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(customTenantId);
    when(requiredPermissionResolver.getLockJobProfilePermission()).thenReturn(LOCK_PERMISSION);
    when(permissionsProvider.getUserPermissions(customTenantId, userId.toString()))
        .thenReturn(List.of(LOCK_PERMISSION));

    // When
    boolean result = permissionsValidator.checkLockJobProfilePermission();

    // Then
    assertThat(result).isTrue();
    verify(permissionsProvider).getUserPermissions(customTenantId, userId.toString());
  }

  @Test
  void shouldReturnFalse_whenLockPermissionIsNull() {
    // Given
    when(requiredPermissionResolver.getLockJobProfilePermission()).thenReturn(null);

    // When
    boolean result = permissionsValidator.checkLockJobProfilePermission();

    // Then
    assertThat(result).isFalse();
    verify(requiredPermissionResolver).getLockJobProfilePermission();
  }

  @Test
  void shouldReturnFalse_whenUserHasSimilarButNotExactLockPermission() {
    // Given
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(requiredPermissionResolver.getLockJobProfilePermission()).thenReturn(LOCK_PERMISSION);
    when(permissionsProvider.getUserPermissions(TENANT_ID, userId.toString()))
        .thenReturn(List.of("data-export.job-profile.lock.extra", "data-export.job-profile"));

    // When
    boolean result = permissionsValidator.checkLockJobProfilePermission();

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void shouldUseDifferentUserId_whenContextChanges() {
    // Given
    UUID newUserId = UUID.randomUUID();
    when(folioExecutionContext.getUserId()).thenReturn(newUserId);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(requiredPermissionResolver.getLockJobProfilePermission()).thenReturn(LOCK_PERMISSION);
    when(permissionsProvider.getUserPermissions(TENANT_ID, newUserId.toString()))
        .thenReturn(List.of(LOCK_PERMISSION));

    // When
    boolean result = permissionsValidator.checkLockJobProfilePermission();

    // Then
    assertThat(result).isTrue();
    verify(permissionsProvider).getUserPermissions(TENANT_ID, newUserId.toString());
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(permissionsProvider.getUserPermissions(TENANT_ID, userId.toString()))
        .thenReturn(List.of(READ_PERMISSION));
    when(requiredPermissionResolver.getReadPermission()).thenReturn(READ_PERMISSION);
    assertDoesNotThrow(() -> permissionsValidator.checkInstanceViewPermissions(TENANT_ID));
  }

  // ========== Tests for checkLockMappingProfilePermission ==========

  @Test
  void shouldReturnTrue_whenUserHasLockMappingProfilePermission() {
    // Given
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(requiredPermissionResolver.getLockMappingProfilePermission())
        .thenReturn(LOCK_MAPPING_PROFILE_PERMISSION);
    when(permissionsProvider.getUserPermissions(TENANT_ID, userId.toString()))
        .thenReturn(List.of(LOCK_MAPPING_PROFILE_PERMISSION));

    // When
    boolean result = permissionsValidator.checkLockMappingProfilePermission();

    // Then
    assertThat(result).isTrue();
    verify(requiredPermissionResolver).getLockMappingProfilePermission();
    verify(permissionsProvider).getUserPermissions(TENANT_ID, userId.toString());
  }

  @Test
  void shouldReturnFalse_whenUserDoesNotHaveLockMappingProfilePermission() {
    // Given
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(requiredPermissionResolver.getLockMappingProfilePermission())
        .thenReturn(LOCK_MAPPING_PROFILE_PERMISSION);
    when(permissionsProvider.getUserPermissions(TENANT_ID, userId.toString()))
        .thenReturn(List.of("some.other.permission"));

    // When
    boolean result = permissionsValidator.checkLockMappingProfilePermission();

    // Then
    assertThat(result).isFalse();
    verify(requiredPermissionResolver).getLockMappingProfilePermission();
    verify(permissionsProvider).getUserPermissions(TENANT_ID, userId.toString());
  }

  @Test
  void shouldReturnFalse_whenUserHasEmptyPermissionsList_forLockMappingProfile() {
    // Given
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(requiredPermissionResolver.getLockMappingProfilePermission())
        .thenReturn(LOCK_MAPPING_PROFILE_PERMISSION);
    when(permissionsProvider.getUserPermissions(TENANT_ID, userId.toString()))
        .thenReturn(Collections.emptyList());

    // When
    boolean result = permissionsValidator.checkLockMappingProfilePermission();

    // Then
    assertThat(result).isFalse();
    verify(requiredPermissionResolver).getLockMappingProfilePermission();
    verify(permissionsProvider).getUserPermissions(TENANT_ID, userId.toString());
  }

  @Test
  void shouldReturnTrue_whenUserHasMultiplePermissions_includingLockMappingProfile() {
    // Given
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(requiredPermissionResolver.getLockMappingProfilePermission())
        .thenReturn(LOCK_MAPPING_PROFILE_PERMISSION);
    when(permissionsProvider.getUserPermissions(TENANT_ID, userId.toString()))
        .thenReturn(List.of("permission1", LOCK_MAPPING_PROFILE_PERMISSION, "permission2"));

    // When
    boolean result = permissionsValidator.checkLockMappingProfilePermission();

    // Then
    assertThat(result).isTrue();
    verify(requiredPermissionResolver).getLockMappingProfilePermission();
    verify(permissionsProvider).getUserPermissions(TENANT_ID, userId.toString());
  }

  @Test
  void shouldUseCorrectTenantId_whenCheckingLockMappingProfilePermission() {
    // Given
    String customTenantId = "custom-tenant";
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(customTenantId);
    when(requiredPermissionResolver.getLockMappingProfilePermission())
        .thenReturn(LOCK_MAPPING_PROFILE_PERMISSION);
    when(permissionsProvider.getUserPermissions(customTenantId, userId.toString()))
        .thenReturn(List.of(LOCK_MAPPING_PROFILE_PERMISSION));

    // When
    boolean result = permissionsValidator.checkLockMappingProfilePermission();

    // Then
    assertThat(result).isTrue();
    verify(permissionsProvider).getUserPermissions(customTenantId, userId.toString());
  }

  @Test
  void shouldReturnFalse_whenLockMappingProfilePermissionIsNull() {
    // Given
    when(requiredPermissionResolver.getLockMappingProfilePermission()).thenReturn(null);

    // When
    boolean result = permissionsValidator.checkLockMappingProfilePermission();

    // Then
    assertThat(result).isFalse();
    verify(requiredPermissionResolver).getLockMappingProfilePermission();
  }

  @Test
  void shouldReturnFalse_whenUserHasSimilarButNotExactLockMappingProfilePermission() {
    // Given
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(requiredPermissionResolver.getLockMappingProfilePermission())
        .thenReturn(LOCK_MAPPING_PROFILE_PERMISSION);
    when(permissionsProvider.getUserPermissions(TENANT_ID, userId.toString()))
        .thenReturn(
            List.of(
                "data-export.mapping-profiles.item.lock.execute.extra",
                "data-export.mapping-profiles"));

    // When
    boolean result = permissionsValidator.checkLockMappingProfilePermission();

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void shouldUseDifferentUserId_whenContextChangesForLockMappingProfile() {
    // Given
    UUID newUserId = UUID.randomUUID();
    when(folioExecutionContext.getUserId()).thenReturn(newUserId);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(requiredPermissionResolver.getLockMappingProfilePermission())
        .thenReturn(LOCK_MAPPING_PROFILE_PERMISSION);
    when(permissionsProvider.getUserPermissions(TENANT_ID, newUserId.toString()))
        .thenReturn(List.of(LOCK_MAPPING_PROFILE_PERMISSION));

    // When
    boolean result = permissionsValidator.checkLockMappingProfilePermission();

    // Then
    assertThat(result).isTrue();
    verify(permissionsProvider).getUserPermissions(TENANT_ID, newUserId.toString());
  }
}
