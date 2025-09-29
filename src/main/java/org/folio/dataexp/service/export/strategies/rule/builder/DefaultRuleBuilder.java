package org.folio.dataexp.service.export.strategies.rule.builder;

import static java.util.Objects.nonNull;

import java.util.Collection;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.processor.rule.Rule;

/**
 * Default implementation of RuleBuilder for building rules based on field ID.
 */
@Log4j2
public class DefaultRuleBuilder implements RuleBuilder {

  /**
   * Builds a rule based on the provided rules and mapping transformation.
   *
   * @param rules collection of rules
   * @param mappingTransformation transformation mapping
   * @return an Optional containing the Rule if present
   */
  @Override
  public Optional<Rule> build(Collection<Rule> rules, Transformations mappingTransformation) {
    return build(rules, mappingTransformation.getFieldId());
  }

  /**
   * Builds a rule based on the provided rules and field ID.
   *
   * @param rules collection of rules
   * @param fieldId field ID
   * @return an Optional containing the Rule if present
   */
  protected Optional<Rule> build(Collection<Rule> rules, String fieldId) {
    Optional<Rule> rule = rules.stream()
        .filter(defaultRule -> nonNull(defaultRule.getId()) && defaultRule.getId().equals(fieldId))
        .findFirst();
    if (rule.isPresent()) {
      return rule;
    } else {
      log.error("Cannot find default rule with field id {}", fieldId);
      return Optional.empty();
    }
  }
}
