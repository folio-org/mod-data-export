package org.folio.clients;

import static org.folio.clients.ClientUtil.buildQueryEndpoint;
import static org.folio.clients.ClientUtil.getRequest;
import static org.folio.util.ExternalPathResolver.CONTENT_TERMS;
import static org.folio.util.ExternalPathResolver.HOLDING;
import static org.folio.util.ExternalPathResolver.IDENTIFIER_TYPES;
import static org.folio.util.ExternalPathResolver.CONTRIBUTOR_NAME_TYPES;
import static org.folio.util.ExternalPathResolver.INSTANCE;
import static org.folio.util.ExternalPathResolver.ITEM;
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
  private static final String QUERY_PATTERN_HOLDING = "instanceId==%s";
  private static final String QUERY_PATTERN_ITEM = "holdingsRecordId==%s";
  private static final int REFERENCE_DATA_LIMIT = 200;
  private static final int HOLDINGS_LIMIT = 1000;

  public Optional<JsonObject> getInstancesByIds(List<String> ids, OkapiConnectionParams params, int partitionSize) {
    return ClientUtil.getByIds(ids, params, resourcesPathWithPrefix(INSTANCE) + QUERY_LIMIT_PATTERN + partitionSize,
        QUERY_PATTERN_INVENTORY);
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
    String queryEndpoint = ClientUtil.buildQueryEndpoint(url, params.getOkapiUrl());
    Optional<JsonObject> responseBody = ClientUtil.getRequest(params, queryEndpoint);
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

  public Optional<JsonObject> getHoldingsByInstanceId(String instanceID, OkapiConnectionParams params) {
    String endpoint = buildQueryEndpoint(resourcesPathWithPrefix(HOLDING) + QUERY_LIMIT_PATTERN + HOLDINGS_LIMIT,
        params.getOkapiUrl(), String.format(QUERY_PATTERN_HOLDING, instanceID));
    return getRequest(params, endpoint);
  }

  public Optional<JsonObject> getItemsByHoldingIds(List<String> holdingIds, OkapiConnectionParams params) {
    return ClientUtil.getByIds(holdingIds, params, resourcesPathWithPrefix(ITEM) + QUERY_LIMIT_PATTERN + HOLDINGS_LIMIT,
        QUERY_PATTERN_ITEM);
  }

  public Map<String, JsonObject> getContributorNameTypes(OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(CONTRIBUTOR_NAME_TYPES) + "?limit=" + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, CONTRIBUTOR_NAME_TYPES);
  }

}
