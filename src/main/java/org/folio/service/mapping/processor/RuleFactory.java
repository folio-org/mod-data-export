package org.folio.service.mapping.processor;

import com.google.common.io.Resources;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.mapping.processor.rule.Rule;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public class RuleFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private List<Rule> defaultRules;

  public List<Rule> create(MappingProfile mappingProfile) {
    if (mappingProfile == null || isEmpty(mappingProfile.getTransformations())) {
      return getDefaultRules();
    }
    return new ArrayList<>(createByMappingFields(mappingProfile.getTransformations()));

  }

  private List<Rule> getDefaultRules() {
    if (Objects.nonNull(this.defaultRules)) {
      return this.defaultRules;
    }
    URL url = Resources.getResource("rules/rulesDefault.json");
    String stringRules = null;
    try {
      stringRules = Resources.toString(url, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOGGER.error("Failed to fetch default rules for export");
      throw new NotFoundException(e);
    }
    this.defaultRules = Arrays.asList(Json.decodeValue(stringRules, Rule[].class));
    return this.defaultRules;
  }

  // The logic will be replaced in the future
  private List<Rule> createByMappingFields(List<Transformations> transformations) { //NOSONAR
    return getDefaultRules();
  }
}
