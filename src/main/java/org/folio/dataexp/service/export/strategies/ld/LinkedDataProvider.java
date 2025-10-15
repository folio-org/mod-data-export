package org.folio.dataexp.service.export.strategies.ld;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.dataexp.domain.dto.LinkedDataResource;
import org.folio.dataexp.service.QueryService;
import org.springframework.stereotype.Component;

/**
 * Retrieve Linked Data resources.
 */
@Component
@RequiredArgsConstructor
public class LinkedDataProvider {

  private final QueryService queryService;

  private static final UUID LINKED_DATA_RESOURCE =
      UUID.fromString("84451307-215c-4021-a46d-b9dcfada7439");
  private static final String GRAPH_FIELD = "resource_subgraph";
  private static final String INVENTORY_ID_FIELD = "inventory_id";
  private static final String VALUE_KEY = "value";
  private static final List<String> RESOURCE_FIELDS = List.of(INVENTORY_ID_FIELD, GRAPH_FIELD);

  /**
   * Retrieve Linked Data resources given a set of inventory identifiers.
   *
   * @param ids set of inventory identifiers
   * @return list of matching Linked Data export resource JSON as String
   */
  public List<LinkedDataResource> getLinkedDataResources(Set<UUID> ids) {
    return queryService.getEntities(ids, LINKED_DATA_RESOURCE, RESOURCE_FIELDS)
      .stream()
      .filter(this::containsRequiredKeys)
      .map(this::createLinkedDataResource)
      .toList();
  }

  private boolean containsRequiredKeys(Map<String, Object> resource) {
    return resource.containsKey(GRAPH_FIELD) && resource.containsKey(INVENTORY_ID_FIELD);
  }

  /*
   * FQM returns a Map<String, Object> where the value object may be another
   * map representing a database column type and value. Here we expect it to be
   * "type": "jsonb", "value": "{...}"
   */
  private LinkedDataResource createLinkedDataResource(Map<String, Object> resource) {
    var ldr = new LinkedDataResource();
    ldr.setInventoryId((String) resource.get(INVENTORY_ID_FIELD));
    var columnObj = resource.get(GRAPH_FIELD);
    if (columnObj instanceof LinkedHashMap) {
      ldr.setResource((String) ((LinkedHashMap<?, ?>) columnObj).get(VALUE_KEY));
    } else {
      ldr.setResource((String) resource.get(GRAPH_FIELD));
    }
    return ldr;
  }
}