package org.folio.dataexp.service.export.strategies.rule.builder;

import org.folio.dataexp.domain.dto.Transformations;
import org.folio.processor.rule.Rule;

import java.util.Collection;
import java.util.Optional;

public interface RuleBuilder {

  Optional<Rule> build(Collection<Rule> rules, Transformations mappingTransformation);

}
