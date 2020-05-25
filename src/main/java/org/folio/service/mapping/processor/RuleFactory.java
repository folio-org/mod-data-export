package org.folio.service.mapping.processor;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import io.vertx.core.json.Json;
import org.folio.service.mapping.processor.rule.DataSource;
import org.folio.service.mapping.processor.rule.Rule;
import org.folio.service.mapping.processor.translations.Translation;
import org.folio.service.mapping.profiles.MappingProfile;
import org.folio.service.mapping.profiles.MappingProfileRule;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.folio.service.mapping.profiles.RecordType.HOLDINGS;
import static org.folio.service.mapping.profiles.RecordType.INSTANCE;


public class RuleFactory {

  public static final int TAG_INDEX = 0;

  static final Map<String, String> TRANSLATIONS_MAP = ImmutableMap.of(
    "electronicAccess.relationshipId", "set_electronic_access_relationship"
  );

  private List<Rule> defaultRules;

  public List<Rule> create(MappingProfile mappingProfile) {
    if (mappingProfile == null) {
      return getDefaultRules();
    }
    List<Rule> rules = new ArrayList<>();
    if (mappingProfile.getRecordTypes().contains(INSTANCE)) {
      rules.addAll(getDefaultRules());
    }
    rules.addAll(createByMappingFields(mappingProfile.getMappingProfileRules()));
    return rules;
  }

  private List<Rule> createByMappingFields(List<MappingProfileRule> mappingProfileRules) {
    List<Rule> rules = new ArrayList<>();
    for (MappingProfileRule mappingProfileRule : mappingProfileRules) {
      if (mappingProfileRule.isEnabled() && isNotBlank(mappingProfileRule.getPath())) {
        if (INSTANCE.equals(mappingProfileRule.getRecordType()) && isBlank(mappingProfileRule.getTransformation())) {
          for (Rule defaultRule : getDefaultRules()) {
            for (DataSource dataSource : defaultRule.getDataSources()) {
              if (isNotBlank(dataSource.getFrom()) && mappingProfileRule.getPath().equals(dataSource.getFrom())) {
                rules.add(defaultRule);
              }
            }
          }
        } else {
          String temporaryLocationTransformation = getTemporaryLocationTransformation(mappingProfileRules);
          if (!(isHoldingsPermanentLocation(mappingProfileRule) && temporaryLocationTransformation.equals(mappingProfileRule.getTransformation()))) {
            rules.add(buildRuleByMappingProfileField(mappingProfileRule));
          }
        }
      }
    }
    return rules;
  }

  private boolean isHoldingsPermanentLocation(MappingProfileRule mappingProfileRule) {
    return HOLDINGS.equals(mappingProfileRule.getRecordType()) && "permanentLocationId".equals(mappingProfileRule.getId());
  }

  private String getTemporaryLocationTransformation(List<MappingProfileRule> mappingProfileRules) {
    Optional<MappingProfileRule> temporaryLocationRule = mappingProfileRules.stream()
      .filter(rule -> HOLDINGS.equals(rule.getRecordType()))
      .filter(rule -> "temporaryLocationId".equals(rule.getId()))
      .findFirst();
    if (temporaryLocationRule.isPresent()) {
      MappingProfileRule mappingProfileRule = temporaryLocationRule.get();
      return mappingProfileRule.getTransformation();
    }
    return EMPTY;
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
      throw new RuntimeException(e);
    }
    this.defaultRules = Arrays.asList(Json.decodeValue(stringRules, Rule[].class));
    return this.defaultRules;
  }

  private Rule buildRuleByMappingProfileField(MappingProfileRule mappingProfileRule) {
    List<String> transformationParts = Lists.newArrayList(Splitter.on(" ").trimResults().split(mappingProfileRule.getTransformation()));
    Rule rule = new Rule();
    rule.setTag(transformationParts.get(TAG_INDEX));
    addDataSources(mappingProfileRule, transformationParts, rule);
    return rule;
  }

  private void addDataSources(MappingProfileRule mappingProfileRule, List<String> transformationParts, Rule rule) {
    List<DataSource> dataSources = new ArrayList<>();
    addFromDataSource(mappingProfileRule, transformationParts, dataSources);
    addEmptyIndicators(dataSources);
    rule.setDataSources(dataSources);
  }

  private void addFromDataSource(MappingProfileRule mappingProfileRule, List<String> transformationParts, List<DataSource> dataSources) {
    DataSource fromDataSource = new DataSource();
    fromDataSource.setFrom(mappingProfileRule.getPath());
    fromDataSource = addTranslationToDataSource(fromDataSource, mappingProfileRule.getId());
    fromDataSource.setSubfield(transformationParts.get(1).replace("$", EMPTY));
    dataSources.add(fromDataSource);
  }

  private void addEmptyIndicators(List<DataSource> dataSources) {
    DataSource indicator1 = createEmptyIndicatorDataSource();
    indicator1.setIndicator("1");
    dataSources.add(indicator1);
    DataSource indicator2 = createEmptyIndicatorDataSource();
    indicator2.setIndicator("2");
    dataSources.add(indicator2);
  }

  private DataSource addTranslationToDataSource(DataSource dataSource, String fieldName) {
    if (TRANSLATIONS_MAP.containsKey(fieldName)) {
      Translation translation = new Translation();
      translation.setFunction(TRANSLATIONS_MAP.get(fieldName));
      dataSource.setTranslation(translation);
    }
    return dataSource;
  }


  private DataSource createEmptyIndicatorDataSource() {
    DataSource indicatorDataSource = new DataSource();
    Translation indicatorTranslation = new Translation();
    indicatorTranslation.setFunction("set_value");
    Map<String, String> parameters = new HashMap<>();
    parameters.put("value", " ");
    indicatorTranslation.setParameters(parameters);
    indicatorDataSource.setTranslation(indicatorTranslation);
    return indicatorDataSource;
  }
}
