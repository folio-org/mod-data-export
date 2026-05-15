package org.folio.dataexp.exception.job.profile;

/** Exception thrown when a user lacks permission to lock a job profile. */
public class LockJobProfilePermissionException extends RuntimeException {
  /**
   * Constructs a new LockJobProfilePermissionException with the specified detail message.
   *
   * @param message the detail message
   */
  public LockJobProfilePermissionException(String message) {
    super(message);
  }
}
