package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.client.QueryClient;
import org.folio.querytool.domain.dto.ContentsRequest;
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
@ContextConfiguration(classes = {QueryService.class, QueryServiceRetryTest.RetryConfig.class})
@TestPropertySource(
    properties = {
      "application.feign-query-client-retry.max-attempts=3",
      "application.feign-query-client-retry.initial-wait-time=0",
      "application.feign-query-client-retry.max-wait-time=0"
    })
class QueryServiceRetryTest {

  @Configuration
  @EnableRetry
  static class RetryConfig {}

  @MockitoBean private QueryClient queryClient;

  @Autowired private QueryService queryService;

  @Test
  void getEntitiesShouldRetryOnTransientFailure() {
    List<Map<String, Object>> expected = List.of(Map.of("id", "entity-1"));
    when(queryClient.getContents(any(ContentsRequest.class)))
        .thenThrow(new RuntimeException("transient"))
        .thenReturn(expected);

    var result =
        queryService.getEntities(Set.of(UUID.randomUUID()), UUID.randomUUID(), List.of("id"));

    assertThat(result).isEqualTo(expected);
    verify(queryClient, times(2)).getContents(any(ContentsRequest.class));
  }

  @Test
  void getEntitiesShouldThrowAfterMaxAttempts() {
    when(queryClient.getContents(any(ContentsRequest.class)))
        .thenThrow(new RuntimeException("persistent"));

    var uuidsSet = Set.of(UUID.randomUUID());
    var id = UUID.randomUUID();
    var idList = List.of("id");
    assertThrows(
        RuntimeException.class,
        () -> queryService.getEntities(uuidsSet, id, idList));
    verify(queryClient, times(3)).getContents(any(ContentsRequest.class));
  }
}
