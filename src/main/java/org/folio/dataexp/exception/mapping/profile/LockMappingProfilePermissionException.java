package org.folio.dataexp.exception.mapping.profile;

/** Exception thrown when a user lacks permission to lock a job profile. */
public class LockMappingProfilePermissionException extends RuntimeException {
  /**
   * Constructs a new LockJobProfilePermissionException with the specified detail message.
   *
   * @param message the detail message
   */
  public LockMappingProfilePermissionException(String message) {
    super(message);
  }
}
