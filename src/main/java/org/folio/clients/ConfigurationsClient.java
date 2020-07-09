package org.folio.clients;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.processor.rule.Rule;
import org.folio.util.OkapiConnectionParams;
import org.folio.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.folio.util.ExternalPathResolver.CONFIGURATIONS;
import static org.folio.util.ExternalPathResolver.resourcesPathWithPrefix;

@Component
public class ConfigurationsClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String QUERY = "?query=";
  private static final String QUERY_VALUE = "code=\"RULES_OVERRIDE\" AND enabled==true";

  /**
   * Fetch rules for the mapping process from mod-configuration. If there are no rules provided in mod-configuration
   * or the rules are failed to decode or they are not enabled, an empty list will return, and default rules will be used
   *
   * @param params okapi headers and connection parameters
   * @return list of {@link Rule}
   */
  public List<Rule> getRulesFromConfiguration(OkapiConnectionParams params) {
    String endpoint = format(resourcesPathWithPrefix(CONFIGURATIONS), params.getOkapiUrl()) + QUERY + StringUtil.urlEncode(QUERY_VALUE);
    Optional<JsonObject> rulesFromConfig = ClientUtil.getRequest(params, endpoint);
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
