package org.folio.service.mapping.processor;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.vertx.core.json.Json;
import org.apache.commons.lang3.StringUtils;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.folio.service.mapping.profiles.RecordType.HOLDINGS;
import static org.folio.service.mapping.profiles.RecordType.INSTANCE;

public class RuleFactory {

  public static final int TAG_INDEX = 0;

  static final Map<String, String> TRANSLATIONS_MAP = ImmutableMap.of(
    "materialTypeId", "set_material_type",
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
      if (mappingProfileRule.isEnabled() && isNoneBlank(mappingProfileRule.getPath())) {
        if (INSTANCE.equals(mappingProfileRule.getRecordType()) && isBlank(mappingProfileRule.getTransformation())) {
          for (Rule defaultRule : getDefaultRules()) {
            for (DataSource dataSource : defaultRule.getDataSources()) {
              if (isNoneBlank(dataSource.getFrom()) && mappingProfileRule.getPath().equals(dataSource.getFrom())) {
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
    List<String> transformationParts = Splitter.on(StringUtils.SPACE).trimResults().splitToList(mappingProfileRule.getTransformation());
    Rule rule = new Rule();
    rule.setTag(transformationParts.get(TAG_INDEX));
    List<DataSource> dataSources = new ArrayList<>();
    DataSource fromDataSource = new DataSource();
    fromDataSource.setFrom(mappingProfileRule.getPath());
    fromDataSource = addTranslationToDataSource(fromDataSource, mappingProfileRule.getId());
    if (transformationParts.size() > 1) {
      if (transformationParts.size() == 2) {
        fromDataSource.setSubfield(transformationParts.get(2).replace("$", EMPTY));
      } else if (transformationParts.size() == 3) {
        fromDataSource.setSubfield(transformationParts.get(3).replace("$", EMPTY));
        List<String> indicators = Splitter.fixedLength(2).trimResults().splitToList(transformationParts.get(2));
        dataSources.add(createIndicatorDataSource(indicators.get(0)));
        dataSources.add(createIndicatorDataSource(indicators.get(1)));
      }
    }
    dataSources.add(fromDataSource);
    rule.setDataSources(dataSources);
    return rule;
  }

  private DataSource addTranslationToDataSource(DataSource dataSource, String fieldName) {
    if (TRANSLATIONS_MAP.containsKey(fieldName)) {
      Translation translation = new Translation();
      translation.setFunction(TRANSLATIONS_MAP.get(fieldName));
      dataSource.setTranslation(translation);
    }
    return dataSource;
  }


  private DataSource createIndicatorDataSource(String indicatorValue) {
    DataSource indicatorDataSource = new DataSource();
    indicatorDataSource.setIndicator(indicatorValue);
    Translation firstIndicatorTranslation = new Translation();
    firstIndicatorTranslation.setFunction("set_value");
    indicatorDataSource.setTranslation(firstIndicatorTranslation);
    return indicatorDataSource;
  }
}
