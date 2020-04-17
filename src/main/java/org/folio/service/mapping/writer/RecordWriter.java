package org.folio.service.mapping.writer;

import org.folio.service.mapping.reader.values.CompositeValue;
import org.folio.service.mapping.reader.values.RuleValue;
import org.folio.service.mapping.reader.values.SimpleValue;

/**
 * The root interface for writers.
 * Writer is responsible to write given value to underlying marc record
 *
 * @see RuleValue
 * @see SimpleValue
 * @see CompositeValue
 */
public interface RecordWriter {

  /**
   * Writes simple value to record whether control field or data field.
   *
   * @param tag   tag name of the marc record
   * @param value simple value
   */
  void write(String tag, SimpleValue value);

  /**
   * Writes composite value to record. This can be only data field.
   *
   * @param tag   tag name of the marc record
   * @param value composite value
   */
  void write(String tag, CompositeValue value);

  /**
   * Returns final result of writing
   *
   * @return underlying record in string representation
   */
  String getResult();
}
