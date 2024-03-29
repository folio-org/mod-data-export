package org.folio.dataexp.service.export.strategies.rule.builder;

import lombok.extern.log4j.Log4j2;

import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.processor.rule.Rule;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.nonNull;

@Log4j2
public class DefaultRuleBuilder implements RuleBuilder {

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
      log.error("Cannot find default rule with field id {}", fieldId);
      return Optional.empty();
    }
  }
}
