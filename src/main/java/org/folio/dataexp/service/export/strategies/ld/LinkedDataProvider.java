package org.folio.dataexp.service.export.strategies.ld;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.dataexp.service.QueryService;
import org.springframework.stereotype.Component;

/**
 * Retrieve Linked Data resources.
 */
@Component
@RequiredArgsConstructor
public class LinkedDataProvider {

  private final QueryService queryService;

  private static final UUID LINKED_DATA_RESOURCE = UUID.fromString("84451307-215c-4021-a46d-b9dcfada7439");
  private static final String GRAPH_FIELD = "resource_subgraph";
  private static final List<String> RESOURCE_FIELDS = List.of("inventory_id", GRAPH_FIELD);

  /**
   * Retrieve Linked Data resources given a set of inventory identifiers.
   *
   * @param ids set of inventory identifiers
   * @return list of matching Linked Data export resource JSON as String
   */
  public List<String> getLinkedDataResources(Set<UUID> ids) {
    return queryService.getEntities(ids, LINKED_DATA_RESOURCE, RESOURCE_FIELDS)
      .stream()
      .map(resource -> (String) resource.get(GRAPH_FIELD))
      .toList();
  }
}