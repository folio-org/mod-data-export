package org.folio.clients;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.processor.rule.Rule;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.service.logs.ErrorLogService;
import org.folio.util.ErrorCode;
import org.folio.util.HelperUtils;
import org.folio.util.OkapiConnectionParams;
import org.folio.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.util.ErrorCode.ERROR_QUERY_HOST;
import static org.folio.util.ExternalPathResolver.CONFIGURATIONS;
import static org.folio.util.ExternalPathResolver.resourcesPathWithPrefix;

@Component
public class ConfigurationsClient {
  public static final String QUERY_VALUE_FOR_HOST = "code=\"FOLIO_HOST\"";
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
  private static final String QUERY = "?query=";
  private static final String QUERY_VALUE = "code=\"RULES_OVERRIDE\" AND enabled==true";
  private static final String RECORDS_URL_PART = "/inventory/view/";

  @Autowired
  private ErrorLogService errorLogService;

  public Optional<JsonObject> getConfigsFromModConfigByQuery(String jobExecutionId, String query, OkapiConnectionParams params ) {
    String endpoint = format(resourcesPathWithPrefix(CONFIGURATIONS), params.getOkapiUrl()) + QUERY + StringUtil.urlEncode(query);
    Optional<JsonObject> response;
    try {
      response = getResponseFromGetRequest(endpoint, params);
    } catch (HttpClientException e) {
      errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_QUERY_CONFIGURATIONS.getCode(), Arrays.asList(query, e.getMessage()), jobExecutionId, params.getTenantId());
      response = Optional.empty();
    }
    return response;
  }

  public String getInventoryRecordLink(String idsUrlPart, String jobExecutionId, OkapiConnectionParams params) {
    Optional<JsonObject> jsonObject = getConfigsFromModConfigByQuery(jobExecutionId, ConfigurationsClient.QUERY_VALUE_FOR_HOST, params);
    String query = RECORDS_URL_PART + idsUrlPart;
    if (jsonObject.isPresent()) {
      JsonArray configs = jsonObject.get().getJsonArray("configs");
      if (configs.size() == 0) {
        LOGGER.error(ERROR_QUERY_HOST.getDescription());
        populateHostNotFoundErrorLog(jobExecutionId, params);
        return EMPTY;
      }
      return configs.getJsonObject(0).getString("value") + query;
    } else {
      return EMPTY;
    }
  }

  /**
   * Fetch rules for the mapping process from mod-configuration. If there are no rules provided in mod-configuration
   * or the rules are failed to decode or they are not enabled, an empty list will return, and default rules will be used
   *
   * @param params okapi headers and connection parameters
   * @return list of {@link Rule}
   */
  public List<Rule> getRulesFromConfiguration(String jobExecutionId, OkapiConnectionParams params) {
    String endpoint = format(resourcesPathWithPrefix(CONFIGURATIONS), params.getOkapiUrl()) + QUERY + StringUtil.urlEncode(QUERY_VALUE);
    Optional<JsonObject> rulesFromConfig ;
    try {
      rulesFromConfig = getResponseFromGetRequest(endpoint, params);
    } catch (HttpClientException e) {
      errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_QUERY_RULES_FROM_CONFIGURATIONS.getCode(), singletonList(e.getMessage()), jobExecutionId, params.getTenantId());
      rulesFromConfig = Optional.empty();
    }
    return rulesFromConfig.map(entries -> constructRulesFromJson(entries, params.getTenantId())).orElse(emptyList());
  }

  protected Optional<JsonObject> getResponseFromGetRequest(String endpoint, OkapiConnectionParams params)
    throws HttpClientException {
    return Optional.of(ClientUtil.getRequest(params, endpoint));
  }

  private void populateHostNotFoundErrorLog(String jobExecutionId, OkapiConnectionParams params) {
    Criterion criterion = HelperUtils.getErrorLogCriterionByJobExecutionIdAndErrorCodes(jobExecutionId,
      singletonList(ERROR_QUERY_HOST.getCode()));
    errorLogService.getByQuery(criterion, params.getTenantId())
      .onSuccess(errorLogList -> {
        long numberOfHostErrorForJob = errorLogList.stream()
          .filter(errorLog -> errorLog.getErrorMessageCode().contains(ERROR_QUERY_HOST.getCode())).count();
        if (numberOfHostErrorForJob == 0) {
          errorLogService.saveGeneralError(ERROR_QUERY_HOST.getCode(), jobExecutionId, params.getTenantId());
        }
      });
  }

  private List<Rule> constructRulesFromJson(JsonObject configRules, String tenantId) {
    List<Rule> rules = new ArrayList<>();
    configRules
      .getJsonArray("configs")
      .stream()
      .map(JsonObject.class::cast)
      .map(object -> getRules(object, tenantId))
      .forEach(rules::addAll);

    if (CollectionUtils.isEmpty(rules)) {
      return emptyList();
    } else {
      return rules;
    }
  }

  private List<Rule> getRules(JsonObject configEntry, String tenantId) {
    List<Rule> rulesFromConfig = new ArrayList<>();
    try {
      new JsonArray(configEntry.getString("value"))
        .stream()
        .map(JsonObject.class::cast)
        .forEach(element ->
          rulesFromConfig.add(Json.decodeValue(format(element.toString(), StandardCharsets.UTF_8), Rule.class)));

      return rulesFromConfig;
    } catch (DecodeException e) {
      LOGGER.error("Fail to decode rules from mod-configuration for tenant {}, with id {}", tenantId, configEntry.getString("id"), e);
    }
    return emptyList();
  }

}
