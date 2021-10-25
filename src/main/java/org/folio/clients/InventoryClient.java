package org.folio.clients;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.service.logs.ErrorLogService;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.folio.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import static java.lang.String.format;
import static org.folio.clients.ClientUtil.buildQueryEndpoint;
import static org.folio.clients.ClientUtil.getRequest;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
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
import static org.folio.util.ExternalPathResolver.RECORD_BULK_IDS;
import static org.folio.util.ExternalPathResolver.resourcesPathWithPrefix;

@Component
public class InventoryClient {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
  private static final String LIMIT_PARAMETER = "?limit=";
  private static final String QUERY_PATTERN_INVENTORY = "id==%s";
  private static final String QUERY_LIMIT_PATTERN = "?query=(%s)&limit=";
  private static final String QUERY_PATTERN_HOLDING = "instanceId==%s";
  private static final String QUERY_PATTERN_ITEM = "holdingsRecordId==%s";
  private static final String QUERY = "?query=";
  private static final String ERROR_MESSAGE_INVALID_STATUS_CODE = "Exception while calling %s, message: Get invalid response with status: %s";
  private static final String ERROR_MESSAGE_INVALID_BODY = "Exception while calling %s, message: Got invalid response body: %s";
  private static final String ERROR_MESSAGE_EMPTY_BODY = "Exception while calling %s, message: empty body returned.";
  private static final String BULK_EDIT_HOLDING_QUERY_PREFIX = "?field=instanceId&recordType=HOLDING&imit=10&query=";
  private static final int REFERENCE_DATA_LIMIT = 1000;
  private static final int HOLDINGS_LIMIT = 1000;

  @Autowired
  private ErrorLogService errorLogService;
  @Autowired
  private WebClient webClient;

