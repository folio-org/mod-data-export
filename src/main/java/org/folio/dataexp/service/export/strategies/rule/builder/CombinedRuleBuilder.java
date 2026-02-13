package org.folio.dataexp.service.export.strategies.rule.builder;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.processor.rule.DataSource;
import org.folio.processor.rule.Metadata;
import org.folio.processor.rule.Rule;

/** Builder for combining transformation rules with default rules. */
@Log4j2
public class CombinedRuleBuilder extends DefaultRuleBuilder {

  private int transformationFieldKeyIndex;
  private String defaultFieldId;

  /**
   * Constructs a CombinedRuleBuilder.
   *
   * @param transformationFieldKeyIndex index of the transformation field key
   * @param defaultFieldId default field ID
   */
  public CombinedRuleBuilder(int transformationFieldKeyIndex, String defaultFieldId) {
    this.transformationFieldKeyIndex = transformationFieldKeyIndex;
    this.defaultFieldId = defaultFieldId;
  }

  /**
   * Builds a combined rule based on the provided rules and mapping transformation.
   *
   * @param rules collection of rules
   * @param mappingTransformation transformation mapping
   * @return an Optional containing the combined Rule if present
   */
  @Override
  public Optional<Rule> build(Collection<Rule> rules, Transformations mappingTransformation) {
    Optional<Rule> defaultRuleOptional = super.build(rules, defaultFieldId);
    if (defaultRuleOptional.isPresent()) {
      Rule defaultRule = defaultRuleOptional.get();
      List<DataSource> defaultDataSources = defaultRule.getDataSources();
      Optional<DataSource> subfieldDataSource =
          getDataSourceByDefaultSubfield(defaultDataSources, mappingTransformation.getFieldId());
      if (subfieldDataSource.isPresent()) {
        Rule rule = new Rule();
        rule.setField(defaultRule.getField());
        if (defaultRule.getMetadata() != null) {
          setMetadata(defaultRule, rule);
        }
        DataSource transformationDataSource = new DataSource();
        transformationDataSource.setSubfield(subfieldDataSource.get().getSubfield());
        transformationDataSource.setFrom(mappingTransformation.getPath());
        List<DataSource> transformationDataSources = new ArrayList<>();
        transformationDataSources.add(transformationDataSource);
        setIndicatorDataSources(defaultDataSources, transformationDataSources);
        rule.setDataSources(transformationDataSources);
        return Optional.of(rule);
      } else {
        log.error(
            "Default rule for the field with id {} doesn't contains necessary"
                + "subfield data source",
            mappingTransformation.getFieldId());
      }
    } else {
      log.error(
          "Field with id {} doesn't have mapping in default rules",
          mappingTransformation.getFieldId());
    }
    return Optional.empty();
  }

  /**
   * Sets metadata from the default rule to the new rule.
   *
   * @param defaultRule the default rule
   * @param rule the rule to set metadata on
   */
  private void setMetadata(Rule defaultRule, Rule rule) {
    Map<String, String> metadata = new HashMap<>();
    for (Map.Entry<String, Metadata.Entry> entry : defaultRule.getMetadata().getData().entrySet()) {
      metadata.put(entry.getKey(), entry.getValue().getFrom());
    }
    rule.setMetadata(metadata);
  }

  /**
   * Gets the DataSource by default subfield.
   *
   * @param defaultDataSources list of default data sources
   * @param transformationFieldId transformation field ID
   * @return an Optional containing the DataSource if found
   */
  private Optional<DataSource> getDataSourceByDefaultSubfield(
      List<DataSource> defaultDataSources, String transformationFieldId) {
    String transformationFieldKey =
        Splitter.on(".").splitToList(transformationFieldId).get(transformationFieldKeyIndex);
    return defaultDataSources.stream()
        .filter(dataSource -> dataSource.getFrom().toLowerCase().contains(transformationFieldKey))
        .findFirst();
  }

  /**
   * Sets indicator data sources from default data sources to transformation data sources.
   *
   * @param defaultDataSources list of default data sources
   * @param transformationDataSources list of transformation data sources
   */
  private void setIndicatorDataSources(
      List<DataSource> defaultDataSources, List<DataSource> transformationDataSources) {
    for (DataSource defaultDataSource : defaultDataSources) {
      if (StringUtils.isNotEmpty(defaultDataSource.getIndicator())) {
        transformationDataSources.add(defaultDataSource);
      }
    }
  }
}
