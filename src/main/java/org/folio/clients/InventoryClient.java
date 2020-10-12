package org.folio.clients;

import static java.lang.String.format;
import static org.folio.clients.ClientUtil.buildQueryEndpoint;
import static org.folio.clients.ClientUtil.getRequest;
import static org.folio.util.ExternalPathResolver.ALTERNATIVE_TITLE_TYPES;
import static org.folio.util.ExternalPathResolver.CALL_NUMBER_TYPES;
import static org.folio.util.ExternalPathResolver.CAMPUSES;
import static org.folio.util.ExternalPathResolver.CONTENT_TERMS;
import static org.folio.util.ExternalPathResolver.CONTRIBUTOR_NAME_TYPES;
import static org.folio.util.ExternalPathResolver.ELECTRONIC_ACCESS_RELATIONSHIPS;
import static org.folio.util.ExternalPathResolver.HOLDING;
import static org.folio.util.ExternalPathResolver.HOLDING_NOTE_TYPES;
import static org.folio.util.ExternalPathResolver.IDENTIFIER_TYPES;
import static org.folio.util.ExternalPathResolver.INSTANCE;
import static org.folio.util.ExternalPathResolver.INSTANCE_BULK_IDS;
import static org.folio.util.ExternalPathResolver.INSTANCE_FORMATS;
import static org.folio.util.ExternalPathResolver.INSTANCE_TYPES;
import static org.folio.util.ExternalPathResolver.INSTITUTIONS;
import static org.folio.util.ExternalPathResolver.ISSUANCE_MODES;
import static org.folio.util.ExternalPathResolver.ITEM;
import static org.folio.util.ExternalPathResolver.ITEM_NOTE_TYPES;
import static org.folio.util.ExternalPathResolver.LIBRARIES;
import static org.folio.util.ExternalPathResolver.LOAN_TYPES;
import static org.folio.util.ExternalPathResolver.LOCATIONS;
import static org.folio.util.ExternalPathResolver.MATERIAL_TYPES;
import static org.folio.util.ExternalPathResolver.resourcesPathWithPrefix;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.service.logs.ErrorLogService;
import org.folio.util.OkapiConnectionParams;
import org.folio.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class InventoryClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String LIMIT_PARAMETER = "?limit=";
  private static final String QUERY_PATTERN_INVENTORY = "id==%s";
  private static final String QUERY_LIMIT_PATTERN = "?query=(%s)&limit=";
  private static final String QUERY_PATTERN_HOLDING = "instanceId==%s";
  private static final String QUERY_PATTERN_ITEM = "holdingsRecordId==%s";
  private static final String QUERY = "?(query=";
  private static final int REFERENCE_DATA_LIMIT = 200;
  private static final int HOLDINGS_LIMIT = 1000;

  @Autowired
  private ErrorLogService errorLogService;

  public Optional<JsonObject> getInstancesByIds(List<String> ids, String jobExecutionId, OkapiConnectionParams params, int partitionSize) {
    try {
      return Optional.of(ClientUtil.getByIds(ids, params, resourcesPathWithPrefix(INSTANCE) + QUERY_LIMIT_PATTERN + partitionSize,
          QUERY_PATTERN_INVENTORY));
    } catch (HttpClientException exception) {
      LOGGER.error(exception.getMessage(), exception.getCause());
      errorLogService.saveGeneralError("Error while getting instances by ids. " + exception.getMessage(), jobExecutionId, params.getTenantId());
      return Optional.empty();
    }
  }

  public Optional<JsonObject> getInstancesBulkUUIDs(String query, OkapiConnectionParams params) {
    if (StringUtils.isEmpty(query)) {
      return Optional.empty();
    }
    String endpoint = format(resourcesPathWithPrefix(INSTANCE_BULK_IDS), params.getOkapiUrl()) + QUERY + StringUtil.urlEncode(query) + ")";
    try {
      return Optional.of(ClientUtil.getRequest(params, endpoint));
    } catch (HttpClientException e) {
      LOGGER.error(e.getMessage(), e.getCause());
      errorLogService.saveGeneralError("Error while getting instances by ids. " + e.getMessage(), StringUtils.EMPTY, params.getTenantId());
      return Optional.empty();
    }
  }

  public Map<String, JsonObject> getNatureOfContentTerms(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(CONTENT_TERMS) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, CONTENT_TERMS);
  }

  public Map<String, JsonObject> getIdentifierTypes(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(IDENTIFIER_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, IDENTIFIER_TYPES);
  }

  public Map<String, JsonObject> getLocations(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(LOCATIONS) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, LOCATIONS);
  }

  public Map<String, JsonObject> getLibraries(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(LIBRARIES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, LIBRARIES);
  }

  public Map<String, JsonObject> getCampuses(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(CAMPUSES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, CAMPUSES);
  }

  public Map<String, JsonObject> getInstitutions(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(INSTITUTIONS) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, INSTITUTIONS);
  }

  public Map<String, JsonObject> getMaterialTypes(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(MATERIAL_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, MATERIAL_TYPES);
  }

  public Map<String, JsonObject> getModesOfIssuance(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(ISSUANCE_MODES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, ISSUANCE_MODES);
  }

  public Map<String, JsonObject> getInstanceTypes(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(INSTANCE_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, INSTANCE_TYPES);
  }

  public Map<String, JsonObject> getInstanceFormats(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(INSTANCE_FORMATS) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, INSTANCE_FORMATS);
  }

  public Map<String, JsonObject> getElectronicAccessRelationships(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(ELECTRONIC_ACCESS_RELATIONSHIPS) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, ELECTRONIC_ACCESS_RELATIONSHIPS);
  }

  public Map<String, JsonObject> getAlternativeTitleTypes(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(ALTERNATIVE_TITLE_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, ALTERNATIVE_TITLE_TYPES);
  }

  public Map<String, JsonObject> getLoanTypes(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(LOAN_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, LOAN_TYPES);
  }

  public Map<String, JsonObject> getHoldingsNoteTypes(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(HOLDING_NOTE_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, HOLDING_NOTE_TYPES);
  }

  public Map<String, JsonObject> getItemNoteTypes(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(ITEM_NOTE_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, ITEM_NOTE_TYPES);
  }

  public Map<String, JsonObject> getCallNumberTypes(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(CALL_NUMBER_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, CALL_NUMBER_TYPES);
  }

  public Map<String, JsonObject> getContributorNameTypes(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = resourcesPathWithPrefix(CONTRIBUTOR_NAME_TYPES) + LIMIT_PARAMETER + REFERENCE_DATA_LIMIT;
    return getReferenceDataByUrl(endpoint, jobExecutionId, params, CONTRIBUTOR_NAME_TYPES);
  }

  private Map<String, JsonObject> getReferenceDataByUrl(String url, String jobExecutionId, OkapiConnectionParams params, String field) {
    String queryEndpoint = ClientUtil.buildQueryEndpoint(url, params.getOkapiUrl());
    Optional<JsonObject> responseBody;
    Map<String, JsonObject> map = new HashMap<>();
    try {
      responseBody = Optional.of(ClientUtil.getRequest(params, queryEndpoint));
    } catch (HttpClientException e) {
      if (StringUtils.isNotEmpty(jobExecutionId)) {
        errorLogService.saveGeneralError("Error while getting reference data from inventory during the export process by calling  " + url, jobExecutionId, params.getTenantId());
      }
      return map;
    }
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

  public Optional<JsonObject> getHoldingsByInstanceId(String instanceID, String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = buildQueryEndpoint(resourcesPathWithPrefix(HOLDING) + QUERY_LIMIT_PATTERN + HOLDINGS_LIMIT,
      params.getOkapiUrl(), format(QUERY_PATTERN_HOLDING, instanceID));
    try {
      return Optional.of(getRequest(params, endpoint));
    } catch (HttpClientException exception) {
      errorLogService.saveGeneralError(format("Error while getting holdings by instance id: %s, message: %s", instanceID, exception.getMessage()), jobExecutionId, params.getTenantId());
      return Optional.empty();
    }
  }

  public Optional<JsonObject> getItemsByHoldingIds(List<String> holdingIds, String jobExecutionId, OkapiConnectionParams params) {
    try {
      return Optional.of(ClientUtil.getByIds(holdingIds, params, resourcesPathWithPrefix(ITEM) + QUERY_LIMIT_PATTERN + HOLDINGS_LIMIT,
          QUERY_PATTERN_ITEM));
    } catch (HttpClientException exception) {
      LOGGER.error(exception.getMessage(), exception.getCause());
      errorLogService.saveGeneralError("Error while getting items by holding ids " + exception.getMessage(), jobExecutionId, params.getTenantId());
      return Optional.empty();
    }
  }

}
