package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {RetryableUserClient.class, RetryableUserClientTest.RetryConfig.class})
@TestPropertySource(
    properties = {
      "application.feign-client-retry.max-attempts=3",
      "application.feign-client-retry.initial-wait-time=0",
      "application.feign-client-retry.max-wait-time=0"
    })
class RetryableUserClientTest {

  @Configuration
  @EnableRetry
  static class RetryConfig {}

  @MockitoBean private UserClient userClient;

  @Autowired private RetryableUserClient retryableUserClient;

  @Test
  void getUserByIdShouldReturnUserWhenClientSucceeds() {
    var user = new User();
    user.setUsername("john.doe");
    when(userClient.getUserById("u1")).thenReturn(user);

    var result = retryableUserClient.getUserById("u1");

    assertThat(result.getUsername()).isEqualTo("john.doe");
    verify(userClient, times(1)).getUserById("u1");
  }

  @Test
  void getUserByIdShouldRetryOnTransientFailure() {
    var user = new User();
    user.setUsername("jane.doe");
    when(userClient.getUserById("u1"))
        .thenThrow(new RuntimeException("transient"))
        .thenReturn(user);

    var result = retryableUserClient.getUserById("u1");

    assertThat(result.getUsername()).isEqualTo("jane.doe");
    verify(userClient, times(2)).getUserById("u1");
  }

  @Test
  void getUserByIdShouldThrowAfterMaxAttempts() {
    when(userClient.getUserById("u1")).thenThrow(new RuntimeException("persistent"));

    assertThrows(RuntimeException.class, () -> retryableUserClient.getUserById("u1"));
    verify(userClient, times(3)).getUserById("u1");
  }
}
