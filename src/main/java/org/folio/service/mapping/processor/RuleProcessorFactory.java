package org.folio.service.mapping.processor;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.vertx.core.json.Json;
import org.apache.commons.lang3.StringUtils;
import org.folio.service.mapping.processor.rule.DataSource;
import org.folio.service.mapping.processor.rule.Rule;
import org.folio.service.mapping.processor.translations.Translation;
import org.folio.service.mapping.profiles.MappingProfileField;
import org.folio.service.mapping.profiles.RecordType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

@Service
public class RuleProcessorFactory {

  public static final int TAG_INDEX = 0;

  static final Map<String, String> TRANSLATIONS_MAP = ImmutableMap.of(
    "materialTypeId", "set_material_type",
    "electronicAccess.relationshipId", "set_electronic_access_relationship"
  );

  private List<Rule> defaultRules;

  public RuleProcessor createDefault() throws IOException {
    return new RuleProcessor(getDefaultRules());
  }

  public RuleProcessor createByMappingFields(List<MappingProfileField> mappingProfileFields) throws IOException {
    List<Rule> rules = new ArrayList<>();
    for (MappingProfileField mappingProfileField : mappingProfileFields) {
      if (isNoneBlank(mappingProfileField.getPath())) {
        if (RecordType.INSTANCE.equals(mappingProfileField.getRecordType()) && isBlank(mappingProfileField.getTransformation())) {
          Rule defaultRule = getDefaultRuleByPath(mappingProfileField.getPath());
          if(Objects.nonNull(defaultRule)) {
            rules.add(defaultRule);
          }
        } else {
          rules.add(buildRuleByMappingProfileField(mappingProfileField));
        }
      }
    }
    return new RuleProcessor(rules);
  }

  private Rule getDefaultRuleByPath(String fieldPath) throws IOException {
    for (Rule defaultRule : getDefaultRules()) {
      for (DataSource dataSource : defaultRule.getDataSources()) {
        if (isNoneBlank(dataSource.getFrom()) && fieldPath.equals(dataSource.getFrom())) {
          return defaultRule;
        }
      }
    }
    return null;
  }

  private List<Rule> getDefaultRules() throws IOException {
    if (Objects.nonNull(this.defaultRules)) {
      return this.defaultRules;
    }
    URL url = Resources.getResource("rules/rulesDefault.json");
    String stringRules = Resources.toString(url, StandardCharsets.UTF_8);
    this.defaultRules = Arrays.asList(Json.decodeValue(stringRules, Rule[].class));
    return this.defaultRules;
  }

  private Rule buildRuleByMappingProfileField(MappingProfileField mappingProfileField) {
    List<String> transformationParts = Splitter.on(StringUtils.SPACE).trimResults().splitToList(mappingProfileField.getTransformation());
    Rule rule = new Rule();
    rule.setTag(transformationParts.get(TAG_INDEX));
    List<DataSource> dataSources = new ArrayList<>();
    DataSource fromDataSource = new DataSource();
    fromDataSource.setFrom(mappingProfileField.getPath());
    fromDataSource = addTranslationToDataSource(fromDataSource, mappingProfileField.getName());
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
