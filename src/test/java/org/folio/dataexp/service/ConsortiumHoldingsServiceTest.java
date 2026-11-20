package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.dataexp.client.ConsortiumSearchClient;
import org.folio.dataexp.domain.dto.Holdings;
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
    classes = {ConsortiumHoldingsService.class, ConsortiumHoldingsServiceTest.RetryConfig.class})
@TestPropertySource(
    properties = {
      "application.feign-client-retry.max-attempts=3",
      "application.feign-client-retry.initial-wait-time=0",
      "application.feign-client-retry.max-wait-time=0"
    })
class ConsortiumHoldingsServiceTest {

  @Configuration
  @EnableRetry
  static class RetryConfig {}

  @MockitoBean private ConsortiumSearchClient consortiumSearchClient;

  @Autowired private ConsortiumHoldingsService consortiumHoldingsService;

  @Test
  void getHoldingsByIdShouldReturnHoldingsWhenClientSucceeds() {
    var holdings = new Holdings();
    holdings.setTenantId("tenant1");
    when(consortiumSearchClient.getHoldingsById("h1")).thenReturn(holdings);

    var result = consortiumHoldingsService.getHoldingsById("h1");

    assertThat(result).isEqualTo(holdings);
    verify(consortiumSearchClient, times(1)).getHoldingsById("h1");
  }

  @Test
  void getHoldingsByIdShouldRetryOnTransientFailure() {
    var holdings = new Holdings();
    holdings.setTenantId("tenant1");
    when(consortiumSearchClient.getHoldingsById("h1"))
        .thenThrow(new RuntimeException("transient"))
        .thenReturn(holdings);

    var result = consortiumHoldingsService.getHoldingsById("h1");

    assertThat(result).isEqualTo(holdings);
    verify(consortiumSearchClient, times(2)).getHoldingsById("h1");
  }

  @Test
  void getHoldingsByIdShouldThrowAfterMaxAttempts() {
    when(consortiumSearchClient.getHoldingsById("h1"))
        .thenThrow(new RuntimeException("persistent"));

    assertThrows(RuntimeException.class, () -> consortiumHoldingsService.getHoldingsById("h1"));
    verify(consortiumSearchClient, times(3)).getHoldingsById("h1");
  }
}
