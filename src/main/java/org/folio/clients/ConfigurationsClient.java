package org.folio.clients;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.service.mapping.processor.RuleFactory;
import org.folio.service.mapping.processor.rule.Rule;
import org.folio.util.OkapiConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.util.ExternalPathResolver.CONFIGURATIONS;
import static org.folio.util.ExternalPathResolver.resourcesPathWithPrefix;

@Component
public class ConfigurationsClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String QUERY_EXPORT_PATTERN = "?query=code=DATA_EXPORT";

  private final RuleFactory ruleFactory;

  public ConfigurationsClient() {
    this.ruleFactory = new RuleFactory();
  }


  /**
   * Fetch rules for the mapping process from mod-configuration. If there are no rules provided in mod-configuration
   * or the rules are failed to decode, an empty list will return, and default rules will be used
   *
   * @param params okapi headers and connection parameters
   * @return list of {@link Rule}
   */
  public List<Rule> getRulesFromConfiguration(MappingProfile mappingProfile, OkapiConnectionParams params) {
    String endpoint = ClientUtil.buildQueryEndpoint(resourcesPathWithPrefix(CONFIGURATIONS) + QUERY_EXPORT_PATTERN, params.getOkapiUrl());
    Optional<JsonObject> rulesFromConfig = ClientUtil.getRequest(params, endpoint);
    return rulesFromConfig.map(entries -> constructRulesFromJson(entries, mappingProfile, params.getTenantId())).orElse(emptyList());
  }

  private List<Rule> constructRulesFromJson(JsonObject configRules, MappingProfile mappingProfile, String tenantId) {
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
      if (mappingProfile != null && isNotEmpty(mappingProfile.getTransformations())) {
        rules.addAll(ruleFactory.buildByTransformations(mappingProfile.getTransformations()));
      }
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
          rulesFromConfig.add(Json.decodeValue(String.format(element.toString(), StandardCharsets.UTF_8), Rule.class)));

      return rulesFromConfig;
    } catch (DecodeException e) {
      LOGGER.error("Fail to decode rules from mod-configuration for tenant {}, with id {}", tenantId, configEntry.getString("id"), e);
    }
    return emptyList();
  }

}
