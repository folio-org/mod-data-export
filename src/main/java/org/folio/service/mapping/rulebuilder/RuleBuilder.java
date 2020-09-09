package org.folio.service.mapping.rulebuilder;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.processor.rule.Rule;
import org.folio.rest.jaxrs.model.Transformations;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.nonNull;

public interface RuleBuilder {
  Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  Optional<Rule> build(Collection<Rule> rules, Transformations mappingTransformation);

}
