package org.folio.clients;

import static org.folio.clients.ClientUtil.buildQueryEndpoint;
import static org.folio.clients.ClientUtil.getRequest;
import static org.folio.util.ExternalPathResolver.ALTERNATIVE_TITLE_TYPES;
import static org.folio.util.ExternalPathResolver.CALL_NUMBER_TYPES;
import static org.folio.util.ExternalPathResolver.CONTENT_TERMS;
import static org.folio.util.ExternalPathResolver.CONTRIBUTOR_NAME_TYPES;
import static org.folio.util.ExternalPathResolver.ELECTRONIC_ACCESS_RELATIONSHIPS;
import static org.folio.util.ExternalPathResolver.HOLDING;
import static org.folio.util.ExternalPathResolver.HOLDING_NOTE_TYPES;
import static org.folio.util.ExternalPathResolver.IDENTIFIER_TYPES;
import static org.folio.util.ExternalPathResolver.INSTANCE;
import static org.folio.util.ExternalPathResolver.INSTANCE_FORMATS;
import static org.folio.util.ExternalPathResolver.INSTANCE_TYPES;
import static org.folio.util.ExternalPathResolver.ISSUANCE_MODES;
import static org.folio.util.ExternalPathResolver.ITEM;
import static org.folio.util.ExternalPathResolver.ITEM_NOTE_TYPES;
import static org.folio.util.ExternalPathResolver.LOAN_TYPES;
import static org.folio.util.ExternalPathResolver.LOCATIONS;
import static org.folio.util.ExternalPathResolver.MATERIAL_TYPES;
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
  private static final String LIMIT_PARAMETER = "?limit=";
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
    String endpoint = resourcesPathWithPrefix(CONTENT_TERMS) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, CONTENT_TERMS);
  }

  public Map<String, JsonObject> getIdentifierTypes(OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(IDENTIFIER_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, IDENTIFIER_TYPES);
  }

  public Map<String, JsonObject> getLocations(OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(LOCATIONS) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, LOCATIONS);
  }

  public Map<String, JsonObject> getMaterialTypes(OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(MATERIAL_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, MATERIAL_TYPES);
  }

  public Map<String, JsonObject> getModesOfIssuance(OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(ISSUANCE_MODES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, ISSUANCE_MODES);
  }

  public Map<String, JsonObject> getInstanceTypes(OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(INSTANCE_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, INSTANCE_TYPES);
  }

  public Map<String, JsonObject> getInstanceFormats(OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(INSTANCE_FORMATS) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, INSTANCE_FORMATS);
  }

  public Map<String, JsonObject> getElectronicAccessRelationships(OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(ELECTRONIC_ACCESS_RELATIONSHIPS) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, ELECTRONIC_ACCESS_RELATIONSHIPS);
  }

  public Map<String, JsonObject> getAlternativeTitleTypes(OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(ALTERNATIVE_TITLE_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, ALTERNATIVE_TITLE_TYPES);
  }

  public Map<String, JsonObject> getLoanTypes(OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(LOAN_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, LOAN_TYPES);
  }

  public Map<String, JsonObject> getHoldingsNoteTypes(OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(HOLDING_NOTE_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, HOLDING_NOTE_TYPES);
  }

  public Map<String, JsonObject> getItemNoteTypes(OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(ITEM_NOTE_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, ITEM_NOTE_TYPES);
  }

  public Map<String, JsonObject> getCallNumberTypes(OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(CALL_NUMBER_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, CALL_NUMBER_TYPES);
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
    String endpoint = resourcesPathWithPrefix(CONTRIBUTOR_NAME_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, params, CONTRIBUTOR_NAME_TYPES);
  }

}
