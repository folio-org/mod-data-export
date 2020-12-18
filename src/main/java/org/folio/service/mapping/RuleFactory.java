package org.folio.service.mapping;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.rest.jaxrs.model.RecordType.HOLDINGS;
import static org.folio.rest.jaxrs.model.RecordType.INSTANCE;
import static org.folio.rest.jaxrs.model.RecordType.ITEM;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.folio.processor.rule.Rule;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.mapping.rulebuilder.CombinedRuleBuilder;
import org.folio.service.mapping.rulebuilder.DefaultRuleBuilder;
import org.folio.service.mapping.rulebuilder.RuleBuilder;
import org.folio.service.mapping.rulebuilder.TransformationRuleBuilder;
import org.folio.service.profiles.mappingprofile.MappingProfileServiceImpl;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RuleFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String DEFAULT_RULES_PATH = "rules/rulesDefault.json";
  private static final String TEMPORARY_LOCATION_FIELD_ID = "holdings.temporarylocation.name";
  private static final String PERMANENT_LOCATION_FIELD_ID = "holdings.permanentlocation.name";
  private static final String DEFAULT_BUILDER_KEY = "default.builder";
  private static final String TRANSFORMATION_BUILDER_KEY = "transformation.builder";
  private static final String INSTANCE_ELECTRONIC_ACCESS_ID = "instance.electronic.access";

  private static final Map<String, RuleBuilder> ruleBuilders = ImmutableMap.<String, RuleBuilder>builder()
    .put(INSTANCE_ELECTRONIC_ACCESS_ID, new CombinedRuleBuilder(3, INSTANCE_ELECTRONIC_ACCESS_ID))
    .put(TRANSFORMATION_BUILDER_KEY, new TransformationRuleBuilder())
    .put(DEFAULT_BUILDER_KEY, new DefaultRuleBuilder())
    .build();

  private List<Rule> defaultRules;

  public List<Rule> create(MappingProfile mappingProfile) {
    return create(mappingProfile, getDefaultRulesFromFile());
  }

  public List<Rule> create(MappingProfile mappingProfile, List<Rule> defaultRules) {
    if (mappingProfile == null || isEmpty(mappingProfile.getTransformations())) {
      LOGGER.info("No Mapping rules specified, using default mapping rules");
      return defaultRules;
    }
    List<Rule> rules = new ArrayList<>(createByTransformations(mappingProfile.getTransformations(), defaultRules));
    if (MappingProfileServiceImpl.isDefault(mappingProfile.getId()) && isNotEmpty(mappingProfile.getTransformations())) {
      rules.addAll(defaultRules);
    }
    return rules;
  }

  public Set<Rule> createByTransformations(List<Transformations> mappingTransformations, List<Rule> defaultRules) {
    Set<Rule> rules = new LinkedHashSet<>();
    String temporaryLocationTransformation = getTemporaryLocationTransformation(mappingTransformations);
    Optional<Rule> rule = Optional.empty();
    for (Transformations mappingTransformation : mappingTransformations) {
      if (isTransformationValidAndNotBlank(mappingTransformation)
        && isPermanentLocationNotEqualsTemporaryLocation(temporaryLocationTransformation, mappingTransformation)) {
        rule = ruleBuilders.get(TRANSFORMATION_BUILDER_KEY).build(rules, mappingTransformation);
      } else if (isInstanceTransformationValidAndBlank(mappingTransformation)) {
        rule = createDefaultByTransformations(mappingTransformation, defaultRules);
      } else if (HOLDINGS.equals(mappingTransformation.getRecordType()) || ITEM.equals(mappingTransformation.getRecordType())) {
        LOGGER.error(String.format("No transformation provided for field name: %s, and with record type: %s",
          mappingTransformation.getFieldId(), mappingTransformation.getRecordType()));
      }
      if (rule.isPresent()) {
        rules.add(rule.get());
      }
    }
    return rules;
  }

  public Optional<Rule> createDefaultByTransformations(Transformations mappingTransformation, List<Rule> defaultRules) {
    RecordType recordType = mappingTransformation.getRecordType();
    if (TRUE.equals(mappingTransformation.getEnabled()) && StringUtils.isNotBlank(mappingTransformation.getFieldId())
      && RecordType.INSTANCE.equals(recordType)) {
      for (Map.Entry<String, RuleBuilder> ruleBuilderEntry : ruleBuilders.entrySet()) {
        if (mappingTransformation.getFieldId().contains(ruleBuilderEntry.getKey())) {
          return ruleBuilderEntry.getValue().build(defaultRules, mappingTransformation);
        }
      }
      return ruleBuilders.get(DEFAULT_BUILDER_KEY).build(defaultRules, mappingTransformation);
    }
    return Optional.empty();
  }

  private boolean isTransformationValidAndNotBlank(Transformations mappingTransformation) {
    return isTransformationValid(mappingTransformation) && StringUtils.isNotBlank(mappingTransformation.getTransformation());
  }

  private boolean isPermanentLocationNotEqualsTemporaryLocation(String temporaryLocationTransformation, Transformations mappingTransformation) {
    return !(isHoldingsPermanentLocation(mappingTransformation) && temporaryLocationTransformation.equals(mappingTransformation.getTransformation()));
  }

  private boolean isInstanceTransformationValidAndBlank(Transformations mappingTransformation) {
    return isTransformationValid(mappingTransformation) && INSTANCE.equals(mappingTransformation.getRecordType()) && StringUtils.isBlank(mappingTransformation.getTransformation());
  }

  private String getTemporaryLocationTransformation(List<Transformations> mappingTransformations) {
    Optional<Transformations> temporaryLocationTransformation = mappingTransformations.stream()
      .filter(transformations -> HOLDINGS.equals(transformations.getRecordType()))
      .filter(transformations -> TEMPORARY_LOCATION_FIELD_ID.equals(transformations.getFieldId()))
      .findFirst();
    if (temporaryLocationTransformation.isPresent()) {
      return temporaryLocationTransformation.get().getTransformation();
    }
    return StringUtils.EMPTY;
  }

  private boolean isHoldingsPermanentLocation(Transformations mappingTransformation) {
    return HOLDINGS.equals(mappingTransformation.getRecordType()) && PERMANENT_LOCATION_FIELD_ID.equals(mappingTransformation.getFieldId());
  }

  private boolean isTransformationValid(Transformations mappingTransformation) {
    return TRUE.equals(mappingTransformation.getEnabled()) && StringUtils.isNotBlank(mappingTransformation.getPath());
  }

  protected List<Rule> getDefaultRulesFromFile() {
    if (nonNull(defaultRules)) {
      return defaultRules;
    }
    URL url = Resources.getResource(DEFAULT_RULES_PATH);
    String stringRules = null;
    try {
      stringRules = Resources.toString(url, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOGGER.error("Failed to fetch default rules for export");
      throw new NotFoundException(e);
    }
    defaultRules = Arrays.asList(Json.decodeValue(stringRules, Rule[].class));
    return defaultRules;
  }

}
