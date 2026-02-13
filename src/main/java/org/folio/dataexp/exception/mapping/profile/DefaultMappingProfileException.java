package org.folio.dataexp.exception.mapping.profile;

/** Exception thrown when a default mapping profile operation fails. */
public class DefaultMappingProfileException extends RuntimeException {
  /**
   * Constructs a new DefaultMappingProfileException with the specified detail message.
   *
   * @param message the detail message
   */
  public DefaultMappingProfileException(String message) {
    super(message);
  }
}
