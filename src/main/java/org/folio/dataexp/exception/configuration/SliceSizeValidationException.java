package org.folio.dataexp.exception.configuration;

/**
 * Exception thrown when slice size validation fails.
 */
public class SliceSizeValidationException extends RuntimeException {
  /**
   * Constructs a new SliceSizeValidationException with the specified detail message.
   *
   * @param message the detail message
   */
  public SliceSizeValidationException(String message) {
    super(message);
  }
}