  public Optional<JsonObject> getInstancesByIds(List<String> ids, String jobExecutionId, OkapiConnectionParams params, int partitionSize) {
    try {
      return Optional.of(ClientUtil.getByIds(ids, params, resourcesPathWithPrefix(INSTANCE) + QUERY_LIMIT_PATTERN + partitionSize,
        QUERY_PATTERN_INVENTORY));
    } catch (HttpClientException exception) {
      LOGGER.error(exception.getMessage(), exception.getCause());
      errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_GETTING_INSTANCES_BY_IDS.getCode(), Arrays.asList(exception.getMessage()), jobExecutionId, params.getTenantId());
      return Optional.empty();
    }
  }

  public Future<Optional<JsonObject>> getInstancesBulkUUIDsAsync(String query, OkapiConnectionParams params) {
    Promise<Optional<JsonObject>> promise = Promise.promise();
    if (StringUtils.isEmpty(query)) {
      promise.complete(Optional.empty());
      return promise.future();
    }
    String endpoint = format(resourcesPathWithPrefix(RECORD_BULK_IDS), params.getOkapiUrl()) + QUERY + StringUtil.urlEncode(query);
    HttpRequest<Buffer> request = webClient.getAbs(endpoint);
    request.putHeader(OKAPI_HEADER_TOKEN, params.getToken());
    request.putHeader(OKAPI_HEADER_TENANT, params.getTenantId());
    request.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    request.putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
    if (params.getOkapiUrl().contains("https")) {
      request.ssl(true);
    }
    request.send(res -> {
      if (res.failed()) {
        logError(res.cause(), params);
        promise.complete(Optional.empty());
      } else {
        HttpResponse<Buffer> response = res.result();
        if (response.statusCode() != HttpStatus.SC_OK) {
          logError(new IllegalStateException(format(ERROR_MESSAGE_INVALID_STATUS_CODE, endpoint, response.statusCode())), params);
          promise.complete(Optional.empty());
        } else {
          try {
            JsonObject instances = response.bodyAsJsonObject();
            if (instances != null) {
              promise.complete(Optional.of(instances));
            } else {
              logError(new IllegalStateException(format(ERROR_MESSAGE_EMPTY_BODY, endpoint)), params);
              promise.complete(Optional.empty());
            }
          } catch (DecodeException ex) {
            LOGGER.debug("Cannot process instances, invalid json body returned.", ex);
            logError(new IllegalStateException(format(ERROR_MESSAGE_INVALID_BODY, endpoint, response.bodyAsString())), params);
            promise.complete(Optional.empty());
          }
        }
      }
    });
    return promise.future();
  }

  private void logError(Throwable throwable, OkapiConnectionParams params) {
    LOGGER.error(throwable.getMessage(), throwable.getCause());
    errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_GETTING_INSTANCES_BY_IDS.getCode(), Arrays.asList(throwable.getMessage()), StringUtils.EMPTY, params.getTenantId());
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
        errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_GETTING_REFERENCE_DATA.getCode(), Arrays.asList(url), jobExecutionId, params.getTenantId());
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
      errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_GETTING_HOLDINGS_BY_INSTANCE_ID.getCode(), Arrays.asList(instanceID, exception.getMessage()), jobExecutionId, params.getTenantId());
      return Optional.empty();
    }
  }

  public Optional<JsonObject> getItemsByHoldingIds(List<String> holdingIds, String jobExecutionId, OkapiConnectionParams params) {
    try {
      return Optional.of(ClientUtil.getByIds(holdingIds, params, resourcesPathWithPrefix(ITEM) + QUERY_LIMIT_PATTERN + HOLDINGS_LIMIT,
          QUERY_PATTERN_ITEM));
    } catch (HttpClientException exception) {
      LOGGER.error(exception.getMessage(), exception.getCause());
      errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_GETTING_ITEM_BY_HOLDINGS_ID.getCode(), Arrays.asList(exception.getMessage()), jobExecutionId, params.getTenantId());
      return Optional.empty();
    }
  }

  public Future<List<String>> getInstanceIdsByHoldingIds(List<String> holdingIds, OkapiConnectionParams params) {
    Promise<List<String>> promise = Promise.promise();
    List<String> holdingBatchUrls = new ArrayList<>();
    String url = format(resourcesPathWithPrefix(RECORD_BULK_IDS), params.getOkapiUrl()) + BULK_EDIT_HOLDING_QUERY_PREFIX;
    Iterator<List<String>> idBatches = Iterables.partition(holdingIds, 10).iterator();
    idBatches.forEachRemaining(ids -> holdingBatchUrls.add(buildBulkEditQuery(ids, url)));
    List<Future<List<String>>> results = new ArrayList<>();
    holdingBatchUrls.forEach(holdingBatchUrl -> results.add(requestInstancesIdsByHoldingIds(holdingBatchUrl, params)));
    GenericCompositeFuture.all(results).onSuccess(v -> {
      List<String> instanceIds = results.stream()
        .map(Future::result)
        .flatMap(List::stream)
        .collect(Collectors.toList());
      promise.complete(instanceIds);
    });
    return promise.future();
  }

  private String buildBulkEditQuery(List<String> holdingIds, String queryPrefix) {
    StringBuilder queryBuilder = new StringBuilder(queryPrefix);
    holdingIds.forEach(holdingId -> queryBuilder.append("id==\"").append(holdingId).append("\"or+"));
    return queryBuilder.substring(0, queryBuilder.lastIndexOf("or+"));
  }

  private Future<List<String>> requestInstancesIdsByHoldingIds(String url, OkapiConnectionParams params) {
    Promise<List<String>> promise = Promise.promise();
    HttpRequest<Buffer> request = webClient.getAbs(url);
    request.putHeader(OKAPI_HEADER_TOKEN, params.getToken());
    request.putHeader(OKAPI_HEADER_TENANT, params.getTenantId());
    request.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    request.putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

    request.send(res -> {
      if (res.failed()) {
        logError(res.cause(), params);
        promise.complete(Collections.emptyList());
      } else {
        HttpResponse<Buffer> response = res.result();
        if (response.statusCode() != HttpStatus.SC_OK) {
          logError(new IllegalStateException(format(ERROR_MESSAGE_INVALID_STATUS_CODE, url, response.statusCode())), params);
          promise.complete(Collections.emptyList());
        } else {
          List<String> instanceIds = new ArrayList<>();
          JsonObject body = response.bodyAsJsonObject();
          body.getJsonArray("ids").stream()
            .map(JsonObject.class::cast)
            .forEach(elem -> instanceIds.add(elem.getString("instanceId")));
          promise.complete(instanceIds);
        }
      }
    });
    return promise.future();
  }

}
