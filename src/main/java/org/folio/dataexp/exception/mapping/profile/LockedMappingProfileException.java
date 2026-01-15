package org.folio.dataexp.exception.mapping.profile;

/** Exception thrown when attempting to modify a locked mapping profile. */
public class LockedMappingProfileException extends RuntimeException {
  /**
   * Constructs a new LockedMappingProfileException with the specified detail message.
   *
   * @param message the detail message
   */
  public LockedMappingProfileException(String message) {
    super(message);
  }
}
