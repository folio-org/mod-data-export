package org.folio.dataexp.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.client.QueryClient;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/** Service for retrieving entities from FQM. */
@Service
@RequiredArgsConstructor
@Log4j2
public class QueryService {

  private final QueryClient queryClient;

  /**
   * Retrieves entities from FQM based on identifiers. Callers should expect to receive the generic
   * return list and then process further by entity type.
   *
   * @param ids list of identifiers for entities to retrieve
   * @param entityTypeId FQM entity type UUID
   * @param fields entity fields to retrieve
   * @return list of entities as field-value maps
   */
  @Retryable(
      maxAttemptsExpression = "${application.feign-query-client-retry.max-attempts}",
      backoff = @Backoff(
          delayExpression = "${application.feign-query-client-retry.initial-wait-time}",
          maxDelayExpression = "${application.feign-query-client-retry.max-wait-time}",
          multiplier = 2.0
      )
  )
  public List<Map<String, Object>> getEntities(
      Set<UUID> ids, UUID entityTypeId, List<String> fields) {
    var requestIds = ids.stream().map(id -> Collections.singletonList(id.toString())).toList();
    var request = new ContentsRequest().ids(requestIds).entityTypeId(entityTypeId).fields(fields);
    return queryClient.getContents(request);
  }
}
