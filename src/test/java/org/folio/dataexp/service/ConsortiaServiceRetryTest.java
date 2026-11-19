package org.folio.dataexp.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.folio.dataexp.client.ConsortiaClient;
import org.folio.dataexp.client.ConsortiumClient;
import org.folio.dataexp.domain.dto.UserTenantCollection;
import org.folio.spring.FolioExecutionContext;
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
    classes = {ConsortiaService.class, ConsortiaServiceRetryTest.RetryConfig.class})
@TestPropertySource(
    properties = {
      "application.feign-client-retry.max-attempts=3",
      "application.feign-client-retry.initial-wait-time=0",
      "application.feign-client-retry.max-wait-time=0"
    })
class ConsortiaServiceRetryTest {

  @Configuration
  @EnableRetry
  static class RetryConfig {}

  @MockitoBean FolioExecutionContext context;
  @MockitoBean ConsortiaClient consortiaClient;
  @MockitoBean ConsortiumClient consortiumClient;

  @Autowired private ConsortiaService consortiaService;

  @Test
  void getCentralTenantIdShouldRetryOnTransientFailure() {
    var emptyCollection = new UserTenantCollection();
    emptyCollection.setUserTenants(Collections.emptyList());
    when(consortiaClient.getUserTenantCollection())
        .thenThrow(new RuntimeException("transient"))
        .thenReturn(emptyCollection);

    var result = consortiaService.getCentralTenantId("tenant1");

    assertThat(result).isEqualTo(EMPTY);
    verify(consortiaClient, times(2)).getUserTenantCollection();
  }

  @Test
  void getCentralTenantIdShouldThrowAfterMaxAttempts() {
    when(consortiaClient.getUserTenantCollection()).thenThrow(new RuntimeException("persistent"));

    assertThrows(RuntimeException.class, () -> consortiaService.getCentralTenantId("tenant1"));
    verify(consortiaClient, times(3)).getUserTenantCollection();
  }
}
