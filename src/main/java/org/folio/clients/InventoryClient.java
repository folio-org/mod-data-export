package org.folio.clients;

import static org.folio.util.ExternalPathResolver.IDENTIFIER_TYPES;
import static org.folio.util.ExternalPathResolver.INSTANCE;
import static org.folio.util.ExternalPathResolver.CONTENT_TERMS;
import static org.folio.util.ExternalPathResolver.resourcesPathWithPrefix;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.folio.util.OkapiConnectionParams;
import org.springframework.stereotype.Component;

@Component
public class InventoryClient {
  private static final String QUERY_PATTERN_INVENTORY = "id==%s";
  private static final String QUERY_LIMIT_PATTERN = "?query=(%s)&limit=";
  private static final int REFERENCE_DATA_LIMIT = 200;

  public Optional<JsonObject> getInstancesByIds(List<String> ids, OkapiConnectionParams params, int partitionSize) {
    return ClientUtil.getByIds(ids, params, resourcesPathWithPrefix(INSTANCE) + QUERY_LIMIT_PATTERN + partitionSize, QUERY_PATTERN_INVENTORY);
  }

  public Map<String, JsonObject> getNatureOfContentTerms(OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(CONTENT_TERMS) + "?limit=" + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, CONTENT_TERMS);
  }

  public Map<String, JsonObject> getIdentifierTypes(OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(IDENTIFIER_TYPES) + "?limit=" + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, IDENTIFIER_TYPES);
  }

  private Map<String, JsonObject> getReferenceDataByUrl(String url, OkapiConnectionParams params, String field) {
    Optional<JsonObject> responseBody = ClientUtil.getRequest(params, url);
    Map<String, JsonObject> map = new HashMap<>();
    responseBody.ifPresent(rb -> {
      if (rb.containsKey(field)) {
        JsonArray array = rb.getJsonArray(field);
        for (Object item : array) {
          JsonObject jsonItem = JsonObject.mapFrom(item);
          map.put(jsonItem.getString("id"), jsonItem);
        }
      }
    });

    return map;
  }

}
