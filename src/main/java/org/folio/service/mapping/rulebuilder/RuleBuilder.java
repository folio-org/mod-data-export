package org.folio.service.mapping.rulebuilder;

import org.folio.processor.rule.Rule;
import org.folio.rest.jaxrs.model.Transformations;

import java.util.Collection;
import java.util.Optional;

public interface RuleBuilder {

  Optional<Rule> build(Collection<Rule> rules, Transformations mappingTransformation);

}
