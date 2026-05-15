package org.folio.dataexp.service.export.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.dataexp.TestMate;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserClient userClient;

  @InjectMocks private UserService userService;

  @Test
  @TestMate(name = "TestMate-f03a772ab6b4a147eee989d62dd7b2d3")
  void getUserNameShouldReturnUsernameWhenUserExists() {
    // Given
    String userId = "a1b2c3d4-e5f6-7890-1234-567890abcdef";
    String tenantId = "test_tenant";
    String expectedUsername = "john.doe";
    User user = new User();
    user.setUsername(expectedUsername);
    when(userClient.getUserById(userId)).thenReturn(user);
    // When
    String actualUsername = userService.getUserName(tenantId, userId);
    // Then
    assertEquals(expectedUsername, actualUsername);
    verify(userClient).getUserById(userId);
  }

  @Test
  @TestMate(name = "TestMate-846357750ca06185e08ccdb095c0cabb")
  void getUserNameShouldReturnNullWhenUserHasNoUsername() {
    // Given
    String userId = "a1b2c3d4-e5f6-7890-1234-567890abcdef";
    String tenantId = "test-tenant";
    User user = new User();
    user.setUsername(null);
    when(userClient.getUserById(userId)).thenReturn(user);
    // When
    String actualUsername = userService.getUserName(tenantId, userId);
    // Then
    assertNull(actualUsername);
    verify(userClient).getUserById(userId);
  }
}
