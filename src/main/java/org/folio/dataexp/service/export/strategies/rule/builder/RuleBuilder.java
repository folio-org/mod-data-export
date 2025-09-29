package org.folio.dataexp.service.export.strategies.rule.builder;

import java.util.Collection;
import java.util.Optional;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.exception.TransformationRuleException;
import org.folio.processor.rule.Rule;

/**
 * Interface for building transformation rules.
 */
public interface RuleBuilder {

  /**
   * Builds a rule based on the provided rules and mapping transformation.
   *
   * @param rules collection of rules
   * @param mappingTransformation transformation mapping
   * @return an Optional containing the Rule if present
   * @throws TransformationRuleException if rule building fails
   */
  Optional<Rule> build(Collection<Rule> rules, Transformations mappingTransformation)
      throws TransformationRuleException;

}
