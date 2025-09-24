package org.folio.dataexp.exception.job.profile;

/**
 * Exception thrown when a default job profile operation fails.
 */
public class DefaultJobProfileException extends RuntimeException {
  /**
   * Constructs a new DefaultJobProfileException with the specified detail message.
   *
   * @param message the detail message
   */
  public DefaultJobProfileException(String message) {
    super(message);
  }
}
