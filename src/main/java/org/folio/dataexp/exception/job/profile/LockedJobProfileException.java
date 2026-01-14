package org.folio.dataexp.exception.job.profile;

/** Exception thrown when attempting to modify a locked job profile. */
public class LockedJobProfileException extends RuntimeException {
  /**
   * Constructs a new LockedJobProfileException with the specified detail message.
   *
   * @param message the detail message
   */
  public LockedJobProfileException(String message) {
    super(message);
  }
}
