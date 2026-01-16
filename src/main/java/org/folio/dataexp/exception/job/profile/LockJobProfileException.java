package org.folio.dataexp.exception.job.profile;

/** Exception thrown when attempting to modify a locked job profile. */
public class LockJobProfileException extends RuntimeException {
  /**
   * Constructs a new LockJobProfileException with the specified detail message.
   *
   * @param message the detail message
   */
  public LockJobProfileException(String message) {
    super(message);
  }
}
