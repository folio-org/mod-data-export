package org.folio.clients;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.processor.rule.Rule;
import org.folio.service.logs.ErrorLogService;
import org.folio.util.ErrorCode;
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
import static org.apache.commons.lang3.StringUtils.EMPTY;
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
      response = Optional.of(ClientUtil.getRequest(params, endpoint));
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
        LOGGER.error("No configuration for host in mod config. There will be no link to the failed entry");
        errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_QUERY_CONFIGURATIONS.getCode(), Arrays.asList(query, "No configuration for host in mod config. There will be no link to the failed record"),
          jobExecutionId, params.getTenantId());
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
      rulesFromConfig = Optional.of(ClientUtil.getRequest(params, endpoint));
    } catch (HttpClientException e) {
      errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_QUERY_RULES_FROM_CONFIGURATIONS.getCode(), Arrays.asList(e.getMessage()), jobExecutionId, params.getTenantId());
      rulesFromConfig = Optional.empty();
    }
    return rulesFromConfig.map(entries -> constructRulesFromJson(entries, params.getTenantId())).orElse(emptyList());
  }

  private List<Rule> constructRulesFromJson(JsonObject configRules, String tenantId) {
    List<Rule> rules = new ArrayList<>();
    configRules
      .getJsonArray("configs")
      .stream()
      .map(object -> (JsonObject) object)
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
        .map(object -> (JsonObject) object)
        .forEach(element ->
          rulesFromConfig.add(Json.decodeValue(format(element.toString(), StandardCharsets.UTF_8), Rule.class)));

      return rulesFromConfig;
    } catch (DecodeException e) {
      LOGGER.error("Fail to decode rules from mod-configuration for tenant {}, with id {}", tenantId, configEntry.getString("id"), e);
    }
    return emptyList();
  }

}
