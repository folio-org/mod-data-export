package org.folio.dataexp.exception;

/** Exception thrown when transformation validation fails. */
public class TransformationValidationException extends RuntimeException {
  /**
   * Constructs a new TransformationValidationException with the specified detail message.
   *
   * @param message the detail message
   */
  public TransformationValidationException(String message) {
    super(message);
  }
}
