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
  public List<Map<String, Object>> getEntities(
      Set<UUID> ids, UUID entityTypeId, List<String> fields) {
    var request = toRequest(ids, entityTypeId, fields);
    return runQuery(request);
  }

  private ContentsRequest toRequest(Set<UUID> ids, UUID entityTypeId, List<String> fields) {
    var requestIds = ids.stream().map(id -> Collections.singletonList(id.toString())).toList();
    return new ContentsRequest().ids(requestIds).entityTypeId(entityTypeId).fields(fields);
  }

  private List<Map<String, Object>> runQuery(ContentsRequest contents) {
    return queryClient.getContents(contents);
  }
}
