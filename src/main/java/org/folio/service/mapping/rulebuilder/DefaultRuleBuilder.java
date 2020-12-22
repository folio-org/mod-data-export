package org.folio.service.mapping.rulebuilder;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.folio.processor.rule.Rule;
import org.folio.rest.jaxrs.model.Transformations;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.nonNull;

public class DefaultRuleBuilder implements RuleBuilder {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public Optional<Rule> build(Collection<Rule> rules, Transformations mappingTransformation) {
    return build(rules, mappingTransformation.getFieldId());
  }

  protected Optional<Rule> build(Collection<Rule> rules, String fieldId) {
    Optional<Rule> rule = rules.stream()
      .filter(defaultRule -> nonNull(defaultRule.getId()) && defaultRule.getId().equals(fieldId))
      .findFirst();
    if (rule.isPresent()) {
      return rule;
    } else {
      LOGGER.error("Cannot find default rule with field id {}", fieldId);
      return Optional.empty();
    }
  }
}
