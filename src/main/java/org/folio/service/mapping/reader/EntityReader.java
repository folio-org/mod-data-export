package org.folio.service.mapping.reader;


import org.folio.service.mapping.processor.rule.Rule;
import org.folio.service.mapping.reader.values.RuleValue;

/**
 * The root interface for readers.
 * EntityReader is intended to define behaviour for reading data by the given rule from an underling entity.
 *
 * @see Rule
 * @see RuleValue
 */
public interface EntityReader {

  /**
   * Reads data by the given rule from an underling entity
   *
   * @param rule mapping rule
   * @return rule value
   */
  RuleValue read(Rule rule);
}
