package org.folio.dataexp.exception;

/**
 * Exception thrown when a transformation rule operation fails.
 */
public class TransformationRuleException extends Exception {
  /**
   * Constructs a new TransformationRuleException with the specified detail message.
   *
   * @param msg the detail message
   */
  public TransformationRuleException(String msg) {
    super(msg);
  }
}
