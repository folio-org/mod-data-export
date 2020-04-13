package org.folio.service.mapping.reader.values;

/**
 * Generic interface to wrap data, that can be read and returned from entity
 *
 * @param <T> data type
 */
public interface RuleValue<T> {
  T getValue();

  Type getType();

  enum Type {
    MISSING,
    SIMPLE,
    COMPOSITE
  }
}
